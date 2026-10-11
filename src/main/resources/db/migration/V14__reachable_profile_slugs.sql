-- A username made only of underscores, which sign-up allows, gave its profile an empty slug, or
-- "-1" and on for the ones after, and the console's command one starting with a hyphen. The empty
-- one made an address nothing could be found at. Each gets an address of its own, made from its id
-- so it can't clash with another.
UPDATE profiles SET slug = 'profile-' || replace(id::text, '-', '')
WHERE slug = '' OR slug LIKE '-%';
