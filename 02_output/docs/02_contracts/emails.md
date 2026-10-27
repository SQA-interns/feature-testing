# Email contract

> Written in: phase 2 · Source: US-006, US-007, SR-05, SR-07, OQ-03 · Agent: writes

Interface between the backend and the SMTP server. Both messages are sent only after the database row and the JSON copy exist (AR-05, OQ-03), one SMTP message per recipient. A send failure is logged as `registration <id>: <participant|organizer> email not sent` with no personal data or exception text containing addresses (ES-07) and does not change the 201 response (AC-006-03).

## Common rules (SR-05)

- `Content-Type: text/plain; charset=UTF-8` only, never HTML: markup in user input appears as literal text (AC-006-02).
- The subject contains only configured text (conference name) and never user input.
- The only user-supplied header value is the participant's `To` address, which has already passed the email validation (one address, no CR/LF, no comma-separated list) and is set as a single parsed `InternetAddress`.
- User text in the body has CR and LF replaced by a space before it is written, so a value cannot add lines that look like other fields.
- `From`: `MAIL_FROM`. No `Cc`, `Bcc` or `Reply-To`.

## Participant confirmation (US-006)

| Part | Content |
|---|---|
| To | the participant's email |
| Subject | `Registration confirmed: <conference name>` |
| Body | Conference name; `Dear <first name> <last name>,`; registration id; registration type (`External participant` / `Student`); selected option names grouped by category (Workshops, Events, Meals, Other activities), or `No options selected.` |

## Organizer notification (US-007)

| Part | Content |
|---|---|
| To | each address in `ORGANIZER_EMAILS` (one message each) |
| Subject | `New registration: <conference name>` |
| Body | Registration id, type, registration time (UTC, ISO 8601), every participant field with its label, selected options by category, consents with the time given. Only registration data (SR-07). |
| Attachment | `registration-<id>.json`, `application/json; charset=UTF-8`, the exact bytes of the stored JSON copy (AC-007-02) |
