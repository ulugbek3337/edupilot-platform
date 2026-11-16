-- Notifications are tenant-owned records and their recipient must be a member
-- of the same center. Fail explicitly if legacy data violates this invariant.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM notifications n
        LEFT JOIN center_memberships cm
          ON cm.center_id = n.center_id
         AND cm.user_id = n.recipient_user_id
        WHERE cm.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot enforce notification tenant integrity: one or more recipients are not members of the notification center';
    END IF;
END $$;

ALTER TABLE notifications
    DROP CONSTRAINT notifications_recipient_user_id_fkey;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_center_recipient_membership
    FOREIGN KEY (center_id, recipient_user_id)
    REFERENCES center_memberships (center_id, user_id)
    ON DELETE CASCADE;
