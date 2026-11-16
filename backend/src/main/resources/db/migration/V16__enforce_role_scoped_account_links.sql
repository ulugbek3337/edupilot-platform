-- Bind student, teacher, and parent records to a role-specific membership
-- in the same center. Existing inconsistent data is rejected before constraints
-- are installed; operators must reconcile it explicitly rather than silently
-- reassigning accounts during deployment.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM students s
        WHERE s.user_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM center_memberships m
              WHERE m.center_id = s.center_id
                AND m.user_id = s.user_id
                AND m.role = 'STUDENT'
          )
    ) THEN
        RAISE EXCEPTION 'V16 blocked: student accounts without matching STUDENT membership exist';
    END IF;

    IF EXISTS (
        SELECT 1 FROM teachers t
        WHERE t.user_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM center_memberships m
              WHERE m.center_id = t.center_id
                AND m.user_id = t.user_id
                AND m.role = 'TEACHER'
          )
    ) THEN
        RAISE EXCEPTION 'V16 blocked: teacher accounts without matching TEACHER membership exist';
    END IF;

    IF EXISTS (
        SELECT 1 FROM parent_student_links p
        WHERE NOT EXISTS (
            SELECT 1 FROM center_memberships m
            WHERE m.center_id = p.center_id
              AND m.user_id = p.parent_user_id
              AND m.role = 'PARENT'
        )
    ) THEN
        RAISE EXCEPTION 'V16 blocked: parent links without matching PARENT membership exist';
    END IF;
END $$;

CREATE UNIQUE INDEX uk_center_membership_center_user_role
    ON center_memberships (center_id, user_id, role);

ALTER TABLE students
    ADD COLUMN membership_role VARCHAR(30) NOT NULL DEFAULT 'STUDENT',
    ADD CONSTRAINT chk_students_membership_role
        CHECK (membership_role = 'STUDENT'),
    DROP CONSTRAINT students_user_id_fkey,
    ADD CONSTRAINT fk_students_center_membership
        FOREIGN KEY (center_id, user_id, membership_role)
        REFERENCES center_memberships (center_id, user_id, role);

ALTER TABLE teachers
    ADD COLUMN membership_role VARCHAR(30) NOT NULL DEFAULT 'TEACHER',
    ADD CONSTRAINT chk_teachers_membership_role
        CHECK (membership_role = 'TEACHER'),
    DROP CONSTRAINT fk_teachers_user,
    ADD CONSTRAINT fk_teachers_center_membership
        FOREIGN KEY (center_id, user_id, membership_role)
        REFERENCES center_memberships (center_id, user_id, role);

ALTER TABLE parent_student_links
    ADD COLUMN parent_membership_role VARCHAR(30) NOT NULL DEFAULT 'PARENT',
    ADD CONSTRAINT chk_parent_links_membership_role
        CHECK (parent_membership_role = 'PARENT'),
    DROP CONSTRAINT parent_student_links_parent_user_id_fkey,
    DROP CONSTRAINT parent_student_links_student_id_fkey,
    ADD CONSTRAINT fk_parent_links_center_membership
        FOREIGN KEY (center_id, parent_user_id, parent_membership_role)
        REFERENCES center_memberships (center_id, user_id, role)
        ON DELETE CASCADE,
    ADD CONSTRAINT fk_parent_links_center_student
        FOREIGN KEY (center_id, student_id)
        REFERENCES students (center_id, id)
        ON DELETE CASCADE;
