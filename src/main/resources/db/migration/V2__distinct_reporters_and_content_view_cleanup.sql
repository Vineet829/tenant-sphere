CREATE INDEX idx_reports_target_reporter ON reports (reported_user_pkid, reported_by_pkid);

-- Preserve incident history while counting each reporting resident only once.
UPDATE profiles p
SET report_count = counts.total,
    reputation = GREATEST(0, 100 - counts.total * 20)
FROM (
    SELECT p2.pkid, COUNT(DISTINCT r.reported_by_pkid)::INTEGER AS total
    FROM profiles p2
    LEFT JOIN reports r ON r.reported_user_pkid = p2.user_pkid
        AND r.reported_by_pkid <> r.reported_user_pkid
    GROUP BY p2.pkid
) counts
WHERE p.pkid = counts.pkid;

-- Do not reactivate accounts automatically: inactivity can have other causes.

DELETE FROM content_views v
WHERE (v.content_type = 'post' AND NOT EXISTS (SELECT 1 FROM posts p WHERE p.pkid = v.object_pkid))
   OR (v.content_type = 'issue' AND NOT EXISTS (SELECT 1 FROM issues i WHERE i.pkid = v.object_pkid));

CREATE FUNCTION remove_deleted_content_views() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    DELETE FROM content_views WHERE content_type = TG_ARGV[0] AND object_pkid = OLD.pkid;
    RETURN OLD;
END;
$$;

-- Cover application deletions and deletions cascading from users/apartments alike.
CREATE TRIGGER post_content_views_cleanup AFTER DELETE ON posts
    FOR EACH ROW EXECUTE FUNCTION remove_deleted_content_views('post');
CREATE TRIGGER issue_content_views_cleanup AFTER DELETE ON issues
    FOR EACH ROW EXECUTE FUNCTION remove_deleted_content_views('issue');
