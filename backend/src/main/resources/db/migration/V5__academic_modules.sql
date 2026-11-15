CREATE TABLE teachers (
 id BIGSERIAL PRIMARY KEY, center_id BIGINT NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
 teacher_code VARCHAR(50) NOT NULL, first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL,
 email VARCHAR(255), phone VARCHAR(30), specialization VARCHAR(120), status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uk_teachers_center_code UNIQUE(center_id,teacher_code),
 CONSTRAINT chk_teachers_status CHECK(status IN ('ACTIVE','INACTIVE'))
);
CREATE TABLE courses (
 id BIGSERIAL PRIMARY KEY, center_id BIGINT NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
 course_code VARCHAR(50) NOT NULL, name VARCHAR(150) NOT NULL, description VARCHAR(2000),
 active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uk_courses_center_code UNIQUE(center_id,course_code)
);
CREATE TABLE study_groups (
 id BIGSERIAL PRIMARY KEY, center_id BIGINT NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
 course_id BIGINT NOT NULL REFERENCES courses(id), teacher_id BIGINT NOT NULL REFERENCES teachers(id),
 group_code VARCHAR(50) NOT NULL, name VARCHAR(150) NOT NULL, starts_on DATE, ends_on DATE,
 active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uk_groups_center_code UNIQUE(center_id,group_code),
 CONSTRAINT chk_groups_dates CHECK(ends_on IS NULL OR starts_on IS NULL OR ends_on >= starts_on)
);
CREATE TABLE lessons (
 id BIGSERIAL PRIMARY KEY, center_id BIGINT NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
 group_id BIGINT NOT NULL REFERENCES study_groups(id), title VARCHAR(180) NOT NULL,
 description VARCHAR(2000), starts_at TIMESTAMP NOT NULL, ends_at TIMESTAMP NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT chk_lessons_time CHECK(ends_at > starts_at),
 CONSTRAINT chk_lessons_status CHECK(status IN ('SCHEDULED','COMPLETED','CANCELLED'))
);
CREATE TABLE attendance_records (
 id BIGSERIAL PRIMARY KEY, center_id BIGINT NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
 lesson_id BIGINT NOT NULL REFERENCES lessons(id), student_id BIGINT NOT NULL REFERENCES students(id),
 status VARCHAR(20) NOT NULL, note VARCHAR(1000), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uk_attendance_lesson_student UNIQUE(lesson_id,student_id),
 CONSTRAINT chk_attendance_status CHECK(status IN ('PRESENT','ABSENT','LATE','EXCUSED'))
);
CREATE TABLE grade_records (
 id BIGSERIAL PRIMARY KEY, center_id BIGINT NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
 student_id BIGINT NOT NULL REFERENCES students(id), group_id BIGINT NOT NULL REFERENCES study_groups(id),
 title VARCHAR(180) NOT NULL, score NUMERIC(6,2) NOT NULL, max_score NUMERIC(6,2) NOT NULL,
 comment VARCHAR(1000), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT chk_grade_score CHECK(score >= 0 AND max_score > 0 AND score <= max_score)
);
CREATE INDEX idx_teachers_center_status ON teachers(center_id,status);
CREATE INDEX idx_courses_center_active ON courses(center_id,active);
CREATE INDEX idx_groups_center_active ON study_groups(center_id,active);
CREATE INDEX idx_lessons_center_group_start ON lessons(center_id,group_id,starts_at);
CREATE INDEX idx_attendance_center_lesson ON attendance_records(center_id,lesson_id);
CREATE INDEX idx_grades_center_student ON grade_records(center_id,student_id);
