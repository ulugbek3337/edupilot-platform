-- Enforce tenant consistency for enrollment relationships at the database boundary.
-- The globally unique IDs remain primary keys; these composite unique indexes
-- allow PostgreSQL to validate that each related row belongs to the same center.
CREATE UNIQUE INDEX uk_students_center_id_id
    ON students (center_id, id);

CREATE UNIQUE INDEX uk_study_groups_center_id_id
    ON study_groups (center_id, id);

ALTER TABLE group_enrollments
    DROP CONSTRAINT group_enrollments_group_id_fkey,
    DROP CONSTRAINT group_enrollments_student_id_fkey;

ALTER TABLE group_enrollments
    ADD CONSTRAINT fk_group_enrollments_group_center
        FOREIGN KEY (center_id, group_id)
        REFERENCES study_groups (center_id, id)
        ON DELETE CASCADE,
    ADD CONSTRAINT fk_group_enrollments_student_center
        FOREIGN KEY (center_id, student_id)
        REFERENCES students (center_id, id)
        ON DELETE CASCADE;
