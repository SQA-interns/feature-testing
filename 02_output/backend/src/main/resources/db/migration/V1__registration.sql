-- Registration storage (BR-07, AR-06); identical to docs/02_contracts/database-schema.sql.
CREATE TABLE registration (
    id                UUID PRIMARY KEY,
    type              VARCHAR(16)  NOT NULL CHECK (type IN ('EXTERNAL', 'STUDENT')),
    first_name        VARCHAR(200) NOT NULL,
    last_name         VARCHAR(200) NOT NULL,
    email             VARCHAR(254) NOT NULL,
    organization      VARCHAR(200),
    study_institution VARCHAR(200),
    study_programme   VARCHAR(200),
    student_id        VARCHAR(200),
    registered_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE registration_option (
    registration_id UUID        NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    option_id       VARCHAR(64) NOT NULL,
    PRIMARY KEY (registration_id, option_id)
);

CREATE TABLE registration_consent (
    registration_id UUID        NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    consent_id      VARCHAR(64) NOT NULL,
    given_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (registration_id, consent_id)
);

CREATE INDEX registration_registered_at_idx ON registration (registered_at);
