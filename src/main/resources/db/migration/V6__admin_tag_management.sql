-- Taggit exposed tags in Django admin. Retain their management in Spring.
ALTER TABLE tags ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE;
ALTER TABLE tags ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE tags ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
INSERT INTO admin_group_permissions(group_pkid,permission)
SELECT pkid, 'tags.' || action FROM admin_groups
CROSS JOIN unnest(ARRAY['view','add','change','delete']) action
WHERE id='38403e42-5ae0-44b7-8381-f761682ac4c0';
