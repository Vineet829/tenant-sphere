Local Cedar Court sample data
============================

`python3 scripts/seed_society.py` previews the additions. Add `--apply` to populate the running local Spring/PostgreSQL app. It uses the `tenant_prod_postgres` Docker container by default; `--container` selects another local container with the same schema. The TenantSphere backend must be running on port 8000 for its Argon2 encoder to create a random, unrecorded password for the fictional accounts.

The dataset fills three Cedar Court blocks, each with six floors and six homes per floor, for 108 resident contacts. It retains the existing residents and guest account, adds technicians in all seven supported trades, and supplies community conversations, real vote relationships, bookmarks, maintenance history and reviews. It also adds activity visible in the guest account's own apartment and profile. Names and situations are fictional; addresses use the reserved `.example` domain.

All writes occur in one transaction. Stable UUIDs and insert-only records make repeated runs safe. Vote, reply, bookmark and view fixtures are created only for newly inserted posts, so rerunning the script does not restore votes or bookmarks someone removed. Existing content, passwords and profile edits are retained. The script sends no email, never runs automatically at application startup, and is separate from schema migrations.

Run `python3 scripts/verify_guest_society.py` to check the guest-facing API through the frontend on port 3000. Add `--check-voting` to check authorization, validation, explicit vote transitions, retries and persisted counts; it restores the selected post's original vote afterward. Frontend voting regressions run with `npm run test:voting` from `client`.

Profile photos
--------------

`python3 scripts/seed_avatars.py --with-identities` previews the portraits and curated profile details for the 122 fictional accounts in `demo_avatars.json`. Add `--apply` to update the local database. The 32 international resident identities in `demo_profile_identities.json` and the portrait assignments change in the same transaction. The remaining residents and technicians keep their existing names.

The identity update requires the recorded sample UUID, home, gender and original profile details to match. It preserves credentials, account IDs and all activity. Portrait updates fill empty fields or replace the exact previous bundled sample image recorded in the manifest. Later name edits and uploaded photos are preserved. Repeated runs make no further changes. For a fresh sample database, run the society seed first, then this portrait command. Omit `--with-identities` when only assigning photos to an already curated database.

The 384 × 384 JPEG avatars are stored in `client/public/assets/avatars/cedar-court/` and served by the frontend. Browsing the app makes no requests to an image provider. The backend stores each local image path in its existing `avatar` field, which is already used in directories, profiles, posts, replies and reviews. Resident and technician portraits show fictional people, not actual Cedar Court residents; the guest account uses a bot avatar. Every avatar has a distinct filename and a SHA-256 checksum in the manifest. A missing bundled file stops the seed rather than requesting a replacement from a service.

Run `python3 scripts/verify_guest_society.py --check-avatars` to verify the curated names and hometowns, photo consistency across the profile and activity APIs, every local portrait through the frontend, and distinct image content.

Names, countries of origin and portraits were chosen together for these fictional sample profiles.

The guest uses `testuser-bot-v5.jpg`, a robot avatar. Earlier versions remain available for cached responses.

The original imported portraits remain bundled so cached responses can still load them. Their source was [Random User Generator](https://randomuser.me/), whose [copyright notice](https://randomuser.me/copyright) identifies the authorized UI Faces collection. They are no longer assigned by the current manifest. No portrait is used as a product endorsement.
