-- Supports tenant-scoped center listing by user and membership state.
CREATE INDEX IF NOT EXISTS idx_center_memberships_user_status_center
    ON center_memberships (user_id, status, center_id);

-- Supports membership authorization checks for a specific center and user.
CREATE INDEX IF NOT EXISTS idx_center_memberships_center_user_status
    ON center_memberships (center_id, user_id, status);
