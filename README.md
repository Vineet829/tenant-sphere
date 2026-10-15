# Tenant Sphere

Tenant Sphere is a community app for apartment complexes, where tenants report
maintenance issues and troublesome neighbors, technicians handle the issues
assigned to them, and tenants share posts with the community. It is
available at [tenant.confidental.online](https://tenant.confidental.online).

## Table of Contents

- [Why I Built It](#why-i-built-it)
- [Features](#features)
- [Roles](#roles)
- [Privacy](#privacy)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Tests](#tests)
- [Configuration](#configuration)
- [Known Limitations](#known-limitations)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

## Why I Built It

In my apartment complex, we reported problems through a shared WhatsApp
account, and messages often got lost. Technicians sometimes ignored requests,
and there was no simple way to report noisy or troublesome neighbors. Tenant
Sphere gives each of these a proper place. Issues are tracked and assigned,
technicians are rated on their work, and reports about a tenant lead to a
warning and, if enough neighbors report them, to their account being
deactivated.

## Features

- **Community Posts**: Tenants and staff write posts, and everyone signed in
  can reply and upvote or downvote. Filter posts by tag, see the top posts,
  and bookmark posts to read later.
- **Issue Reporting**: Report a problem with your apartment and follow its
  status. An administrator assigns it to a technician, who updates it as they
  work.
- **Tenant Reports**: Report a neighbor for misconduct. The first report sends
  them a warning email that doesn't say who reported them. Once five different
  people have reported someone, their account is deactivated automatically.
  A superuser can reactivate it after removing enough reports to bring the
  count below five. Reports from guest accounts don't count, and staff
  accounts are never deactivated by reports.
- **Tenants and Technicians**: Browse the tenants and technicians in your
  community, and rate technicians on their work.
- **Profiles**: Edit your profile and avatar, and see your posts, activity,
  and reputation.
- **Accounts**: Sign up with email activation, sign in with Google, and reset
  a forgotten password. Visitors can also look around with a temporary guest
  account that expires after 24 hours.
- **Administration**: Manage users, posts, issues, reports, ratings, tags,
  apartments, and groups in an admin console, with a change history.

## Roles

- **Tenant**: Every new account starts as a tenant.
- **Technician**: Choosing a trade on your profile, such as plumber or
  electrician, lists you as a technician. Staff and guest accounts aren't
  listed.
- **Administrator**: Staff accounts can see, change, and delete every issue.
  In the admin console they work with the permissions they are given, such as
  assigning issues to technicians. Superusers, like the account
  `make superuser` creates, can do everything, including managing groups and
  switching accounts on or off.

## Privacy

- Anyone signed in, including guest accounts when guest access is on, can see
  the name, username, avatar, bio, gender, home country and city, occupation,
  reputation, average rating, join date, and apartment unit, building, and
  floor of every active tenant and technician. Staff and guest accounts aren't
  listed. Email addresses and phone numbers aren't shown.
- The list of posts, the top posts, popular tags, and posts by tag can be read
  without signing in, including each author's username and avatar.
- A report is seen only by the person who filed it and the administrators.
  The warning email may include the report's title, but never who filed it.
  Each different reporter, other than guests, lowers the reported user's
  reputation by 20 points, down to 0, and reputation is visible to anyone
  signed in.
- An issue is seen only by the tenant who reported it, the assigned
  technician, and staff.

## Architecture

- **Client**: A Next.js 15 app with React 19, Redux Toolkit, and Tailwind CSS.
- **Backend**: A Java 21 and Spring Boot 3.5 API on PostgreSQL 16, with Flyway
  migrations. It sends email over SMTP and serves the admin console, built
  with Vaadin.
- **Docker**: nginx sits in front of both. It sends the API (`/api/v1`), the
  admin console (`/admin` and `/VAADIN`), and uploaded files (`/media`) to the
  backend, and the site's pages to the client.

## Getting Started

The commands below are for macOS and Linux. On Windows, use WSL.

### Prerequisites

- Docker and make
- Java 21, Maven, and Node.js 22 (22.13 or newer) to run the backend and
  client directly

### Run with Docker

Copy the example settings, then set `POSTGRES_PASSWORD`, `SIGNING_KEY` from
`openssl rand -hex 32`, and `DOMAIN=http://localhost:8080` in
`.envs/.env.local`. The other values can stay as they are, because `local.yml`
sends mail to Mailpit and allows cookies over plain HTTP.

```bash
cp .envs/.env.example .envs/.env.local
make build
```

The first build takes a few minutes. Then open <http://localhost:8080>. Emails
go to Mailpit at <http://localhost:8025>. To stop the app, run `make down`, or
`make down-v` to delete the data as well.

### Create an Administrator

```bash
make superuser EMAIL=admin@example.com USERNAME=community_admin
```

It asks for the password. Then sign in to the admin console at
<http://localhost:8080/admin/login>. The
[administration guide](docs/ADMINISTRATION.md) has more.

### Run the Backend and Client Directly

Use this when working on the code, so you don't rebuild the backend's Docker
image after each change. If the full stack from `make build` is running, stop
it first with `make down`, because its client container also writes to
`client/.next`. PostgreSQL and Mailpit still run in Docker, with the same
`POSTGRES_PASSWORD` and `SIGNING_KEY` as in `.envs/.env.local`. The first
start of the backend also installs the admin console's frontend packages, so
it takes longer.

```bash
# Terminal 1: PostgreSQL and Mailpit, then the API on port 8000
docker compose -f local.yml up -d postgres mailpit
export POSTGRES_USER=tenantsphere POSTGRES_PASSWORD="your-database-password"
export SIGNING_KEY="your-signing-key"
export COOKIE_SECURE=false DOMAIN=http://localhost:3000
mvn spring-boot:run
```

```bash
# Terminal 2: the client on port 3000
cd client
npm install
npm run dev
```

Open <http://localhost:3000>. The client passes API and admin requests to the
backend on port 8000.

## Tests

Stop the client's dev server first, or the Docker stack with `make down`:
`npm run build` writes to `client/.next` too.

```bash
mvn test                  # backend, needs Docker for PostgreSQL

cd client                 # after npm install
npm run lint
npx tsc --noEmit
npm run test:regressions
npm run build
```

## Configuration

Start from `.envs/.env.example`, which explains most settings. Copy it to
`.envs/.env.local` for `local.yml`, or to `.envs/.env.production` for
`production.yml`. For production:

- **Keys**: Set a strong `POSTGRES_PASSWORD` and a `SIGNING_KEY` from
  `openssl rand -hex 32`. Changing the signing key signs everyone out.
- **Domain**: Set `DOMAIN` to the site's https address, which emails use for
  their links, and keep `COOKIE_SECURE=true`.
- **HTTPS**: `production.yml` is a starting point that serves HTTPS with Let's
  Encrypt certificates. It doesn't request them, so get them first, for
  example with certbot, into `/etc/letsencrypt/live/` on the server. Set
  `SERVER_NAME` to the site's host name, such as `example.com`, in the shell
  or in a `.env` file beside `production.yml`. nginx uses it for
  `server_name` and the certificate paths.
- **Email**: Set the `EMAIL_*` settings and `DEFAULT_FROM_EMAIL` for a real
  mail server, such as Gmail with an app password on port 587. The settings
  cover STARTTLS; `EMAIL_USE_SSL` has no effect.
- **Google Sign-In**: Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`, and
  set `REDIRECT_URIS` to your site's address followed by `/google`.
- **Guest Access**: Set `GUEST_LOGIN_ENABLED=true` to give each visitor a
  temporary account that expires after 24 hours.

## Known Limitations

- Each installation serves one apartment complex.
- There are no in-app notifications. Emails cover sign-up and passwords,
  report warnings and deactivations, and issues being logged and resolved.
  An administrator switching an account off or on, or setting its password,
  in the admin console sends no email.

## Project Structure

- `src/`: The Spring Boot backend and the admin console.
- `client/`: The Next.js client.
- `package.json`, `vite.config.ts`, and `tsconfig.json` at the root: The admin
  console's frontend, which Vaadin builds.
- `docker/`: The backend, PostgreSQL, and nginx Dockerfiles and the nginx
  settings for `local.yml` and `production.yml`. The client's Dockerfiles are
  in `client/docker/`.
- `scripts/`: Scripts that fill a community with sample data.
- `docs/ADMINISTRATION.md`: The admin console guide.

## Contributing

Bug reports and questions are welcome as
[issues](https://github.com/Vineet829/tenant-sphere/issues).

## License

[MIT](LICENSE) © 2024-2026 Vineet Singh.
