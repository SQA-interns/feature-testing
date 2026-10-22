# Release notes

> Written in: phase 7 · Source: `general/quality/definition-of-done.md`, `docs/06_verification-report.md` · Agent: writes

## Version 1.0.0 (2026-10-22)

First release of the conference registration.

### Features

- Registration for external participants (US-001) and students (US-002) with the fixed fields of BR-01, configurable options grouped as workshops, events, meals and other activities, and the mandatory privacy consent (OQ-02).
- Options and consents from a configuration file, changed by restart without code changes (US-003); every option lists the registration types it is available to (OQ-01).
- Confirmation in the page after the backend accepted the registration, field errors next to the fields, a general message on failure (US-004).
- Storage in PostgreSQL plus a raw JSON copy per registration, both written before success is reported (US-005, AR-05).
- Confirmation email to the participant (US-006) and notification email with the JSON copy attached to every organizer address (US-007); an email failure never undoes a registration (OQ-03).
- Excel export for organizers (US-008) and an organizer search page with text search, type and option filters and paging (US-009), both behind HTTP Basic organizer access.
- Security: reCAPTCHA v2 verified on the server, rate and size limits on every `/api` endpoint, organizer access only over HTTPS except from localhost, security headers, non-root containers.

### Quality

| Component | Tests | Line coverage | Mutation score (adjusted) |
|---|---|---|---|
| backend | 288 (unit, architecture, acceptance) | 98.09 % | 93.15 % |
| frontend | 109 (unit, acceptance) | 100 % | 93.83 % |
| both | 6 end-to-end journeys in Chromium | – | – |

No open Critical, High or Medium finding; accepted items are listed in `docs/06_verification-report.md`.

### Manual tests a human must do (DoD-09)

These cannot be automated without the real external services:

1. **reCAPTCHA with production keys:** with `CAPTCHA_TEST_MODE=false` and the real `RECAPTCHA_SITE_KEY`/`RECAPTCHA_SECRET_KEY`, submit one registration with the widget solved (accepted, 201) and one with an expired or missing token (rejected with a `captchaToken` error, nothing stored).
2. **Email delivery:** with the real SMTP settings, register once and confirm that the participant's real mailbox receives the confirmation and every address in `ORGANIZER_EMAILS` receives the notification with the `registration-<id>.json` attachment; check that Slovenian characters display correctly.
3. **TLS and reverse proxy:** behind the production nginx with Let's Encrypt: HTTPS works and HTTP redirects to HTTPS; `/` serves the frontend and `/api` reaches the backend; the proxy sets the upstream `Host` itself (backend README); with `TRUSTED_PROXIES` set to the proxy's address the organizer page works over HTTPS and an organizer request over plain HTTP is refused.
4. **Organizer page in a real browser:** sign in, search, filter and page through the registrations; no browser login dialog appears on a wrong password.

### Operations

- **Retention (OQ-06):** registrations and JSON copies are kept until the operator deletes them; there is no automatic deletion. Delete the database rows (`registration`, `registration_option`, `registration_consent`) and the files in the JSON copy directory **no later than 12 months after the conference**.
- **Backups:** back up the PostgreSQL volume and the JSON copy volume together.
- **Configuration:** production needs `APP_ENVIRONMENT=production`, `CONFERENCE_CONFIG_FILE`, real reCAPTCHA keys, SMTP settings, `TRUSTED_PROXIES` and `ORGANIZER_HTTPS_ONLY=true`; the backend refuses to start otherwise (see the backend README).

### Known limitations and decisions pending review

- D-10: a valid organizer request over plain HTTP from another host is answered 403 (the criteria did not fix a status).
- D-11: the 100-character search limit applies to the text as sent, before surrounding spaces are removed.
- D-08: vitest 4.1.11 has two Moderate advisories (dev tooling only; not in the shipped bundle).
- The organizer page's option filter lists the active options; an inactive option id can still be searched through the API.
- Locally, all browser requests through the frontend container count as one client for the rate limit.
