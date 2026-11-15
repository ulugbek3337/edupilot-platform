CREATE TABLE group_enrollments (
 id BIGSERIAL PRIMARY KEY, center_id BIGINT NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
 group_id BIGINT NOT NULL REFERENCES study_groups(id) ON DELETE CASCADE,
 student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
 active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uk_group_enrollment UNIQUE(group_id,student_id)
);
CREATE INDEX idx_group_enrollments_center_group ON group_enrollments(center_id,group_id,active);
