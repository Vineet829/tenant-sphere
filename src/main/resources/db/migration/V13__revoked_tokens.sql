-- Sign-in tokens signed out before they run out. Each stays refused until it
-- would have run out anyway, and is then removed.
CREATE TABLE revoked_tokens (
    token_id   UUID        PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_revoked_tokens_expiry ON revoked_tokens (expires_at);
