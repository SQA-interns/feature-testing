# Release notes

> Written in: phase 7 · Agent: writes

Version 0.1.0, 2026-10-22.

## Delivered

| Story | Delivered |
|---|---|
| US-001, US-002 | registration forms for external participants and students with backend validation (required fields, email format, active and available options, mandatory consents, reCAPTCHA) |
| US-003 | options and consents from a configuration file (`CONFERENCE_CONFIG`), validated at start-up |
| US-004 | confirmation in the page; field errors next to the fields; general error without details |
| US-005 | storage in PostgreSQL and as a raw JSON copy in one unit of work (no 201 unless both exist) |
| US-006, US-007 | participant confirmation and one organizer notification per address with the JSON copy attached, sent after storage; a mail failure keeps the registration |
| US-008 | Excel export for the organizer (HTTP Basic) |
| US-009 | organizer search page and API: text search, type and option filters, 20 per page, newest first |
| security and operations | rate and size limits, HTTPS-only organizer access, security headers, non-root containers, health and readiness, local compose stack |

All 48 acceptance criteria and 6 end-to-end scenarios pass; quality gate met (backend line 98.32 %, mutation 91.20 %; frontend line 100 %, mutation 93.81 %). Evidence: `docs/06_verification-report.md`.

## Known limitations

- The rate limit is kept in memory per backend instance; several instances behind one proxy each allow the limit.
- There is one organizer account; credentials are entered on the organizer page and kept only in the page's memory (a reload signs out).
- The organizer search page is the only organizer page; there is no administration UI, editing, deletion or filtered export (out of scope).
- The backend image copies a jar built on the host (`./mvnw package` first); the stack is not built from source inside Docker for the backend.
- A configuration change (programme, consents) needs a backend restart.
- Accepted findings: npm `min-release-age` is not set because the pinned npm 11.6.2 does not support it (F-01); Moderate advisories remain in dev-only test tooling (F-07, F-08). See `docs/06_verification-report.md`.
- Registrations and JSON copies are never deleted automatically (OQ-06): **the operator must delete the database rows and the JSON copies no later than 12 months after the conference** (`DELETE FROM registration;` cascades to options and consents; remove the files in the `json-copies` volume).

## Must be tested manually by a human

| # | What | How | Why not automated |
|---|---|---|---|
| M-01 | reCAPTCHA with production keys | set `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY`, `CAPTCHA_TEST_MODE=false`; submit one real registration; then submit with the widget unsolved (expect the captcha error) | tests use test mode and a mocked verification endpoint; no calls to Google |
| M-02 | email delivery to real mailboxes | configure the external SMTP account (`SMTP_HOST`, `SMTP_PORT`, `SMTP_TLS=true`, `SMTP_USERNAME`, `SMTP_PASSWORD`); register; check the participant mailbox and every `ORGANIZER_EMAILS` mailbox, incl. the JSON attachment and č, š, ž | tests use Mailpit |
| M-03 | HTTPS, redirects and `/api` routing on the external nginx | `https://` serves the page, `http://` redirects to HTTPS, `/api/form-config` reaches the backend, the certificate (Let's Encrypt) is valid | no TLS or reverse proxy locally |
| M-04 | proxy headers (SR-08) and HTTPS-only access (SR-06) in production | send a request with a forged `Host` header and check the backend sees the upstream host; sign in on `/organizer` over HTTPS (works) and request `/api/export` over plain HTTP with valid credentials (403, or redirected by the proxy) | needs the production proxy |
| M-05 | production start-up guards | start the backend with `APP_ENVIRONMENT=production` and in turn `CAPTCHA_TEST_MODE=true`, empty reCAPTCHA keys, `ORGANIZER_HTTPS_ONLY=false`: it must refuse to start each time | unit-tested only |
| M-06 | data retention | after the conference, delete rows and JSON copies within 12 months (see Known limitations) | an operator procedure |
| M-07 | export opened in Excel | open the downloaded `registrations.xlsx` in Microsoft Excel or LibreOffice; check Slovenian characters and that no cell is evaluated as a formula | tests parse the file programmatically |

## Decisions pending review

| Decision | Applied option | Open question for the reviewer |
|---|---|---|
| D-11 | organizer requests over plain HTTP from a non-local client are answered 403 (field `protocol`) before credentials are checked | is 403 acceptable, or should it be 401 like wrong credentials? The frozen tests accept any 4xx without data |

All other decisions (D-01 … D-16) are resolved; see `docs/decisions-log.md`.

## Readme check from a clean checkout

2026-10-22, `git clone` of commit `cf35702` into an empty folder, then every command of the READMEs as written (`out/logs/07_clean-checkout.log`):

| Step | Result |
|---|---|
| `git clone` | needs `-c core.longpaths=true` on Windows below a deep folder (now stated in the root README); clean working tree |
| backend `./mvnw -B package -DskipTests` | jar built |
| backend check command | clean |
| backend `./mvnw -B verify` | 227 tests, 0 failures |
| frontend `npm ci`, `npm run check`, `npm test`, `npm run build` | all succeed, 62 tests pass |
| root quick start: `docker compose --env-file ../.env up -d --build` | all four services healthy, page 200, readiness UP |
| frontend `npm run test:e2e` | 6 passed |
