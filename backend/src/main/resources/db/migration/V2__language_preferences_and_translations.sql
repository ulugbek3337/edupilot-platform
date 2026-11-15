ALTER TABLE users
    ADD COLUMN language_preference VARCHAR(2) NOT NULL DEFAULT 'uz';

ALTER TABLE users
    ADD CONSTRAINT chk_users_language_preference
    CHECK (language_preference IN ('uz', 'ru'));

CREATE TABLE content_translations (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(30) NOT NULL,
    entity_id BIGINT NOT NULL,
    field_name VARCHAR(80) NOT NULL,
    language_code VARCHAR(2) NOT NULL,
    translated_value TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_content_translations_language
        CHECK (language_code IN ('uz', 'ru')),
    CONSTRAINT uq_content_translation
        UNIQUE (entity_type, entity_id, field_name, language_code)
);

CREATE INDEX idx_content_translations_lookup
    ON content_translations (entity_type, entity_id, language_code);
