-- How many requests of one kind a network address or an email made in the
-- current window, keyed by a hash of the two, so no address or email is kept.
CREATE TABLE request_limits (
    key_hash     VARCHAR(64) PRIMARY KEY,
    window_start TIMESTAMPTZ NOT NULL,
    attempts     INTEGER     NOT NULL
);
CREATE INDEX idx_request_limits_window ON request_limits (window_start);
