-- Google subjects are stable identities. Existing password accounts are deliberately
-- not linked by matching email; their owners must keep using password sign-in.
CREATE TABLE google_identities (
    subject VARCHAR(255) PRIMARY KEY,
    user_pkid BIGINT NOT NULL UNIQUE REFERENCES users(pkid) ON DELETE CASCADE
);
