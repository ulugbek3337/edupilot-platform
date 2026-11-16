-- Memberships are the bridge between global identities and tenant centers.
-- Allow a user to read only their own memberships before choosing a center, or
-- tenant-scoped services to read memberships inside the already validated center.
-- All membership writes require an explicit center context.
ALTER TABLE center_memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE center_memberships FORCE ROW LEVEL SECURITY;

CREATE POLICY center_memberships_select
    ON center_memberships
    FOR SELECT
    USING (
        center_id = NULLIF(current_setting('app.current_center_id', true), '')::BIGINT
        OR user_id = NULLIF(current_setting('app.current_user_id', true), '')::BIGINT
    );

CREATE POLICY center_memberships_insert
    ON center_memberships
    FOR INSERT
    WITH CHECK (
        center_id = NULLIF(current_setting('app.current_center_id', true), '')::BIGINT
    );

CREATE POLICY center_memberships_update
    ON center_memberships
    FOR UPDATE
    USING (
        center_id = NULLIF(current_setting('app.current_center_id', true), '')::BIGINT
    )
    WITH CHECK (
        center_id = NULLIF(current_setting('app.current_center_id', true), '')::BIGINT
    );

CREATE POLICY center_memberships_delete
    ON center_memberships
    FOR DELETE
    USING (
        center_id = NULLIF(current_setting('app.current_center_id', true), '')::BIGINT
    );

-- Refresh tokens are global-user records, not center records. Their owner can
-- read/rotate/revoke only their own rows; the migration role bypasses RLS only
-- during trusted schema/data migrations and is never used by the application.
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens FORCE ROW LEVEL SECURITY;

CREATE POLICY refresh_tokens_owner_only
    ON refresh_tokens
    USING (
        user_id = NULLIF(current_setting('app.current_user_id', true), '')::BIGINT
    )
    WITH CHECK (
        user_id = NULLIF(current_setting('app.current_user_id', true), '')::BIGINT
    );
