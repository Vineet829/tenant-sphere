ALTER TABLE users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0 CHECK (auth_version >= 0);
