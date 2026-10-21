# Release notes

> Written in: phase 7 · Agent: writes

Version 1.0.0 of the conference registration (backend and frontend), 2026-10-21.

## Delivered

- Registration forms for external participants (US-001) and students (US-002) with exactly the fixed fields of each type, the options available to the type grouped as workshops, events, meals and other activities, and the mandatory `privacy` consent, never preselected.
- Conference options and consents from a configuration file (`CONFERENCE_CONFIG_PATH`, schema in `docs/02_contracts/conference-config.schema.json`); an invalid file stops the backend at start (US-003).
- Confirmation in the page only after the backend accepted the registration; field errors next to their fields; a general message without technical details on failures (US-004).
- Storage in PostgreSQL and as a raw JSON copy per registration, both before the success answer (US-005, AR-05).
- Participant confirmation email and one organizer notification per configured address with the JSON copy attached; a mail failure never undoes a registration (US-006, US-007).
- Excel export for the organizer over HTTP Basic, refused over plain HTTP from non-local clients (US-008, SR-06).
- reCAPTCHA v2 verified on the backend, a deterministic test mode for local and test use, rate and body-size limits, security headers, non-root containers, health and readiness endpoints.
- Local stack with `docker compose` (backend, frontend, PostgreSQL, Mailpit); data on named volumes survives container recreation.

Verification: 35/35 acceptance criteria tested; 311 automated tests green (backend 251, frontend 55, end-to-end 5); line coverage backend 99.11 %, frontend 100 %; mutation score backend 99.10 % adjusted (96.70 % raw), frontend 90.85 %; no open Critical or High finding (`docs/06_verification-report.md`).

## Known limitations

- Medium, accepted (`docs/06_verification-report.md`): the pinned Vite 6.4.3 has advisory GHSA-698v-4r3x-3vcv in its dev server (not in the shipped bundle; fix 6.4.4 needs approval, D-16); `qs` advisories only through Stryker (PA-03); nginx `/api/` location without `internal`; no npm `min-release-age` (needs npm 11.10).
- The rate limit is kept in memory per backend instance and counts per client address and endpoint; behind several backend instances the limit applies per instance.
- Email addresses are accepted with an ASCII domain only (D-09); text fields are limited to 200 characters, email to 254 (D-08).
- The local stack runs with `ORGANIZER_HTTPS_ONLY=false` (D-11); production refuses that setting.
- Registrations and JSON copies are never deleted automatically (OQ-06): **the operator must delete the database rows and the JSON copies no later than 12 months after the conference.**
- The end-to-end tests are not part of the mutation runs (D-15).
- Built and tested on the host with Oracle JDK 21.0.11 and Docker Compose 5.6.0 instead of the pinned Temurin 21.0.10+7 and Compose 5.5.1 (approved, D-02, D-03); the images use the pinned Temurin.

## Must be tested manually by a human

From `project/00_setup/environments.md`, "External services":

1. **Google reCAPTCHA v2 with production keys:** set `CAPTCHA_TEST_MODE=false`, `RECAPTCHA_SITE_KEY` and `RECAPTCHA_SECRET_KEY`; the page shows the Google widget; one real registration succeeds; a registration without solving the widget is refused.
2. **SMTP delivery to a real mailbox:** with the production SMTP settings (`SMTP_HOST`, `SMTP_PORT`, `SMTP_TLS=true`, `SMTP_USERNAME`, `SMTP_PASSWORD`), the participant confirmation and the organizer notification with the JSON attachment arrive in real mailboxes and are not marked as spam.
3. **TLS and the reverse proxy:** the external nginx with Let's Encrypt serves `/` from the frontend over HTTPS, redirects HTTP to HTTPS, forwards `/api` to the backend, sets the upstream `Host` itself (SR-08) and sends `X-Forwarded-Proto: https`; the organizer export works over HTTPS and is refused (403) over plain HTTP.
4. **Production start guard:** start the backend with `APP_ENVIRONMENT=production` and the production settings; it starts, and it refuses to start when `CAPTCHA_TEST_MODE=true` (SR-02).

## Decisions pending review

| Decision | Applied default |
|---|---|
| D-08 | text fields at most 200 characters, email at most 254 |
| D-09 | email: atext local part, ASCII domain with at least one dot |
| D-10 | rate limit default 60 requests per minute per client and endpoint |
| D-11 | local compose stack with `ORGANIZER_HTTPS_ONLY=false` |
| D-16 | Vite 6.4.3 kept; approve 6.4.4 in `tech-stack.md` for the next run |

All other decisions (D-01 … D-17) are resolved; see `docs/decisions-log.md`.
