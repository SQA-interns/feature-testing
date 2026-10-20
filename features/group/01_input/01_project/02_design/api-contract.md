# API contract

> Owner: Architect · Read in: phases 2, 3, 4, 6 · Agent: read-only

The REST API between the frontend or organizer and the backend is fixed by this file (AR-09): paths, methods, field names, status codes and the local test settings below are the same in every run of this project, so that external checks can use them. Everything not stated here (internal design, additional optional response fields, the OpenAPI document written in phase 2) is the agent's choice. The phase 2 contract in `docs/02_contracts/` must describe exactly this API.

## Local stack

| Item | Value |
|---|---|
| Backend base URL | `http://127.0.0.1:8080` (published by `02_output/docker-compose.yml`) |
| Mailpit web and API | `http://127.0.0.1:8025` |
| reCAPTCHA test mode | setting `CAPTCHA_TEST_MODE=true`; then the token `test-pass` is accepted and every other token is rejected |
| Rate limit | setting `RATE_LIMIT_PER_MINUTE`: requests per minute per client and endpoint; the agent chooses the default |
| Organizer HTTPS-only access | setting `ORGANIZER_HTTPS_ONLY`; `false` is allowed only on local plain HTTP |

`docker-compose.yml` passes these three settings from the environment to the backend, so they can be changed with `docker compose up` without editing a file. Organizer credentials are `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD` from `.env`, sent with HTTP Basic authentication.

## Common rules

- Request and response bodies are JSON (`application/json; charset=utf-8`), except the export.
- Registration types are `EXTERNAL` and `STUDENT`; option categories are `WORKSHOP`, `EVENT`, `MEAL` and `OTHER`.
- Times are ISO 8601 with offset or `Z`.
- A rejected request (HTTP 400) has the body `{"errors": [{"field": "<name>", "message": "<text>"}]}`, one entry per invalid field. `field` is the request field's name as written in this file (for example `email`, `options`, `consents`, `captchaToken`); for a query parameter it is the parameter name.
- A request without valid organizer credentials on an organizer endpoint is answered with HTTP 401 and no registration data.
- A request over the rate limit (`RATE_LIMIT_PER_MINUTE`) is answered with HTTP 429.

## Endpoints

### `GET /api/form-config` (public)

Response 200:

```json
{
  "options": [
    {"id": "ws-ai", "name": "AI workshop", "category": "WORKSHOP", "availableTo": ["EXTERNAL", "STUDENT"]}
  ],
  "consents": [
    {"id": "privacy", "text": "I agree that …", "mandatory": true}
  ],
  "captchaSiteKey": "…"
}
```

Lists only active options (AC-003-01).

### `POST /api/registrations` (public)

Request:

```json
{
  "type": "EXTERNAL",
  "firstName": "Ana",
  "lastName": "Novak",
  "email": "ana@example.org",
  "organization": "Example d.o.o.",
  "studyInstitution": null,
  "studyProgramme": null,
  "studentId": null,
  "options": ["ws-ai"],
  "consents": ["privacy"],
  "captchaToken": "test-pass"
}
```

- `organization` is required for `EXTERNAL`; `studyInstitution`, `studyProgramme` and `studentId` are required for `STUDENT`. Fields of the other type are ignored.
- `options` lists the selected option ids; `consents` lists the ids of the consents given.

Responses: 201 `{"id": "<registration id>"}`; 400 with field errors; any other error status when the registration could not be stored (AC-005-03).

### `GET /api/export` (organizer)

Response 200: an Excel workbook (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`). The first sheet's first row holds these column headers, in this order, followed by one row per registration:

`id`, `type`, `firstName`, `lastName`, `email`, `organization`, `studyInstitution`, `studyProgramme`, `studentId`, `options`, `consents`, `registeredAt`, `groupId`, `contactName`, `contactEmail`

`options` and `consents` are the ids separated by `, `; a cell that does not apply to the registration's type is empty. Response 401 without valid organizer credentials.

### `POST /api/group-registrations` (public)

Request:

    {
      "contact": {"firstName": "Maja", "lastName": "Kranjc", "email": "maja@example.org"},
      "organization": "Example d.o.o.",
      "members": [
        {"firstName": "Ana", "lastName": "Novak", "email": "ana@example.org", "options": ["ws-ai"]},
        {"firstName": "Luka", "lastName": "Zupan", "email": "luka@example.org", "options": []}
      ],
      "consents": ["privacy"],
      "captchaToken": "test-pass"
    }

Responses: 201 `{"groupId": "<group id>", "registrationIds": ["<id of member 1>", "<id of member 2>"]}`, the ids in the order of `members`; 400 with field errors, where a member's field is named `members[<index from 0>].<field>` (for example `members[1].email`), the member list as a whole `members`, the contact person's fields `contact.<field>`; any other error status when the group could not be stored (AC-009-09).

In the export, `contactName` is the contact person's first and last name separated by a space; `groupId`, `contactName` and `contactEmail` are empty for individual registrations.
