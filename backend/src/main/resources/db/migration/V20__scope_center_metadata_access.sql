-- Centers expose only the selected center or centers associated with the
-- authenticated user's own memberships. Track the creator so new center rows
-- can be returned safely before the initial admin membership is inserted.
ALTER TABLE centers
    ADD COLUMN created_by_user_id BIGINT;

UPDATE centers center_row
SET created_by_user_id = (
    SELECT membership.user_id
    FROM center_memberships membership
    WHERE membership.center_id = center_row.id
    ORDER BY CASE WHEN membership.role = 'CENTER_ADMIN' THEN 0 ELSE 1 END,
             membership.created_at ASC,
             membership.id ASC
    LIMIT 1
)
WHERE center_row.created_by_user_id IS NULL;

DO $migration$
BEGIN
    IF EXISTS (SELECT 1 FROM centers WHERE created_by_user_id IS NULL) THEN
        RAISE EXCEPTION 'V20 blocked: one or more centers have no membership owner; assign an authorized creator before enabling center RLS';
    END IF;
END;
$migration$;

ALTER TABLE centers
    ALTER COLUMN created_by_user_id SET NOT NULL;

ALTER TABLE centers
    ADD CONSTRAINT fk_centers_created_by_user
    FOREIGN KEY (created_by_user_id)
    REFERENCES users(id)
    ON DELETE RESTRICT;

CREATE INDEX idx_centers_created_by_user
    ON centers(created_by_user_id);

ALTER TABLE centers ENABLE ROW LEVEL SECURITY;
ALTER TABLE centers FORCE ROW LEVEL SECURITY;

CREATE POLICY centers_select_scoped
    ON centers
    FOR SELECT
    USING (
        id = NULLIF(current_setting('app.current_center_id', true), '')::BIGINT
        OR created_by_user_id = NULLIF(current_setting('app.current_user_id', true), '')::BIGINT
        OR EXISTS (
            SELECT 1
            FROM center_memberships membership
            WHERE membership.center_id = centers.id
              AND membership.user_id = NULLIF(current_setting('app.current_user_id', true), '')::BIGINT
        )
    );

CREATE POLICY centers_insert_by_creator
    ON centers
    FOR INSERT
    WITH CHECK (
        created_by_user_id = NULLIF(current_setting('app.current_user_id', true), '')::BIGINT
    );

-- No runtime UPDATE or DELETE policy is added: the current API has no center
-- rename, transfer, or deletion workflow. Add explicit admin workflows later.
