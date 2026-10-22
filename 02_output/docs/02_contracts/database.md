# Database contract

> Written in: phase 2 · Source: `docs/02_specification.md` 5.1, AR-06 · Agent: writes

PostgreSQL, UTF-8. Created only by Flyway migrations in `backend/src/main/resources/db/migration` (AR-06); the first is `V1__registration.sql`. JPA validates against it (`ddl-auto=validate`).

| Table | Column | Type | Null | Notes |
|---|---|---|---|---|
| `registration` | `id` | `uuid` | no | primary key |
| | `type` | `varchar(16)` | no | `EXTERNAL` or `STUDENT` (check constraint) |
| | `first_name` | `varchar(100)` | no | trimmed |
| | `last_name` | `varchar(100)` | no | trimmed |
| | `email` | `varchar(254)` | no | trimmed |
| | `organization` | `varchar(200)` | yes | `EXTERNAL` only |
| | `study_institution` | `varchar(200)` | yes | `STUDENT` only |
| | `study_programme` | `varchar(200)` | yes | `STUDENT` only |
| | `student_id` | `varchar(50)` | yes | `STUDENT` only |
| | `registered_at` | `timestamptz` | no | index `registration_registered_at_idx (registered_at desc, id desc)` |
| `registration_option` | `registration_id` | `uuid` | no | FK `registration(id)` on delete cascade |
| | `option_id` | `varchar(64)` | no | primary key (`registration_id`, `option_id`); index on `option_id` |
| `registration_consent` | `registration_id` | `uuid` | no | FK `registration(id)` on delete cascade |
| | `consent_id` | `varchar(64)` | no | primary key (`registration_id`, `consent_id`) |
| | `given_at` | `timestamptz` | no | |

Option and consent ids are the configuration's identifiers (`conference-config.schema.json`); they are not foreign keys, so a stored registration survives a configuration change. All queries are parameterised (SB-05).
