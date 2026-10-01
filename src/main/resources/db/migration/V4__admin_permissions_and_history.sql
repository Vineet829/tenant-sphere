CREATE TABLE admin_groups (
    pkid BIGSERIAL PRIMARY KEY,
    id UUID NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_admin_groups_name ON admin_groups (LOWER(name));

CREATE TABLE admin_group_permissions (
    group_pkid BIGINT NOT NULL REFERENCES admin_groups(pkid) ON DELETE CASCADE,
    permission VARCHAR(80) NOT NULL,
    PRIMARY KEY (group_pkid, permission)
);
CREATE TABLE admin_user_groups (
    user_pkid BIGINT NOT NULL REFERENCES users(pkid) ON DELETE CASCADE,
    group_pkid BIGINT NOT NULL REFERENCES admin_groups(pkid) ON DELETE CASCADE,
    PRIMARY KEY (user_pkid, group_pkid)
);
CREATE TABLE admin_user_permissions (
    user_pkid BIGINT NOT NULL REFERENCES users(pkid) ON DELETE CASCADE,
    permission VARCHAR(80) NOT NULL,
    PRIMARY KEY (user_pkid, permission)
);

-- Preserve access for existing staff. New staff need explicit grants, as in Django.
INSERT INTO admin_groups (id, name)
VALUES ('38403e42-5ae0-44b7-8381-f761682ac4c0', 'Community administrators');
INSERT INTO admin_group_permissions (group_pkid, permission)
SELECT g.pkid, resource || '.' || action
FROM admin_groups g
CROSS JOIN unnest(ARRAY['users','profiles','apartments','issues','posts','ratings','reports','content_views']) resource
CROSS JOIN unnest(ARRAY['view','add','change','delete']) action
WHERE g.id = '38403e42-5ae0-44b7-8381-f761682ac4c0';
INSERT INTO admin_user_groups (user_pkid, group_pkid)
SELECT u.pkid, g.pkid FROM users u CROSS JOIN admin_groups g
WHERE u.is_staff AND g.id = '38403e42-5ae0-44b7-8381-f761682ac4c0';

CREATE TABLE admin_audit_log (
    pkid BIGSERIAL PRIMARY KEY,
    actor_id UUID,
    actor_label VARCHAR(254) NOT NULL,
    resource VARCHAR(80) NOT NULL,
    object_pkid BIGINT NOT NULL,
    object_label TEXT NOT NULL,
    action VARCHAR(30) NOT NULL,
    before_data JSONB,
    after_data JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admin_history_object ON admin_audit_log(resource, object_pkid, pkid DESC);
CREATE INDEX idx_admin_history_time ON admin_audit_log(pkid DESC);

-- History intentionally survives deletion of an account or of the managed record.
CREATE FUNCTION protect_admin_audit_log() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Administration history is append-only';
END;
$$;
CREATE TRIGGER admin_history_immutable BEFORE UPDATE OR DELETE ON admin_audit_log
    FOR EACH ROW EXECUTE FUNCTION protect_admin_audit_log();
