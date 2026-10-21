# Release notes

> Written in: phase 7 · Agent: writes

Version 1.0.0, 2026-10-21. Components: `backend` (Spring Boot 4.1.1, Java 21), `frontend` (React 19.3, Vite 6.4.4, nginx 1.30.5); local stack `02_output/docker-compose.yml`.

## Delivered

- **Registration forms** for external participants and students (US-001, US-002): exactly the fields of each type, options grouped as workshops, events, meals and other activities, only active options available to the type, consents never preselected, validation in the browser and on the server, field errors next to the fields (NFR-03).
- **Configurable options and consents** (US-003): a JSON file (`CONFERENCE_CONFIG`, schema `docs/02_contracts/conference-config.schema.json`); a new file and a restart change the programme; an invalid file stops the start.
- **Confirmation** in the page after the backend accepted the registration (US-004), a general message without details on failures.
- **Storage** (US-005): PostgreSQL row (Flyway schema) and a raw JSON copy per registration on a persistent volume, in one transaction; a failed copy leaves nothing behind.
- **Emails** (US-006, US-007): plain-text confirmation to the participant; notification to every organizer address with the JSON copy attached; sent after storage, a mail failure never loses a registration.
- **Excel export** for organizers (US-008), HTTP Basic, HTTPS-only for clients that are not on the machine itself.
- **Protection**: reCAPTCHA v2 verified on the server, test mode only in local/test profiles, rate limit per client and endpoint (HTTP 429), body size limit (HTTP 413), security headers, non-root containers, health and readiness endpoints.
- **Quality**: 321 automated tests (backend 230, frontend 86, end-to-end 5), line coverage 99.5 % (backend) and 100 % (frontend), mutation score 95.8 % (backend, PIT) and 94.9 % raw / 98.5 % adjusted (frontend, Stryker); `docs/03_test-strategy.md`, `docs/06_verification-report.md`.

## Known limitations

- **Retention (OQ-06)**: there is no automatic deletion. The operator must delete the database rows (`registration`, cascading to options and consents) and the JSON copies in `JSON_COPY_DIR` **no later than 12 months after the conference**.
- The rate limit is kept in the memory of one backend instance; several instances behind a load balancer each count separately.
- Behind the production reverse proxy, the backend trusts `X-Forwarded-For` / `X-Forwarded-Proto` from private-network proxies only (`server.forward-headers-strategy=native`); the proxy must set them.
- The production reverse proxy must set the upstream `Host` itself and never forward the client's (SR-08); the bundled nginx of the frontend image does so for the local stack.
- Option ids, names and consents are read once at start; changing them needs a restart.
- Accepted findings (`docs/06_verification-report.md`): F-04 (npm 11.6.2 cannot enforce a minimum release age), F-05 (Moderate advisories in the dev-only Stryker dependency `qs`), F-03, F-06, F-07, F-08 (Low).
- Dependency and tool versions that differ from `tech-stack.md`: vite 6.4.4 instead of 6.4.3 (D-09), Docker Compose 5.6.0 on the build host (D-09), `@types/node` 24.13.6 added for the e2e helpers (D-14).

## Must be tested manually by a human

| What | How | Why it is not automated |
|---|---|---|
| Google reCAPTCHA v2 with production keys | set `CAPTCHA_TEST_MODE=false`, `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` for the production domain; submit one registration through the widget; check that a missing or expired check is refused | tests use the test mode and a mocked verification endpoint; no network calls to Google in tests |
| Delivery through the real SMTP server | set `MAIL_HOST`, `MAIL_PORT`, `MAIL_TLS=true`, `SMTP_USERNAME`, `SMTP_PASSWORD`; register once; check that the participant and every organizer address receive their message (attachment opens, Slovenian characters readable) | tests use Mailpit |
| HTTPS, redirects and `/api` routing through the external nginx with Let's Encrypt | open the public URL over HTTP (redirect to HTTPS), register, export over HTTPS (200) and try the export over plain HTTP from outside (refused); check the proxy sets `Host` and `X-Forwarded-Proto` | no TLS proxy in the local stack |
| Production start-up refusals | start without a profile and with `CAPTCHA_TEST_MODE=true`, then with an empty reCAPTCHA key, then with `ORGANIZER_HTTPS_ONLY=false`: each must refuse to start | covered by unit tests of `StartupGuard`, not on the production host |
| Excel workbook in the organizers' spreadsheet program | open an export with Slovenian names in Excel or LibreOffice | tests read the workbook with POI and a zip reader |

## Decisions pending review

| Decision | Applied behaviour |
|---|---|
| D-11 | a consent id that is not configured is rejected (HTTP 400, field `consents`) |
| D-12 | text fields at most 200 characters, email at most 254; longer values are rejected with a field error |
| D-13 | a missing or unknown registration `type` is rejected (HTTP 400, field `type`) |

Every other decision (D-01..D-16) is resolved; `docs/decisions-log.md`.
