-- When an account was first turned on. An account that never was is still
-- waiting for its activation email; any other account that is off was turned
-- off, by staff or by reports, and only staff can turn it on again.
ALTER TABLE users ADD COLUMN activated_at TIMESTAMPTZ;

-- Accounts that are on, have signed in, or were ever on in the console's
-- history were activated before, at the latest when they first signed in.
UPDATE users u SET activated_at = COALESCE(u.last_login, u.date_joined)
WHERE u.is_active
   OR u.last_login IS NOT NULL
   OR EXISTS (
        SELECT 1 FROM admin_audit_log h
        WHERE h.resource = 'users'
          AND h.object_pkid = u.pkid
          AND ((h.before_data ->> 'is_active') = 'true' OR (h.after_data ->> 'is_active') = 'true'));

-- Set however an account is turned on: by its activation link, by Google
-- sign-in, as a guest, or in the console.
CREATE FUNCTION remember_first_activation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.is_active AND NEW.activated_at IS NULL THEN
        NEW.activated_at := clock_timestamp();
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER users_first_activation BEFORE INSERT OR UPDATE OF is_active ON users
    FOR EACH ROW EXECUTE FUNCTION remember_first_activation();
