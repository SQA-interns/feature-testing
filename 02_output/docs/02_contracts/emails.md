# Email contract

> Written in: phase 2 · Source: `docs/02_specification.md` 6.1, 6.2, SR-05, SR-07 · Agent: writes

Sent over SMTP (`SMTP_HOST`, `SMTP_PORT`, `SMTP_TLS`, optional `SMTP_USERNAME`/`SMTP_PASSWORD`) from `MAIL_FROM`, only after the registration and its JSON copy are committed (AR-05). Connect, read and write timeouts 5 s. A failure is logged as `Email for registration <id> failed` and never changes the API answer (OQ-03).

## Participant confirmation (US-006)

| Part | Value |
|---|---|
| To | the registration's `email` (validated: no whitespace or control characters) |
| Subject | `Registration confirmed: <CONFERENCE_NAME>` (configuration only, no user input) |
| Body | `text/plain; charset=UTF-8`; lines: greeting with first and last name, `Your registration for <CONFERENCE_NAME> was received.`, `Registration id: <id>`, `Selected options:` followed by one `- <option name>` per option (or `- none`) |

## Organizer notification (US-007)

| Part | Value |
|---|---|
| To | every address in `ORGANIZER_EMAILS` (comma separated, trimmed) |
| Subject | `New registration: <CONFERENCE_NAME>` |
| Body | `text/plain; charset=UTF-8`; `<label>: <value>` lines for id, type, first name, last name, email, the type's institution fields, options (ids), consents (ids), registered at |
| Attachment | `registration-<id>.json`, `application/json`, the exact bytes of the stored JSON copy (AC-007-02) |

User input appears only in plain text bodies, so line breaks, header-like text and markup stay literal text (AC-006-02); no header is built from user input except the participant address, which V-03 restricts.
