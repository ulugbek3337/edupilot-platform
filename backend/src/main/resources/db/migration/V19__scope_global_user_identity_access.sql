-- The users table contains global identity data, including password hashes.
-- Permit authentication lookups for the supplied email, self-service access to
-- the authenticated user, and access to identities that are members of the
-- currently selected tenant. Do not expose unrelated tenant identities.
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

CREATE POLICY users_select_scoped
    ON users
    FOR SELECT
    USING (
        id = NULLIF(current_setting('app.current_user_id', true), '')::BIGINT
        OR email = NULLIF(current_setting('app.current_user_email', true), '')
        OR EXISTS (
            SELECT 1
            FROM center_memberships membership
            WHERE membership.user_id = users.id
              AND membership.center_id = NULLIF(current_setting('app.current_center_id', true), '')::BIGINT
        )
    );

CREATE POLICY users_insert_own_identity
    ON users
    FOR INSERT
    WITH CHECK (
        email = NULLIF(current_setting('app.current_user_email', true), '')
    );

CREATE POLICY users_update_scoped
    ON users
    FOR UPDATE
    USING (
        id = NULLIF(current_setting('app.current_user_id', true), '')::BIGINT
        OR email = NULLIF(current_setting('app.current_user_email', true), '')
        OR EXISTS (
            SELECT 1
            FROM center_memberships membership
            WHERE membership.user_id = users.id
              AND membership.center_id = NULLIF(current_setting('app.current_center_id', true), '')::BIGINT
        )
    )
    WITH CHECK (
        id = NULLIF(current_setting('app.current_user_id', true), '')::BIGINT
        OR email = NULLIF(current_setting('app.current_user_email', true), '')
        OR EXISTS (
            SELECT 1
            FROM center_memberships membership
            WHERE membership.user_id = users.id
              AND membership.center_id = NULLIF(current_setting('app.current_center_id', true), '')::BIGINT
        )
    );

-- No DELETE policy is provided to the runtime role. Account deactivation is
-- represented by status/disabled fields and membership revocation.
