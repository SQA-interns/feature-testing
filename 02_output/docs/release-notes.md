# Release notes

> Written in: phase 7 · Agent: writes

Version 0.1.0 of the conference registration, from run `tjan/single-agent/sdd/v002/run-2` (2026-10-27).

## Delivered

- **Registration forms (US-001, US-002):** external participants and students, each with its fixed fields, configurable options and consents. Validation runs in the browser and is enforced on the backend (BR-01..BR-05). Text accepts Unicode, including č, š and ž.
- **Configurable options (US-003):** workshops, events, meals and other activities, with an `active` flag and the registration types each is offered to, in `config/conference.json`. An invalid file stops the startup.
- **Confirmation (US-004):** shown only after the backend accepted the registration. Field errors appear next to their fields; a general message replaces technical details.
- **Storage (US-005):** each accepted registration is stored in PostgreSQL and as a raw JSON copy `<id>.json` on a persistent volume, both or neither (BR-07).
- **Emails (US-006, US-007):** a plain-text confirmation goes to the participant, and a notification with the JSON copy attached goes to each organizer address. Emails are sent after storage; a mail failure does not undo the registration (OQ-03).
- **Export (US-008):** an Excel workbook of every registration for the organizer, over HTTP Basic.
- **Protection:** server-side reCAPTCHA v2 verification, per-client rate limits, body size limit, security headers, non-root containers, startup refusal of unsafe production settings (SR-01..SR-08, SB-01..SB-14).
- **Operations:** health and readiness endpoints used by the container health checks, and Flyway migrations for the schema.

Quality at release: 380 automated tests pass (backend 301, frontend 74, end-to-end 5). Line coverage is 99.4 % on the backend and 100 % on the frontend. Mutation scores are 92.7 % (PIT) and 90.3 % (Stryker). No Critical or High finding is open (`06_verification-report.md`).

## Known limitations

- **Organizer login is HTTP Basic** (D-18, F-01). Expose the export only over HTTPS (`ORGANIZER_HTTPS_ONLY=true`, the default) and use a random password of at least 16 characters. There is one organizer account, from `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD`.
- **Rate-limit counters live in the memory of one backend instance.** They reset on restart and are not shared if more than one instance runs.
- **No editing or cancelling of a registration,** no participant accounts and no administration UI (out of scope). Options change only by editing the configuration file and restarting the backend.
- **Duplicate registrations with the same email are accepted** and both are exported (OQ-05).
- **Retention (OQ-06):** nothing is deleted automatically. The operator must delete the database rows and the JSON copies no later than 12 months after the conference.
- **Dev-only dependency advisories:** `npm audit` reports 5 Moderate advisories in test tooling (`vitest` 4.1.11, `@vitest/mocker`, `@vitest/coverage-v8`; `qs` and `typed-rest-client` through Stryker), accepted in D-05 and D-06. The shipped frontend has none (`npm audit --omit=dev`).
- **End-to-end tests run against the containers,** so they are outside the coverage and mutation figures (D-17).

## Must be tested manually by a human

1. **Real reCAPTCHA:** with production keys and `RECAPTCHA_TEST_MODE=false`, submit one registration through the real widget. It is accepted, and a submission with an expired or missing challenge is rejected.
2. **Real email delivery:** with the production SMTP server, check that the participant confirmation and the organizer notification (with the JSON attachment) arrive in real mailboxes, display č, š and ž correctly, and are not marked as spam.
3. **HTTPS and reverse proxy:** behind the production nginx with Let's Encrypt, check that HTTPS works, that HTTP redirects to it, that `/api` reaches the backend with the proxy's own `Host` header, and that `X-Forwarded-Proto https` is set. The export must work over HTTPS and answer 403 over plain HTTP from another host.
4. **Production startup:** start the backend once with `APP_ENVIRONMENT=production` and the real settings. It starts, and the readiness endpoint reports `UP`.
5. **Export in Excel:** open a downloaded workbook in Microsoft Excel or LibreOffice. Every column is shown, Slovenian characters are intact, and a value such as `=1+1` stays text.
6. **Browsers:** fill the form in the browsers the conference expects, including a phone. Errors show next to the fields and the confirmation appears.
7. **Data survival in production:** recreate the production containers once. Registrations and JSON copies remain.

## Decisions pending review

| Decision | Choice made by the agent |
|---|---|
| D-11 | Organizer credentials over plain HTTP from another host get 403 with no redirect (AC-008-04, SR-06) |
| D-12 | Shipped `config/conference.json`: the example options and the one mandatory `privacy` consent |
| D-13 | User-facing language is English |
| D-14 | Maximum lengths: names and institutions 200, email 254, student ID 50 |

Every other decision (D-01..D-10, D-15..D-18) is resolved; see `decisions-log.md`.
