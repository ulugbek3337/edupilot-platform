-- Supports tenant-scoped center listing by user and membership state.
CREATE INDEX IF NOT EXISTS idx_center_memberships_user_status_center
    ON center_memberships (user_id, status, center_id);
