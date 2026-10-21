-- Registration storage (docs/02_contracts/database.json); changed only by new migrations (AR-06)
create table registration (
    id                uuid primary key,
    type              varchar(16)  not null check (type in ('EXTERNAL', 'STUDENT')),
    first_name        varchar(200) not null,
    last_name         varchar(200) not null,
    email             varchar(254) not null,
    organization      varchar(200),
    study_institution varchar(200),
    study_programme   varchar(200),
    student_id        varchar(200),
    registered_at     timestamp with time zone not null
);

create index registration_registered_at_idx on registration (registered_at, id);

create table registration_option (
    registration_id uuid        not null references registration (id) on delete cascade,
    position        integer     not null,
    option_id       varchar(64) not null,
    primary key (registration_id, position),
    unique (registration_id, option_id)
);

create table registration_consent (
    registration_id uuid        not null references registration (id) on delete cascade,
    consent_id      varchar(64) not null,
    given_at        timestamp with time zone not null,
    primary key (registration_id, consent_id)
);
