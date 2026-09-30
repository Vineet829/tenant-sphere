const assert = require('node:assert/strict');
const test = require('node:test');
const fs = require('node:fs');
const path = require('node:path');
const { createRequire } = require('node:module');
const ts = require('typescript');
const React = require('react');
const { renderToStaticMarkup } = require('react-dom/server');

const root = path.resolve(__dirname, '..');
function sourceLoader(mocks = {}) {
  const cache = new Map();
  function load(filename) {
    filename = path.resolve(root, filename);
    if (!fs.existsSync(filename)) filename = [filename + '.tsx', filename + '.ts', path.join(filename, 'index.ts')].find(fs.existsSync);
    assert.ok(filename, 'Source module exists');
    if (cache.has(filename)) return cache.get(filename).exports;
    const module = { exports: {} };
    cache.set(filename, module);
    const compiled = ts.transpileModule(fs.readFileSync(filename, 'utf8'), {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true },
    }).outputText;
    function localRequire(name) {
      if (Object.hasOwn(mocks, name)) return mocks[name];
      if (name.startsWith('@/')) return load(path.join(root, 'src', name.slice(2)));
      if (name.startsWith('.')) return load(path.resolve(path.dirname(filename), name));
      return createRequire(filename)(name);
    }
    new Function('require', 'module', 'exports', compiled)(localRequire, module, module.exports);
    return module.exports;
  }
  return load;
}
function elements(node, predicate) {
  if (Array.isArray(node)) return node.flatMap(child => elements(child, predicate));
  if (!node?.props) return [];
  return [...(predicate(node) ? [node] : []), ...elements(node.props.children, predicate)];
}
function deferred() {
  let resolve, reject;
  const promise = new Promise((res, rej) => { resolve = res; reject = rej; });
  return { promise, resolve, reject };
}
const sample = { id: 'post-id', slug: 'example', title: 'Community post', body: 'Body', author_username: 'neighbour', created_at: '2026-09-01', replies: [], tags: [], upvotes: 0, downvotes: 0, is_upvoted: false, is_downvoted: false };
const noop = () => null;

test('one signed score and the selected direction are displayed even at zero', () => {
  const Actions = sourceLoader({ '@/components/estate/ConfirmButton': noop })('src/components/post/PostActions.tsx').default;
  for (const [upvotes, downvotes, isUpvoted, isDownvoted] of [[0, 0, false, false], [0, 1, false, true], [1, 0, true, false], [4, 4, false, true]]) {
    const tree = Actions({ slug: 'example', upvotes, downvotes, isUpvoted, isDownvoted, isUpvoteLoading: false, isDownvoteLoading: false });
    const score = elements(tree, e => e.props.className === 'es-vote-score');
    assert.equal(score.length, 1);
    assert.equal(score[0].props.children.find(value => typeof value === 'number'), upvotes - downvotes);
    const buttons = elements(tree, e => e.type === 'button');
    assert.equal(buttons[0].props['aria-pressed'], isUpvoted);
    assert.equal(buttons[1].props['aria-pressed'], isDownvoted);
    assert.equal(buttons[0].props['aria-label'], isUpvoted ? 'Remove upvote' : 'Upvote');
    assert.equal(buttons[1].props['aria-label'], isDownvoted ? 'Remove downvote' : 'Downvote');
  }
});

function detailHarness(post) {
  let actions, response = deferred();
  const calls = [], notifications = [];
  const load = sourceLoader({
    '@/lib/redux/features/posts/postApiSlice': {
      useGetSinglePostQuery: () => ({ currentData: { post } }),
      useSetPostVoteMutation: () => [args => { calls.push(args); return { unwrap: () => response.promise }; }, { isLoading: false }],
      useBookmarkPostMutation: () => [noop, {}], useDeletePostMutation: () => [noop, {}],
    },
    '@/lib/redux/features/users/usersApiSlice': { useGetUserProfileQuery: () => ({}) },
    '@/utils': { sortByDateDescending: items => items },
    'next/navigation': { useRouter: () => ({ back() {} }) },
    'react-toastify': { toast: { success: message => notifications.push(['success', message]), error: message => notifications.push(['error', message]) } },
    '../shared/ProtectedRoutes': ({ children }) => children,
    '../forms/add-reply/CreateReplyForm': noop,
    './PostActions': props => { actions = props; return null; }, './PostBody': noop, './PostHeader': noop, './RepliesList': noop,
  });
  renderToStaticMarkup(React.createElement(load('src/components/post/PostDetails.tsx').default, { params: { slug: post.slug } }));
  return { actions, calls, notifications, get response() { return response; }, retry() { response = deferred(); } };
}

test('rapid repeated or opposing clicks submit only one request before React rerenders', async () => {
  const harness = detailHarness(sample);
  const first = harness.actions.handleDownVote();
  await harness.actions.handleDownVote();
  await harness.actions.handleUpvote();
  assert.deepEqual(harness.calls, [{ postId: sample.id, vote: -1 }]);
  assert.equal(harness.notifications.length, 0);
  harness.response.resolve({ message: 'Post downvoted successfully!' });
  await first;
  assert.deepEqual(harness.notifications, [['success', 'Post downvoted successfully!']]);
});

test('active votes request removal and failed requests can be retried safely', async () => {
  for (const direction of ['up', 'down']) {
    const harness = detailHarness({ ...sample, is_upvoted: direction === 'up', is_downvoted: direction === 'down' });
    const action = harness.actions[direction === 'up' ? 'handleUpvote' : 'handleDownVote'];
    const first = action();
    harness.response.reject(new Error('Offline')); await first;
    assert.equal(harness.notifications[0][0], 'error');
    harness.retry();
    const retry = action();
    harness.response.resolve({ message: 'Vote removed' }); await retry;
    assert.deepEqual(harness.calls, [{ postId: sample.id, vote: 0 }, { postId: sample.id, vote: 0 }]);
  }
});

test('confirmed votes update every cached view immediately; failures preserve state; logout clears it', { timeout: 10000 }, async () => {
  const originalFetch = global.fetch, OriginalRequest = global.Request;
  const gate = deferred();
  let readsBlocked = false, failure = false;
  let confirmed = { ...sample };
  const other = { ...sample, id: 'other-id', slug: 'other', upvotes: 9 };
  const load = sourceLoader({ 'cookies-next': { getCookie: () => 'csrf-fixture' } });
  const api = load('src/lib/redux/features/posts/postApiSlice.ts').postApiSlice;
  const users = load('src/lib/redux/features/users/usersApiSlice.ts').usersApiSlice;
  const store = load('src/lib/redux/store.ts').makeStore();
  const subscriptions = [];
  const page = results => ({ count: results.length, results, next: null, previous: null });
  global.Request = class extends OriginalRequest {
    constructor(input, init) { super(typeof input === 'string' ? new URL(input, 'http://localhost') : input, init); }
  };
  global.fetch = async request => {
    const url = new URL(request.url);
    if (request.method === 'PUT') {
      if (failure) return new Response(JSON.stringify({ message: 'Unavailable' }), { status: 503 });
      const { vote } = await request.json();
      confirmed = { ...confirmed, upvotes: vote === 1 ? 1 : 0, downvotes: vote === -1 ? 1 : 0, is_upvoted: vote === 1, is_downvoted: vote === -1 };
      readsBlocked = true;
      return new Response(JSON.stringify({ ...confirmed, message: 'Vote updated' }));
    }
    if (readsBlocked) await gate.promise;
    const rows = [confirmed, other];
    let body;
    if (url.pathname.includes('/profiles/')) body = { profile_posts: page(rows) };
    else if (url.pathname.endsWith('/my-posts/')) body = { my_posts: page(rows) };
    else if (url.pathname.endsWith('/bookmarked/posts/')) body = { bookmarked_posts: page(rows) };
    else if (url.pathname.includes('/tags/')) body = { posts_by_tag: page(rows) };
    else if (url.pathname.endsWith('/top-posts/')) body = { top_posts: page(rows) };
    else if (url.pathname.endsWith('/example/')) body = { post: confirmed };
    else body = { posts: page(rows) };
    return new Response(JSON.stringify(body));
  };
  const views = [
    [api, 'getSinglePost', 'example', data => [data.post]],
    [api, 'getAllPosts', { page: 1 }, data => data.posts.results],
    [api, 'getMyPosts', undefined, data => data.my_posts.results],
    [api, 'getAllMyBookmarks', undefined, data => data.bookmarked_posts.results],
    [api, 'getPostsByTag', 'updates', data => data.posts_by_tag.results],
    [api, 'getTopPosts', undefined, data => data.top_posts.results],
    [users, 'getProfilePosts', { slug: 'neighbour', page: 1 }, data => data.profile_posts.results],
  ];
  try {
    for (const [slice, endpoint, args] of views) {
      const subscription = store.dispatch(slice.endpoints[endpoint].initiate(args));
      subscriptions.push(subscription); await subscription.unwrap();
    }
    await store.dispatch(api.endpoints.setPostVote.initiate({ postId: sample.id, vote: -1 })).unwrap();
    for (const [slice, endpoint, args, rows] of views) {
      const posts = rows(slice.endpoints[endpoint].select(args)(store.getState()).data);
      assert.equal(posts[0].upvotes - posts[0].downvotes, -1, endpoint);
      assert.equal(posts[0].is_downvoted, true, endpoint);
      if (posts[1]) assert.equal(posts[1].upvotes, 9, 'Unrelated posts are untouched');
    }
    failure = true;
    await assert.rejects(store.dispatch(api.endpoints.setPostVote.initiate({ postId: sample.id, vote: 1 })).unwrap());
    assert.equal(api.endpoints.getSinglePost.select('example')(store.getState()).data.post.is_downvoted, true);
    gate.resolve();
    await Promise.all(store.dispatch(api.util.getRunningQueriesThunk()));
    store.dispatch(load('src/lib/redux/features/auth/authSlice.ts').setLogout());
    assert.equal(api.endpoints.getSinglePost.select('example')(store.getState()).data, undefined);
  } finally {
    gate.resolve();
    for (const subscription of subscriptions) subscription.unsubscribe();
    store.dispatch(api.util.resetApiState());
    global.fetch = originalFetch; global.Request = OriginalRequest;
  }
});
