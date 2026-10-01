ALTER TABLE users ADD COLUMN is_guest BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN guest_expires_at TIMESTAMPTZ;
ALTER TABLE users ADD CONSTRAINT chk_guest_expiry CHECK (is_guest = (guest_expires_at IS NOT NULL));
CREATE INDEX idx_guests_expiry ON users(guest_expires_at) WHERE is_guest;
CREATE TABLE guest_login_limits (
    address_hash VARCHAR(64) PRIMARY KEY,
    window_start TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL
);

-- Retire the shared credential that was published by the old guest button.
-- Keep its authored community content, but revoke its password and all sessions.
UPDATE users SET is_active=FALSE, password='!retired-shared-guest', auth_version=auth_version+1
WHERE LOWER(email)='testuser@gmail.com';
