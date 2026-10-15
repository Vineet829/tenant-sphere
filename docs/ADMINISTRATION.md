# TenantSphere administration

The Spring backend replaces the registered Django admin models with a Vaadin administration application. The resident-facing Next.js application and its APIs remain separate.

## Running and opening the admin

- Development with `mvn spring-boot:run`: `http://localhost:8000/admin/login`.
- Development with Docker (`make build`, `local.yml`): `http://localhost:8080/admin/login` through the local Nginx container.
- Deployment (`production.yml`): `/admin/login` on the main site. Both Nginx configurations forward `/admin`, `/admin-assets/`, and `/VAADIN/` to the Spring container built from `docker/spring/Dockerfile`.
- Existing `/supersecret` bookmarks redirect to `/admin/`.
- Use an active staff or superuser account. Guest/resident accounts cannot enter administration.

The client's development server (`npm run dev`) also passes `/admin` to Spring. Any proxy placed in front of the admin must preserve the trailing slash on `/admin/`: Vaadin uses that exact URL for initialization and UI requests.

Copy `.envs/.env.example` to `.envs/.env.local` or `.envs/.env.production` for the Docker setups. `local.yml` already sets `COOKIE_SECURE=false` and points mail at its Mailpit container, because the local Nginx serves plain HTTP. With `mvn spring-boot:run` over `http://localhost`, set `COOKIE_SECURE=false` yourself: Safari drops `Secure` cookies even on localhost.

Java 21 and PostgreSQL are required. Build the deployable application with:

```sh
mvn -Pproduction package
java -jar target/tenant-sphere-0.1.0.jar
```

Use the application's existing database, signing, mail and cookie environment variables. Set `DOMAIN` to the resident-facing site URL. Keep secure cookies enabled on HTTPS deployments. Production builds include the admin frontend; Node/Vite is not needed by the running server.

SMTP connection/read/write timeouts are five seconds. `EMAIL_CLIENT_NAME` supplies the SMTP greeting hostname (defaults to `localhost` for local Mailpit); set it to the deployment's mail client hostname when using an external relay. This avoids a blocking lookup of the workstation's local hostname during email delivery and health checks.

The dependency baseline is Spring Boot 3.5.16 and Vaadin 24.10.7. The latter is the final freely available Vaadin 24 release, including the published upload, reserved-path and build-log fixes. Later Vaadin 24 patches require a commercial maintenance subscription. A future move to Vaadin 25 also requires the Spring Boot 4 migration; it is not silently bundled into this admin replacement. See [Vaadin's release policy](https://vaadin.com/roadmap) and [security advisories](https://vaadin.com/security).

## Creating an administrator

Apply the database migrations by starting the packaged application, then run this operator command with the same `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER` and `POSTGRES_PASSWORD` environment variables:

```sh
java -Dloader.main=com.tenantsphere.admin.CreateAdminCommand \
  -cp target/tenant-sphere-0.1.0.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  admin@example.com community_admin
```

The command prompts for a password and confirmation without echoing them. Passwords must be 12–128 characters with uppercase, lowercase and numeric characters. Automated setup can provide `TENANTSPHERE_ADMIN_PASSWORD` from a secret manager; do not pass passwords as command arguments or commit them to the repository. Optional third and fourth arguments set the administrator's first and last names.

This creates a separate active superuser, its profile, and audit history in one transaction. It refuses existing email addresses or usernames and never promotes or overwrites an existing resident account. It requires direct database credentials and does not expose a web setup endpoint.

Production builds regenerate the route-specific frontend bundle, so adding an administration page cannot silently reuse an older bundle missing that page's components.

## Django admin coverage

| Area | Management operations |
| --- | --- |
| Users | Create, edit and delete; hashed passwords with confirmation; active/staff/superuser flags; group membership and direct permissions; join/login dates. New users receive a profile automatically. |
| Profiles | Create missing profiles, edit personal/contact details, validated avatar upload/removal, delete. Report counts and reputation are derived from reports. Deleting a profile deactivates its account. |
| Apartments | Create, edit, assign/reassign a tenant, delete with issue cascade preview. |
| Issues | Create, edit all business fields, assign a person, set status/priority, delete; bulk status changes; related content views. Resolution/reopening updates dates and sends notifications only after commit. |
| Posts | Create, edit body/author/tags, delete; related content views. Vote totals remain controlled by actual votes. |
| Ratings | Create, edit and delete; a rating is from a resident to a technician; average rating. |
| Reports | Create, edit reporter/target/details, delete; distinct-reporter counts and reputation stay synchronized. |
| Content views | Create, edit and delete; real post/issue selection and IP validation. |
| Tags | Create, rename and delete tags; show usage; removal detaches tags without deleting posts. |
| Groups | Create, rename, delete; assign model permissions. Superusers only. |

Every model supports search, sorting, database paging, creation-date filters, record history and confirmed bulk deletion. Relevant choice filters include active/staff/superuser, gender/occupation, building/floor, issue status/priority, rating and content type. Post/issue editors show recent related content views. Use the Content views section to manage those records.

Generated IDs, timestamps, slugs, votes, report counts and reputation are not arbitrary form inputs. They remain consistent with the application rather than allowing an administrator to fabricate counters.

## Permissions and session safety

- Superusers have all model permissions and are the only people who can manage groups, grants, privileged accounts and permission flags.
- Staff need `resource.view`, `.add`, `.change`, or `.delete`. Change permission also permits viewing that model. Group and direct grants are combined.
- Migration V4 creates the **Community administrators** group and assigns it to existing staff to preserve their current access. Newly created staff need explicit grants.
- Permissions and account status are re-read on each operation, including callbacks from an already-open page.
- Staff cannot grant themselves privileges or change another staff/superuser's credentials.
- Administrators cannot delete their own account or remove their own administration access. At least one active superuser must remain.
- Sign-ins, sign-ups, activation and password reset emails, and Google sign-in starts are limited per network address, and sign-ins and emails per email address too; activation and reset emails are also capped at 100 an hour in all, to stay within the sending mailbox's limits. The counts are kept in the database as hashes, so every instance shares them.
- Password changes and deactivation increment the account's token version. Previously issued access and refresh tokens stop working, including after reactivation. Existing sessions are preserved when migration V5 is first applied.
- JWT authentication remains stateless. The separate HTTP session holds Vaadin UI state; its ID changes at successful password sign-in, not on every authenticated request. This preserves session-fixation protection without breaking concurrent uploads or navigation.
- Updates compare the current record revision with the editor's revision to prevent silent lost changes.

## Reports and deletion

Deactivation occurs at **five different reporting accounts**, not counting guest accounts. Repeated incidents by one person count once. Self-reporting is rejected. Editing, deleting, or cascading reports updates both affected targets. Removing a report does not reactivate an account automatically; a superuser must review and reactivate it explicitly after the count falls below five. Staff and superuser accounts are never deactivated by reports: their reports are kept for other administrators to review, so residents can't lock the administrators out. A save that leaves an account with five or more reporters with neither staff nor superuser status deactivates it, as a fifth report would.

Deletion requires an explicit confirmation with dependent-record counts and effects. The service checks permission to delete every registered model in the cascade, then verifies that the preview is still current. Bulk batches are limited to 100 selected roots and 5,000 dependent registered records. User deletion also repairs surviving vote totals and report counts. Database triggers remove orphaned post/issue content views.

Administration history stores the actor, action, record identity and before/after values in the same transaction as the change. Passwords and password hashes are excluded. Cascade deletions are recorded, and history survives deletion of the user or object. Database triggers prohibit editing or deleting individual audit rows. History begins at migration V4; it does not invent a history for earlier changes.

## Verification

`AdminManagementTest` exercises all registered resource lifecycles, permissions, stale edits, report thresholds, cascades, bulk rollback, uploads, search/paging, password hashing and append-only history against PostgreSQL. Existing authentication, voting, profile and safety regression tests remain part of the Maven test suite. Browser verification should also cover actual form submission and routing through the packaged app and Nginx.

Tests use Testcontainers and require a running Docker engine. If a local Docker installation rejects the default client API version, run Maven with `-Dapi.version=1.44`. `SecuritySessionRegressionTest` verifies stable UI sessions across JWT requests and session rotation at password sign-in.

Django infrastructure tables for Sites, Celery Beat and social-auth are not copied into the Spring database. Site settings and mail execution use Spring configuration and async services. Google OAuth remains outside this verification scope, as requested.
