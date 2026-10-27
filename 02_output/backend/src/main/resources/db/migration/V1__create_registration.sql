-- Contract: registration storage between the backend and PostgreSQL (BR-07, AR-06).
-- The Flyway migration V1 creates exactly this schema; later changes are new migrations.
-- Lengths follow D-14. Option and consent texts are stored as accepted, so the export
-- stays correct after the configuration changes (AR-04).

CREATE TABLE registration (
    id                 UUID         PRIMARY KEY,
    type               VARCHAR(16)  NOT NULL CHECK (type IN ('EXTERNAL', 'STUDENT')),
    first_name         VARCHAR(200) NOT NULL,
    last_name          VARCHAR(200) NOT NULL,
    email              VARCHAR(254) NOT NULL,
    organization       VARCHAR(200),
    study_institution  VARCHAR(200),
    study_programme    VARCHAR(200),
    student_id         VARCHAR(50),
    registered_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT registration_fields_by_type CHECK (
        (type = 'EXTERNAL' AND organization IS NOT NULL
            AND study_institution IS NULL AND study_programme IS NULL AND student_id IS NULL)
        OR
        (type = 'STUDENT' AND organization IS NULL
            AND study_institution IS NOT NULL AND study_programme IS NOT NULL AND student_id IS NOT NULL)
    )
);

CREATE INDEX registration_registered_at_idx ON registration (registered_at);

CREATE TABLE registration_option (
    registration_id  UUID         NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    position         INTEGER      NOT NULL,
    option_id        VARCHAR(64)  NOT NULL,
    option_name      VARCHAR(200) NOT NULL,
    category         VARCHAR(16)  NOT NULL CHECK (category IN ('WORKSHOP', 'EVENT', 'MEAL', 'OTHER')),
    PRIMARY KEY (registration_id, option_id),
    UNIQUE (registration_id, position)
);

CREATE TABLE registration_consent (
    registration_id  UUID          NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    consent_id       VARCHAR(64)   NOT NULL,
    consent_text     VARCHAR(1000) NOT NULL,
    given_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (registration_id, consent_id)
);
