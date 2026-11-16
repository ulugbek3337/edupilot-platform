-- Add composite tenant-aware foreign keys across the academic data model.
-- These constraints prevent a row belonging to one center from referencing
-- teachers, courses, groups, lessons, or students owned by another center.
CREATE UNIQUE INDEX uk_teachers_center_id_id
    ON teachers (center_id, id);

CREATE UNIQUE INDEX uk_courses_center_id_id
    ON courses (center_id, id);

CREATE UNIQUE INDEX uk_lessons_center_id_id
    ON lessons (center_id, id);

ALTER TABLE study_groups
    DROP CONSTRAINT study_groups_course_id_fkey,
    DROP CONSTRAINT study_groups_teacher_id_fkey;

ALTER TABLE study_groups
    ADD CONSTRAINT fk_study_groups_course_center
        FOREIGN KEY (center_id, course_id)
        REFERENCES courses (center_id, id),
    ADD CONSTRAINT fk_study_groups_teacher_center
        FOREIGN KEY (center_id, teacher_id)
        REFERENCES teachers (center_id, id);

ALTER TABLE lessons
    DROP CONSTRAINT lessons_group_id_fkey;

ALTER TABLE lessons
    ADD CONSTRAINT fk_lessons_group_center
        FOREIGN KEY (center_id, group_id)
        REFERENCES study_groups (center_id, id);

ALTER TABLE attendance_records
    DROP CONSTRAINT attendance_records_lesson_id_fkey,
    DROP CONSTRAINT attendance_records_student_id_fkey;

ALTER TABLE attendance_records
    ADD CONSTRAINT fk_attendance_lesson_center
        FOREIGN KEY (center_id, lesson_id)
        REFERENCES lessons (center_id, id),
    ADD CONSTRAINT fk_attendance_student_center
        FOREIGN KEY (center_id, student_id)
        REFERENCES students (center_id, id);

ALTER TABLE grade_records
    DROP CONSTRAINT grade_records_student_id_fkey,
    DROP CONSTRAINT grade_records_group_id_fkey;

ALTER TABLE grade_records
    ADD CONSTRAINT fk_grade_student_center
        FOREIGN KEY (center_id, student_id)
        REFERENCES students (center_id, id),
    ADD CONSTRAINT fk_grade_group_center
        FOREIGN KEY (center_id, group_id)
        REFERENCES study_groups (center_id, id);
