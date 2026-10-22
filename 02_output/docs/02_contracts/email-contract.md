# Email contract

> Written in: phase 2 · Source: US-006, US-007, SR-05, SR-07, OQ-03, `docs/02_specification.md` §8 · Agent: writes

Both emails are `text/plain; charset=UTF-8` (the organizer email as `multipart/mixed` with the text part and one attachment). From: `MAIL_FROM`. Sent only after the database row and the JSON copy exist; a failure is logged with the registration id only and does not change the 201 answer.

## Participant confirmation (US-006)

| Part | Content |
|---|---|
| To | the participant's validated email address (only recipient) |
| Subject | `Registration confirmation: <CONFERENCE_NAME>` (no participant input) |
| Body | `Dear <first name> <last name>,` / `thank you for registering for <CONFERENCE_NAME>.` / `Registration type: External participant|Student` / `Selected options:` followed by one line `- <option display name>` per option, or `- none` / `Registration id: <id>` |

## Organizer notification (US-007)

| Part | Content |
|---|---|
| To | one message per address in `ORGANIZER_EMAILS` |
| Subject | `New registration <id>` |
| Body | `A new registration was received for <CONFERENCE_NAME>.` then one line per field: `Id`, `Type`, `First name`, `Last name`, `Email`, `Organization` (external) or `Study institution`, `Study programme`, `Student ID` (student), `Options` (ids, `, `-separated), `Consents` (`<id> (<givenAt>)`), `Registered at` |
| Attachment | `registration-<id>.json`, `application/json`, bytes identical to the JSON copy file (AC-007-02) |

## Safety (SR-05)

- Participant input never appears in a header other than the validated `To` address of the participant email (no whitespace or control characters can pass validation).
- Bodies are plain text; markup or header-like text in the input appears as literal text in the body only.
