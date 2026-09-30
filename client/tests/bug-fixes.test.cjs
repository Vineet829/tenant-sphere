const assert = require('node:assert/strict');
const test = require('node:test');
const React = require('react');
const { renderToStaticMarkup } = require('react-dom/server');
const { sourceLoader, elements } = require('./source-loader.cjs');

test('errors tolerate null, primitives and malformed fields', () => {
  const extract = sourceLoader()('src/utils/extractErrorMessage.ts').default;
  for (const data of [null, undefined, 0, false, 'Gateway error', []]) assert.equal(extract({ data }), null);
  assert.equal(extract({ data: { detail: 'Expired session' } }), 'Expired session');
  assert.equal(extract({ data: { email: ['Already registered', null, 42], other: { title: ['Required'] } } }), 'Already registered, Required');
});

test('return destinations stay local', () => {
  const { safeLoginDestination } = sourceLoader()('src/utils/loginDestination.ts');
  for (const url of [null, 'https://evil.example', '//evil.example', '/\\evil.example', '/login', '/register', '/ x']) assert.equal(safeLoginDestination(url), '/welcome');
  assert.equal(safeLoginDestination('/post/lift-service?from=home'), '/post/lift-service?from=home');
});

test('private route never renders its children before authentication or while redirecting', () => {
  let state = { auth: { isInitialized: false, isAuthenticated: false } };
  const load = sourceLoader({
    '@/lib/redux/hooks/typedHooks': { useAppSelector: select => select(state) },
    'next/navigation': { useRouter: () => ({ replace() {} }), usePathname: () => '/issue/example' },
    '@/components/shared/Spinner': () => React.createElement('span', null, 'Loading'),
  });
  const Guard = load('src/components/shared/ProtectedRoutes.tsx').default;
  const render = () => renderToStaticMarkup(React.createElement(Guard, null, 'PRIVATE'));
  assert.ok(!render().includes('PRIVATE'));
  state.auth.isInitialized = true; assert.ok(!render().includes('PRIVATE'));
  state.auth.isAuthenticated = true; assert.ok(render().includes('PRIVATE'));
});

test('same display name does not expose another resident’s issue actions', () => {
  let currentUser = { id: 'different-person', full_name: 'Alex Lee', is_staff: false, is_superuser: false };
  const issue = { id: 'issue', reported_by_id: 'reporter', assigned_to_id: 'technician', reported_by: 'Alex Lee', assigned_to: 'Alex Lee', title: 'Tap', description: 'Leak', status: 'reported', priority: 'low' };
  const load = sourceLoader({
    '@/lib/redux/features/auth/authApiSlice': { useGetUserQuery: () => ({ data: currentUser }) },
    '@/lib/redux/features/issues/issueApiSlice': { useGetSingleIssueQuery: () => ({ data: { issue } }), useDeleteIssueMutation: () => [() => {}, {}] },
    'next/navigation': { useRouter: () => ({ push() {} }) },
    '@/components/estate/ConfirmButton': () => React.createElement('span', null, 'DELETE_ACTION'),
  });
  const Detail = load('src/components/issue/IssueDetails.tsx').default;
  const render = () => renderToStaticMarkup(React.createElement(Detail, { params: { id: 'issue' } }));
  assert.ok(!render().includes('DELETE_ACTION')); assert.ok(!render().includes('/issue/update-issue/'));
  currentUser.id = 'reporter'; assert.ok(render().includes('DELETE_ACTION'));
  currentUser.id = 'technician'; assert.ok(render().includes('/issue/update-issue/'));
  currentUser.id = 'staff'; currentUser.is_staff = true;
  assert.ok(render().includes('DELETE_ACTION')); assert.ok(render().includes('/issue/update-issue/'));
});

test('bookmarks use a separate page number and show the total across pages', () => {
  const load = sourceLoader({
    '@/lib/redux/features/posts/postApiSlice': { useGetAllMyBookmarksQuery: page => {
      assert.equal(page, 1);
      return { currentData: { bookmarked_posts: { count: 23, total_pages: 3, page_size: 9, results: [] } } };
    } },
    '@/components/estate/PageHeader': props => React.createElement('span', null, `TOTAL:${props.count}`),
  });
  const Card = load('src/components/cards/BookmarkedPostCard.tsx').default;
  const html = renderToStaticMarkup(React.createElement(Card));
  assert.ok(html.includes('TOTAL:23')); assert.ok(html.includes('of 3')); assert.ok(html.includes('Next'));
  const Controls = load('src/components/shared/PageControls.tsx').default;
  let page;
  const next = elements(Controls({ page: 1, totalPages: 3, onChange: value => page = value }), e => e.type === 'button')[1];
  next.props.onClick(); assert.equal(page, 2);
});

test('HTTPS avatar upload uses the host-only CSRF cookie and retries after refreshing an expired access token', { timeout: 10000 }, async () => {
  const oldFetch = global.fetch, OldRequest = global.Request;
  let csrf, uploads = 0, refreshes = 0, bootstraps = 0;
  const oldWindow = global.window;
  global.window = { location: { protocol: 'https:' } };
  const load = sourceLoader({ 'cookies-next': { getCookie: name => name === '__Host-XSRF-TOKEN' ? csrf : 'untrusted-legacy-cookie' } });
  const api = load('src/lib/redux/features/users/usersApiSlice.ts').usersApiSlice;
  const store = load('src/lib/redux/store.ts').makeStore();
  global.Request = class extends OldRequest { constructor(input, init) { super(typeof input === 'string' ? new URL(input, 'http://localhost') : input, init); } };
  global.fetch = async request => {
    if (request.url.endsWith('/csrf/')) { bootstraps++; csrf = 'browser-csrf'; return new Response(null, { status: 204 }); }
    assert.equal(request.headers.get('X-XSRF-TOKEN'), 'browser-csrf');
    if (request.url.endsWith('/refresh/')) { refreshes++; return Response.json({ message: 'Refreshed' }); }
    assert.ok(request.url.endsWith('/avatar/')); assert.match(request.headers.get('content-type'), /^multipart\/form-data; boundary=/);
    const body = await request.formData(); assert.equal(body.get('avatar').name, 'photo.png');
    uploads++; return uploads === 1 ? Response.json({ detail: 'Expired' }, { status: 401 }) : Response.json({ avatar: '/media/avatars/photo.png' });
  };
  try {
    const form = new FormData(); form.append('avatar', new Blob(['png'], { type: 'image/png' }), 'photo.png');
    await store.dispatch(api.endpoints.uploadAvatar.initiate(form)).unwrap();
    assert.equal(uploads, 2); assert.equal(refreshes, 1); assert.equal(bootstraps, 1);
    assert.equal(store.getState().auth.isAuthenticated, true);
  } finally { store.dispatch(api.util.resetApiState()); global.fetch = oldFetch; global.Request = OldRequest; if (oldWindow === undefined) delete global.window; else global.window = oldWindow; }
});

test('bad login credentials never refresh into the previous account', { timeout: 10000 }, async () => {
  const oldFetch = global.fetch, OldRequest = global.Request;
  const load = sourceLoader({ 'cookies-next': { getCookie: () => 'csrf-fixture' } });
  const api = load('src/lib/redux/features/auth/authApiSlice.ts').authApiSlice;
  const store = load('src/lib/redux/store.ts').makeStore(); let requests = [];
  global.Request = class extends OldRequest { constructor(input, init) { super(typeof input === 'string' ? new URL(input, 'http://localhost') : input, init); } };
  global.fetch = async request => { requests.push(new URL(request.url).pathname); return Response.json({ detail: 'Incorrect' }, { status: 401 }); };
  try {
    await assert.rejects(store.dispatch(api.endpoints.loginUser.initiate({ email: 'person@example.com', password: 'wrong' })).unwrap());
    assert.deepEqual(requests, ['/api/v1/auth/login/']); assert.equal(store.getState().auth.isAuthenticated, false);
  } finally { store.dispatch(api.util.resetApiState()); global.fetch = oldFetch; global.Request = OldRequest; }
});
