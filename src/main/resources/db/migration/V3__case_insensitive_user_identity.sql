-- Match registration/profile validation even when requests arrive concurrently.
CREATE UNIQUE INDEX uq_users_email_case_insensitive ON users (LOWER(email));
CREATE UNIQUE INDEX uq_users_username_case_insensitive ON users (LOWER(username));
