ALTER TABLE teachers ADD COLUMN user_id BIGINT;
ALTER TABLE teachers ADD CONSTRAINT fk_teachers_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
CREATE UNIQUE INDEX uk_teachers_user_id ON teachers(user_id) WHERE user_id IS NOT NULL;
