CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    center_id BIGINT NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
    student_number VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(30),
    date_of_birth DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_students_center_number UNIQUE (center_id, student_number),
    CONSTRAINT chk_students_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_students_center_status_name
    ON students (center_id, status, last_name, first_name);
