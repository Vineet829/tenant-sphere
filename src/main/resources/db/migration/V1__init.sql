CREATE TABLE users (
    pkid          BIGSERIAL PRIMARY KEY,
    id            UUID         NOT NULL UNIQUE,
    email         VARCHAR(254) NOT NULL UNIQUE,
    username      VARCHAR(60)  NOT NULL UNIQUE,
    first_name    VARCHAR(60)  NOT NULL,
    last_name     VARCHAR(60)  NOT NULL,
    password      VARCHAR(255) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_staff      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_superuser  BOOLEAN      NOT NULL DEFAULT FALSE,
    date_joined   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_login    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);

CREATE TABLE profiles (
    pkid              BIGSERIAL PRIMARY KEY,
    id                UUID        NOT NULL UNIQUE,
    user_pkid         BIGINT      NOT NULL UNIQUE REFERENCES users (pkid) ON DELETE CASCADE,
    avatar            VARCHAR(500),
    gender            VARCHAR(10) NOT NULL DEFAULT 'other',
    bio               TEXT,
    occupation        VARCHAR(20) NOT NULL DEFAULT 'tenant',
    phone_number      VARCHAR(30) NOT NULL DEFAULT '+919997008000',
    country_of_origin VARCHAR(2)  NOT NULL DEFAULT 'IN',
    city_of_origin    VARCHAR(180) NOT NULL DEFAULT 'New Delhi',
    report_count      INTEGER     NOT NULL DEFAULT 0,
    reputation        INTEGER     NOT NULL DEFAULT 100,
    slug              VARCHAR(255) NOT NULL UNIQUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_profiles_gender CHECK (gender IN ('male', 'female', 'other')),
    CONSTRAINT chk_profiles_occupation CHECK (occupation IN
        ('mason', 'carpenter', 'plumber', 'roofer', 'painter',
         'electrician', 'hvac', 'tenant'))
);

CREATE TABLE apartments (
    pkid        BIGSERIAL PRIMARY KEY,
    id          UUID        NOT NULL UNIQUE,
    unit_number VARCHAR(10) NOT NULL UNIQUE,
    building    VARCHAR(50) NOT NULL,
    floor       INTEGER     NOT NULL CHECK (floor >= 0),
    tenant_pkid BIGINT      REFERENCES users (pkid) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_apartments_tenant ON apartments (tenant_pkid);

CREATE TABLE posts (
    pkid        BIGSERIAL PRIMARY KEY,
    id          UUID         NOT NULL UNIQUE,
    title       VARCHAR(250) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    body        TEXT         NOT NULL,
    author_pkid BIGINT       NOT NULL REFERENCES users (pkid) ON DELETE CASCADE,
    upvotes     INTEGER      NOT NULL DEFAULT 0 CHECK (upvotes >= 0),
    downvotes   INTEGER      NOT NULL DEFAULT 0 CHECK (downvotes >= 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_posts_author ON posts (author_pkid);
CREATE INDEX idx_posts_created_at ON posts (created_at DESC);

CREATE TABLE replies (
    pkid        BIGSERIAL PRIMARY KEY,
    id          UUID        NOT NULL UNIQUE,
    post_pkid   BIGINT      NOT NULL REFERENCES posts (pkid) ON DELETE CASCADE,
    author_pkid BIGINT      NOT NULL REFERENCES users (pkid) ON DELETE CASCADE,
    body        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_replies_post ON replies (post_pkid);

CREATE TABLE tags (
    pkid BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE post_tags (
    post_pkid BIGINT NOT NULL REFERENCES posts (pkid) ON DELETE CASCADE,
    tag_pkid  BIGINT NOT NULL REFERENCES tags (pkid) ON DELETE CASCADE,
    PRIMARY KEY (post_pkid, tag_pkid)
);

CREATE TABLE post_upvotes (
    post_pkid BIGINT NOT NULL REFERENCES posts (pkid) ON DELETE CASCADE,
    user_pkid BIGINT NOT NULL REFERENCES users (pkid) ON DELETE CASCADE,
    PRIMARY KEY (post_pkid, user_pkid)
);

CREATE TABLE post_downvotes (
    post_pkid BIGINT NOT NULL REFERENCES posts (pkid) ON DELETE CASCADE,
    user_pkid BIGINT NOT NULL REFERENCES users (pkid) ON DELETE CASCADE,
    PRIMARY KEY (post_pkid, user_pkid)
);

CREATE TABLE post_bookmarks (
    post_pkid BIGINT NOT NULL REFERENCES posts (pkid) ON DELETE CASCADE,
    user_pkid BIGINT NOT NULL REFERENCES users (pkid) ON DELETE CASCADE,
    PRIMARY KEY (post_pkid, user_pkid)
);

CREATE TABLE issues (
    pkid             BIGSERIAL PRIMARY KEY,
    id               UUID         NOT NULL UNIQUE,
    apartment_pkid   BIGINT       NOT NULL REFERENCES apartments (pkid) ON DELETE CASCADE,
    reported_by_pkid BIGINT       NOT NULL REFERENCES users (pkid) ON DELETE CASCADE,
    assigned_to_pkid BIGINT       REFERENCES users (pkid) ON DELETE SET NULL,
    title            VARCHAR(255) NOT NULL,
    description      TEXT         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'reported',
    priority         VARCHAR(20)  NOT NULL DEFAULT 'low',
    resolved_on      DATE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_issues_status CHECK (status IN ('reported', 'resolved', 'in_progress')),
    CONSTRAINT chk_issues_priority CHECK (priority IN ('low', 'medium', 'high'))
);

CREATE INDEX idx_issues_reported_by ON issues (reported_by_pkid);
CREATE INDEX idx_issues_assigned_to ON issues (assigned_to_pkid);

CREATE TABLE ratings (
    pkid             BIGSERIAL PRIMARY KEY,
    id               UUID        NOT NULL UNIQUE,
    rated_user_pkid  BIGINT      NOT NULL REFERENCES users (pkid) ON DELETE CASCADE,
    rating_user_pkid BIGINT      NOT NULL REFERENCES users (pkid) ON DELETE CASCADE,
    rating           INTEGER     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment          TEXT        NOT NULL DEFAULT '',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ratings_pair UNIQUE (rated_user_pkid, rating_user_pkid)
);

CREATE TABLE reports (
    pkid               BIGSERIAL PRIMARY KEY,
    id                 UUID         NOT NULL UNIQUE,
    title              VARCHAR(255) NOT NULL,
    slug               VARCHAR(255) NOT NULL UNIQUE,
    reported_by_pkid   BIGINT       NOT NULL REFERENCES users (pkid) ON DELETE CASCADE,
    reported_user_pkid BIGINT       NOT NULL REFERENCES users (pkid) ON DELETE CASCADE,
    description        TEXT         NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE content_views (
    pkid         BIGSERIAL PRIMARY KEY,
    id           UUID        NOT NULL UNIQUE,
    content_type VARCHAR(50) NOT NULL,
    object_pkid  BIGINT      NOT NULL,
    user_pkid    BIGINT      REFERENCES users (pkid) ON DELETE SET NULL,
    viewer_ip    VARCHAR(45),
    last_viewed  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_content_views UNIQUE (content_type, object_pkid, user_pkid, viewer_ip)
);

CREATE INDEX idx_content_views_target ON content_views (content_type, object_pkid);

CREATE TABLE verification_tokens (
    pkid       BIGSERIAL PRIMARY KEY,
    id         UUID         NOT NULL UNIQUE,
    user_pkid  BIGINT       NOT NULL REFERENCES users (pkid) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    purpose    VARCHAR(30)  NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_verification_purpose CHECK (purpose IN ('activation', 'password_reset'))
);

CREATE INDEX idx_verification_tokens_user ON verification_tokens (user_pkid);
