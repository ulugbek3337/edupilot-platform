ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN student_account BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE students
    ADD COLUMN user_id BIGINT REFERENCES users(id);

CREATE UNIQUE INDEX uk_students_user_id ON students(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_students_center_user ON students(center_id, user_id);
