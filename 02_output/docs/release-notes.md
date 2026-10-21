# Release notes

> Written in: phase 7 · Agent: writes

Version 1.1.0 of the conference registration (backend and frontend), 2026-10-22: version 1.0.0 (base run `tanej_single-agent_sdd_v001_run-1`) plus the organizer registration search (US-009, change run `tjan_single-agent_sdd_v001_run-1-search`).

## New in 1.1.0

- Organizer search API `GET /api/registrations` (US-009, BR-09, BR-10): optional `text` (case-insensitive, any part of first name, last name, email, organization or study institution; at most 100 characters after trimming), `type` (`EXTERNAL` or `STUDENT`), `option` (a configured option id) and `page`; all filters combined; newest first, 20 per page, with the total number of matches. Invalid parameters are answered 400 with an error per parameter; the same organizer access as the export (HTTP Basic, HTTPS or localhost only).
- Organizer search page at `/organizer`: sign-in, search field, type and option filters, result table and page navigation. The login stays in memory only; stored participant data is always shown as text (SR-09).
- The rate limit now counts per client, method and path, so organizer searches and participant registrations (`GET` and `POST /api/registrations`) have separate budgets.
- The Maven wrapper is executable from a clean checkout on Linux and macOS (D-23).

## Delivered (unchanged from 1.0.0)

- Registration forms for external participants (US-001) and students (US-002) with exactly the fixed fields of each type, the options available to the type grouped as workshops, events, meals and other activities, and the mandatory `privacy` consent, never preselected.
- Conference options and consents from a configuration file (`CONFERENCE_CONFIG_PATH`); an invalid file stops the backend at start (US-003).
- Confirmation in the page only after the backend accepted the registration; field errors next to their fields; a general message without technical details on failures (US-004).
- Storage in PostgreSQL and as a raw JSON copy per registration, both before the success answer (US-005, AR-05).
- Participant confirmation email and organizer notifications with the JSON copy attached; a mail failure never undoes a registration (US-006, US-007).
- Excel export for the organizer over HTTP Basic, refused over plain HTTP from non-local clients (US-008, SR-06).
- reCAPTCHA v2 verified on the backend, a deterministic test mode for local and test use, rate and body-size limits, security headers, non-root containers, health and readiness endpoints; local stack with `docker compose`.

Verification (`docs/06_verification-report.md`): 48/48 acceptance criteria tested; 381 automated tests green (backend 281, frontend 94, end-to-end 6); the 116 frozen tests of 1.0.0 unchanged; line coverage backend 99.20 %, frontend 100 %; mutation score backend 99.18 % adjusted (96.99 % raw), frontend 91.19 %; no open Critical or High finding.

## Known limitations

- Medium, accepted: the pinned Vite 6.4.3 has dev-server advisories that `npm audit` rates High (launch-editor, `server.fs`); they are not in the shipped bundle and the human lowered them to Medium (D-22). `@vitest/mocker` ≤ 4.1.11 (test tooling, D-29) and `qs` through Stryker (PA-03) have Moderate advisories. **Approve Vite 6.4.5 and a fixed Vitest in `tech-stack.md` for the next run.** Also accepted: nginx `/api/` location without `internal`; no npm `min-release-age` (needs npm 11.10).
- The search folds upper and lower case of Č, Š, Ž in PostgreSQL with `lower()`; that needs a database with a UTF-8 locale (the pinned image has one). An option filter accepts every configured option id, also inactive ones (G-8); the page offers only the active options.
- Reloading the organizer page signs the organizer out (the login is kept in memory only, by design).
- The rate limit is kept in memory per backend instance; behind several instances it applies per instance.
- Email addresses are accepted with an ASCII domain only (D-09); text fields are limited to 200 characters, email to 254 (D-08).
- The local stack runs with `ORGANIZER_HTTPS_ONLY=false` (D-11); production refuses that setting.
- Registrations and JSON copies are never deleted automatically (OQ-06): **the operator must delete the database rows and the JSON copies no later than 12 months after the conference.**
- The end-to-end tests are not part of the mutation runs (D-15).
- Host tools differed from the pins and were approved: base run Oracle JDK 21.0.11 and Compose 5.6.0 (D-02, D-03); this run Ubuntu OpenJDK 21.0.12.1, Docker Engine 29.3.1 and Compose 5.1.1 (D-19 … D-21). The images use the pinned versions.
- On the US-009 machine, volumes of an earlier stack were left in place; this run's stack used `COMPOSE_PROJECT_NAME=konferenca-us009` (D-24). The old volumes `konferenca-registration_pgdata` and `_jsoncopies` (created 2026-10-06) can be deleted by the operator.

## Must be tested manually by a human

From `project/00_setup/environments.md`, "External services", plus the organizer search page:

1. **Google reCAPTCHA v2 with production keys:** set `CAPTCHA_TEST_MODE=false`, `RECAPTCHA_SITE_KEY` and `RECAPTCHA_SECRET_KEY`; the page shows the Google widget; one real registration succeeds; a registration without solving the widget is refused.
2. **SMTP delivery to a real mailbox:** with the production SMTP settings, the participant confirmation and the organizer notification with the JSON attachment arrive in real mailboxes and are not marked as spam.
3. **TLS and the reverse proxy:** the external nginx with Let's Encrypt serves `/` and `/organizer` from the frontend over HTTPS, redirects HTTP to HTTPS, forwards `/api` to the backend, sets the upstream `Host` itself (SR-08) and sends `X-Forwarded-Proto: https`; the organizer export and search work over HTTPS and are refused (403) over plain HTTP.
4. **Production start guard:** start the backend with `APP_ENVIRONMENT=production`; it starts with the production settings and refuses to start when `CAPTCHA_TEST_MODE=true` (SR-02).
5. **Organizer search page in the organizers' browsers** (US-009): sign in with a wrong password (an error, no browser login dialog, no data), then with the right one; search a known participant by part of the last name in another case, filter by type and by option, page through more than 20 registrations; reload the page and check that it asks for the login again.

## Decisions pending review

| Decision | Applied default |
|---|---|
| D-08 | text fields at most 200 characters, email at most 254 |
| D-09 | email: atext local part, ASCII domain with at least one dot |
| D-10 | rate limit default 60 requests per minute per client and endpoint |
| D-11 | local compose stack with `ORGANIZER_HTTPS_ONLY=false` |
| D-16 | Vite 6.4.3 kept (superseded by D-22: approve 6.4.5 for the next run) |
| D-26 | the 100-character search text limit counts the trimmed text |
| D-27 | strict search parameters: exact `EXTERNAL`/`STUDENT`, configured option ids, page 1 … 999999 in digits; blank values rejected |
| D-29 | `@vitest/mocker` advisories accepted as Medium; vitest 4.1.11 kept |

All other decisions (D-01 … D-29) are resolved; see `docs/decisions-log.md`.
