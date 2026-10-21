# Interfaces without a machine-readable schema

> Written in: phase 2 · Source: `project/02_design/architecture.md` (Interfaces), `api-contract.md` · Agent: writes

| Interface | Contract |
|---|---|
| Options, registration, export, search (REST) | `openapi.yaml` |
| Registration storage (SQL) | `database-schema.sql` (Flyway `V1__registration.sql`) |
| JSON copy (file) | `registration-copy.schema.json` |
| Conference options (configuration) | `conference-config.schema.json` |
| Emails (SMTP) | section "Emails" below |
| Anti-automation (reCAPTCHA) | section "reCAPTCHA" below |
| Registration form (UI) | section "Registration form" below |
| Organizer search page (UI, US-009) | section "Organizer search page" below |

## Emails

Sent by the backend over SMTP (`SMTP_HOST`, `SMTP_PORT`, `SMTP_TLS`, optional `SMTP_USERNAME` / `SMTP_PASSWORD`) only after the database row and the JSON copy exist (OQ-03). A send failure is logged with the registration id only and does not change the HTTP 201 answer.

| Mail | To | From | Subject | Body | Attachment |
|---|---|---|---|---|---|
| Participant confirmation (US-006) | the participant's validated email, one recipient | `MAIL_FROM` | `Registration confirmed: <conference name>` | `text/plain; charset=UTF-8`: conference name, participant first and last name, registration id, selected option names (or "none") | none |
| Organizer notification (US-007) | each address in `ORGANIZER_EMAILS`, one message per address | `MAIL_FROM` | `New registration: <conference name>` | `text/plain; charset=UTF-8`: registration id, type, every registration field (`label: value`), option ids and names, consent ids with time, registration time | `registration-<id>.json`, `application/json`, the bytes of the stored JSON copy |

Safety (SR-05, SR-07): headers contain no participant input except the recipient address, which passed the email rule (D-09, no whitespace or control characters) and is set through the mail API, never by string concatenation; bodies are plain text, so markup is shown literally; the organizer mail and attachment carry only registration fields.

## reCAPTCHA

| Mode | Setting | Behaviour |
|---|---|---|
| Production | `CAPTCHA_TEST_MODE=false` (default), `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | Backend posts `secret` and `response` (form-encoded) to `RECAPTCHA_VERIFY_URL` (default `https://www.google.com/recaptcha/api/siteverify`) with a 5 s timeout; accepts only `{"success": true}`. Any other answer, error or timeout rejects the token. |
| Test | `CAPTCHA_TEST_MODE=true` | No network call; `test-pass` is accepted, every other token rejected (api-contract). |

Start-up guard (SR-02): with `APP_ENVIRONMENT=production`, the backend refuses to start when test mode is on or either key is empty. Frontend: `captchaSiteKey` (and `captchaTestMode`) come from `GET /api/form-config`. With a site key the frontend loads the Google v2 widget and sends its token; in test mode it shows a "test mode" checkbox that yields the token `test-pass`.

## Registration form

Single page at `/` served by the frontend; it calls only `/api/*` (AR-01).

1. On load: `GET /api/form-config`. On failure: a general error, no form.
2. Choice of registration type: radio buttons "External participant" and "Student"; no type preselected, no fields before a choice.
3. Fields: external: first name, last name, email, organization / institution. Student: first name, last name, email, study institution, study programme, student ID. Each field has a label and an error slot right after it.
4. Options: active options available to the chosen type, as checkboxes grouped under the headings Workshops, Events, Meals, Other activities (a group without options is not shown). None preselected.
5. Consents: one checkbox per configured consent with its text, never preselected (BR-05).
6. reCAPTCHA widget, then the submit button.
7. Before sending: required fields (after trimming), email format (D-09), length (D-08), mandatory consents and the captcha token are checked; any error is shown next to its field and no request is sent (AC-004-03).
8. 201: the form is replaced by a confirmation with the participant's name and the selected option names (AC-004-01).
9. 400: each `errors[].field` message is shown next to its field (unknown fields as a general message); entered values stay (AC-004-02).
10. Any other status or a network error: a general message "Registration could not be completed. Please try again later." with no technical detail; values stay (AC-004-04).

Accessible names (used by the tests): field labels "First name", "Last name", "Email", "Organization / institution", "Study institution", "Study programme", "Student ID"; group headings "Workshops", "Events", "Meals", "Other activities"; button "Register"; error elements have `role="alert"` and are linked with `aria-describedby`.

## Organizer search page

Page at `/organizer` of the frontend (nginx serves `index.html` for every path; the app chooses the page by `location.pathname`). The only organizer page (`scope.md`); it calls only `/api/form-config` and `GET /api/registrations` (AR-01).

1. Sign-in form: heading "Organizer sign-in", fields "Username" and "Password" (type password), button "Sign in". No registration data is shown before a successful sign-in.
2. Sign in = `GET /api/registrations` (no filters, page 1) with `Authorization: Basic …` and `X-Requested-With: XMLHttpRequest`. 401: the alert "Wrong username or password." and no data; 403: the alert "Organizer access requires HTTPS."; any other error: "Registrations could not be loaded. Please try again later." Credentials stay in component memory only (never in storage, cookies or the URL) and are dropped on "Sign out".
3. After sign-in: heading "Registrations"; a search form with the text field "Search" (max 100 characters), the select "Type" (choices "All types", "External", "Student"), the select "Option" (choice "All options" plus every option of `GET /api/form-config` by name, value = option id) and the button "Search". A search (button or Enter) sends the current text, type and option with page 1; empty choices are not sent.
4. Result: the text "<total> registrations found" (or "1 registration found"), then a table with the columns "Name" (first and last name), "Email", "Type" ("External" / "Student"), "Organization / study institution", "Options" (option names, the id when the option is not in the form configuration), "Registered" (the ISO time as received); one row per item, in the order received. No match: "No registrations found." and no rows.
5. Page navigation: "Page <page> of <pages>" (pages = ceil(total / 20), at least 1), buttons "Previous" (disabled on page 1) and "Next" (disabled on the last page); they repeat the last search with the other page.
6. A 400 answer shows each `errors[].message` as an alert; a 401 later (for example a changed password) returns to the sign-in form with the "Wrong username or password." alert.
7. Every value from the API is rendered as React text (SR-09); markup in stored data appears literally and is never executed.

Accessible names (used by the tests): headings "Organizer sign-in", "Registrations"; fields "Username", "Password", "Search"; selects "Type", "Option"; buttons "Sign in", "Search", "Previous", "Next", "Sign out"; the result table is a `table` with column headers as listed; alerts have `role="alert"`.
