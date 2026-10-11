-- Titles written wholly in another script, such as Hindi, used to get an empty slug, or "-2" and on
-- for the ones after; and a post titled like one of the API's or the client's own pages, such as
-- "My posts", got that page's address. None of them could be opened. Each gets an address of its
-- own, made from its id so it can't clash with another.
UPDATE posts SET slug = 'post-' || replace(id::text, '-', '')
WHERE slug = '' OR slug ~ '^-[0-9]+$' OR slug IN ('my-posts', 'top-posts', 'popular-tags', 'update-post');

UPDATE tags SET slug = 'tag-' || replace(id::text, '-', '')
WHERE slug = '' OR slug ~ '^-[0-9]+$';

UPDATE reports SET slug = 'report-' || replace(id::text, '-', '')
WHERE slug = '' OR slug ~ '^-[0-9]+$';
