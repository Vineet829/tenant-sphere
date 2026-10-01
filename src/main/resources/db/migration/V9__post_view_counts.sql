-- Top posts sort by net score, then views. Keep the view total on the post so the ranking is an
-- index scan instead of a correlated count over content_views for every post.
ALTER TABLE posts ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0;

UPDATE posts p
SET view_count = v.total
FROM (
    SELECT object_pkid, COUNT(*)::INTEGER AS total
    FROM content_views
    WHERE content_type = 'post'
    GROUP BY object_pkid
) v
WHERE p.pkid = v.object_pkid;

-- Triggers cover every writer: the view recorder, admin edits, deletions and cascades.
CREATE FUNCTION maintain_post_view_count() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP IN ('DELETE', 'UPDATE') AND OLD.content_type = 'post' THEN
        UPDATE posts SET view_count = view_count - 1 WHERE pkid = OLD.object_pkid;
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') AND NEW.content_type = 'post' THEN
        UPDATE posts SET view_count = view_count + 1 WHERE pkid = NEW.object_pkid;
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER content_views_post_count
    AFTER INSERT OR DELETE ON content_views
    FOR EACH ROW EXECUTE FUNCTION maintain_post_view_count();

-- A repeat view only refreshes timestamps; count only moves between targets.
CREATE TRIGGER content_views_post_count_moved
    AFTER UPDATE OF content_type, object_pkid ON content_views
    FOR EACH ROW
    WHEN (OLD.content_type IS DISTINCT FROM NEW.content_type
          OR OLD.object_pkid IS DISTINCT FROM NEW.object_pkid)
    EXECUTE FUNCTION maintain_post_view_count();

CREATE INDEX idx_posts_top ON posts ((upvotes - downvotes) DESC, view_count DESC, created_at DESC, pkid DESC);
