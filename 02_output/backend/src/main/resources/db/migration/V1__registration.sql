-- Registration storage (docs/02_contracts/database.md, AR-06)
CREATE TABLE registration (
    id                uuid         PRIMARY KEY,
    type              varchar(16)  NOT NULL CHECK (type IN ('EXTERNAL', 'STUDENT')),
    first_name        varchar(100) NOT NULL,
    last_name         varchar(100) NOT NULL,
    email             varchar(254) NOT NULL,
    organization      varchar(200),
    study_institution varchar(200),
    study_programme   varchar(200),
    student_id        varchar(50),
    registered_at     timestamptz  NOT NULL
);

CREATE INDEX registration_registered_at_idx ON registration (registered_at DESC, id DESC);

CREATE TABLE registration_option (
    registration_id uuid        NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    option_id       varchar(64) NOT NULL,
    PRIMARY KEY (registration_id, option_id)
);

CREATE INDEX registration_option_option_idx ON registration_option (option_id);

CREATE TABLE registration_consent (
    registration_id uuid        NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    consent_id      varchar(64) NOT NULL,
    given_at        timestamptz NOT NULL,
    PRIMARY KEY (registration_id, consent_id)
);
