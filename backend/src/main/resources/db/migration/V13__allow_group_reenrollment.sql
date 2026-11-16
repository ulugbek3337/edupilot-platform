-- Keep at most one active enrollment per student and group while preserving
-- historical inactive enrollment rows for audit/history.
ALTER TABLE group_enrollments
    DROP CONSTRAINT uk_group_enrollment;

CREATE UNIQUE INDEX uk_group_enrollment_active
    ON group_enrollments (group_id, student_id)
    WHERE active = TRUE;
