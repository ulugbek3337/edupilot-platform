CREATE TABLE parent_student_links (
 id BIGSERIAL PRIMARY KEY, center_id BIGINT NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
 parent_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
 active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uk_parent_student_center UNIQUE(center_id,parent_user_id,student_id)
);
CREATE TABLE notifications (
 id BIGSERIAL PRIMARY KEY, center_id BIGINT NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
 recipient_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 type VARCHAR(40) NOT NULL, title VARCHAR(180) NOT NULL, message VARCHAR(1500) NOT NULL,
 read_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE audit_logs (
 id BIGSERIAL PRIMARY KEY, center_id BIGINT NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
 actor_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
 action VARCHAR(80) NOT NULL, entity_type VARCHAR(80) NOT NULL, entity_id BIGINT,
 details VARCHAR(1500), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_parent_links_center_parent ON parent_student_links(center_id,parent_user_id,active);
CREATE INDEX idx_parent_links_center_student ON parent_student_links(center_id,student_id,active);
CREATE INDEX idx_notifications_center_user_created ON notifications(center_id,recipient_user_id,created_at DESC);
CREATE INDEX idx_audit_center_created ON audit_logs(center_id,created_at DESC);
