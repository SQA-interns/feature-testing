-- Registrations (BR-07, AC-005-01). Schema changes only through Flyway (AR-06, ES-08).
create table registration (
    id                uuid primary key,
    seq               bigserial    not null unique,
    type              varchar(16)  not null check (type in ('EXTERNAL', 'STUDENT')),
    first_name        varchar(200) not null,
    last_name         varchar(200) not null,
    email             varchar(254) not null,
    organization      varchar(200),
    study_institution varchar(200),
    study_programme   varchar(200),
    student_id        varchar(200),
    search_text       varchar(1100) not null,
    registered_at     timestamptz  not null
);

create index registration_newest_idx on registration (registered_at desc, seq desc);

create table registration_option (
    registration_id uuid        not null references registration (id) on delete cascade,
    position        integer     not null,
    option_id       varchar(64) not null,
    primary key (registration_id, position),
    unique (registration_id, option_id)
);

create index registration_option_option_idx on registration_option (option_id);

create table registration_consent (
    registration_id uuid        not null references registration (id) on delete cascade,
    consent_id      varchar(64) not null,
    given_at        timestamptz not null,
    primary key (registration_id, consent_id)
);
