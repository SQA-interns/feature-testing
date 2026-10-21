# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location | Runner | Through | Frozen |
|---|---|---|---|---|---|
| Acceptance | backend | `backend/src/test/java/si/konferenca/registration/acceptance/` (12 test classes, 102 tests) | JUnit 6 via `./mvnw test` | HTTP to the app on a random port (`@SpringBootTest`); PostgreSQL 16.15 and Mailpit 1.31.1 in Testcontainers; a JDK `HttpServer` as the mocked reCAPTCHA verify endpoint | yes |
| Acceptance | frontend | `frontend/src/acceptance/` (2 test files, 9 tests) | Vitest 4 + Testing Library, jsdom | the whole `<App />`; `fetch` replaced at the `/api` boundary | yes |
| End-to-end | all | `frontend/e2e/*.e2e.spec.ts` (5 tests, one per E2E-01 … E2E-05) | Playwright 1.63, Chromium | the browser and REST API against `docker compose` (`02_output/docker-compose.yml`); PostgreSQL via `psql` in the container, JSON copies via the backend container, mail via the Mailpit API, the workbook via a zip/XML reader | yes |
| Unit | backend | `backend/src/test/java/si/konferenca/registration/{domain,config,application,adapter}/` (144 tests) | JUnit 6, AssertJ, Spring mocks | classes directly; JDK `HttpServer` for the verify endpoint | no |
| Architecture | backend | `backend/src/test/java/si/konferenca/registration/ArchitectureTest.java` (5 rules, A1 … A5 of spec §2) | ArchUnit 1.3.2 | compiled classes | no |
| Unit | frontend | `frontend/src/**/*.test.{ts,tsx}` outside `acceptance/` (46 tests) | Vitest + Testing Library | modules and components | no |

AC coverage: every one of the 35 criteria is named by at least one acceptance test (backend for the API criteria, frontend for AC-001-08, AC-002-07, AC-004-01 … 04). DoD-P05 is covered by `CaptchaVerificationAcceptanceTest`.

### Harness assumptions (from the contracts; the implementation follows them)

| # | Assumption | Source |
|---|---|---|
| H-1 | The backend reads its settings by the names of spec section 4 (`DB_URL`, `SMTP_PORT`, `CONFERENCE_CONFIG_PATH`, …) as Spring placeholders, so tests pass them as properties. | spec §4 |
| H-2 | `CONFERENCE_CONFIG_PATH` accepts a plain absolute file path (and `classpath:` for the default). | spec §3.1 |
| H-3 | The JSON copy directory is not required at start; a write into it fails when the path is a regular file. | spec §3.2 step 5 |
| H-4 | The backend trusts `X-Forwarded-For` / `X-Forwarded-Proto` from loopback and private proxies (`server.forward-headers-strategy=native`). | spec §3.4 |
| H-5 | Option groups are `fieldset`/`legend` groups named "Workshops", "Events", "Meals", "Other activities"; option and consent checkboxes are labelled by their name or text. | interfaces "Registration form" 4, accessible names |
| H-6 | In test mode the captcha is a checkbox whose name contains "test mode" and yields the token `test-pass`. | interfaces "reCAPTCHA" |
| H-7 | A field error is linked to its input with `aria-describedby` and has `role="alert"`; an input without an error has no description. | interfaces "Registration form" |
| H-8 | Mailpit lists envelope-only recipients as `Bcc`; the injection test therefore checks the recipient set, not the absence of a `Bcc` header. | checked against Mailpit 1.31.1 in phase 3 |
| H-9 | The e2e run gets `ORGANIZER_USERNAME` / `ORGANIZER_PASSWORD` from the environment (exported from `.env` without printing). | working-rules "Secrets" |

### Self-check before the freeze

- Mailpit API field names, attachment download and delete-all were verified against a real `axllent/mailpit:v1.31.1` message (python `smtplib`).
- The e2e workbook reader was run on a workbook written by POI 5.5.1 (shared strings, empty cells, `&`, `<`, č š ž): rows came back exactly.
- Timestamps are read with `getObject(…, OffsetDateTime.class)`; JSON-copy vs database time is compared with 1 ms tolerance (PostgreSQL rounds to microseconds).
- The AC-003-03 cases first start the same settings with a valid file, so a refusal cannot come from another cause; on the skeleton they fail on that control start.
- Byte comparisons use `isEqualTo(byte[])` / `Buffer.equals` (whole content), never element-wise `doesNotContain`.

## US-009 change (this run, D-18)

The base frozen tests above stay unchanged and are still checked against `docs/03_acceptance-manifest.sha256`. The US-009 tests are frozen separately in `docs/03_acceptance-manifest-us-009.sha256`.

| Level | Component | Location | Through | Criteria |
|---|---|---|---|---|
| Acceptance | backend | `backend/src/test/java/si/konferenca/registration/acceptance/SearchAcceptanceTest.java` (14 methods, 30 tests with parameters) | HTTP to the app (`DefaultContextAcceptanceTest`, the frozen base harness, used unchanged); registrations created through `POST /api/registrations`; `registered_at` read from PostgreSQL for AC-009-01 | AC-009-01 … AC-009-11 |
| Acceptance | frontend | `frontend/src/acceptance/organizer-search.acceptance.test.tsx` (3 tests), harness `organizer-harness.tsx` (new; imports `formConfig` from the frozen `harness.tsx`) | `<App />` rendered at `/organizer`, `fetch` replaced at the `/api` boundary; the mock answers 401 unless the `Authorization` header carries the test organizer | AC-009-12, AC-009-13 |
| End-to-end | all | `frontend/e2e/organizer-search.e2e.spec.ts` (1 test, E2E-06) | the browser at `/organizer` and the REST API (setup registrations) against the compose stack; credentials from the environment (H-9) | AC-009-01, 02, 03, 12, DoD-P06 |

AC coverage: each of AC-009-01 … AC-009-13 is named by at least one acceptance test; E2E-06 is the only new e2e test.

### Harness assumptions (US-009)

| # | Assumption | Source |
|---|---|---|
| H-10 | The organizer page is shown at the path `/organizer`; field labels "Username", "Password", button "Sign in"; after sign-in a `table`, the textbox "Search", the selects (combobox) "Type" and "Option", the buttons "Search", "Previous", "Next"; errors are `role="alert"`. | interfaces "Organizer search page" |
| H-11 | The "Type" choices are labelled "External" and "Student", the "Option" choices by option name; the tests select by label, not by value. | interfaces "Organizer search page" 3 |
| H-12 | The result table's third column is "Type" with the text "External" or "Student" (E2E-06). | interfaces "Organizer search page" 4 |
| H-13 | Query values are URL-encoded with `%20` for spaces; Slovenian characters as UTF-8 percent escapes. | api-contract (query parameters) |
| H-14 | `registeredAt` of a search item is the stored `registered_at` (compared with 1 ms tolerance). | spec 3.4a step 6 |

### Self-check before the US-009 freeze

- Every text case of AC-009-02 was checked against all three fixture registrations by hand: each search text occurs in exactly the expected registration and only in a searched field (`computer` and `63210001` occur only in the study programme and student ID, which are not searched).
- AC-009-05 uses four registrations, each missing exactly one of the three conditions, so a missing AND cannot pass.
- AC-009-07 compares the exact id order of both pages; 26 registrations are created one after another over HTTP, so their times differ.
- AC-009-08 checks one error per invalid parameter, a 100-character text as the accepted boundary, and that the rejection carries no registration data. Only behaviour stated in AC-009-08 is frozen; the D-26/D-27 choices (blank or lower-case values, non-numeric page) are left to non-frozen tests.
- AC-009-13 checks the literal text, the absence of `img`, `script` and `b` elements in the table and that the injected handlers did not run.
- E2E-06 runs on a shared database: it uses unique last names, checks that its two new registrations are on page 1 (newest first), and for the type filter checks that every listed row's "Type" cell is "External" instead of assuming one row.

### First run of the US-009 tests (phase 3, before any production change)

| Suite | Passed | Failed | Reason (one line per group) |
|---|---|---|---|
| backend `SearchAcceptanceTest` (`out/logs/phase3-us009-backend-first-run.log`) | 3 | 27 | the endpoint does not exist: `GET /api/registrations` falls to the deny-all rule, which answers an authenticated organizer with 403; the 3 passing tests are AC-009-09 (no or wrong credentials → 401 without data), which the base security configuration already provides for every non-public path |
| frontend organizer acceptance (`phase3-us009-frontend-first-run.log`) | 0 | 3 | no organizer page: `/organizer` shows the registration form, so the "Username" field is not found |
| E2E-06 (`phase3-us009-e2e-first-run.log`) | 0 | 1 | the setup registrations succeed; the organizer page has no "Username" field |

No failure is a build, configuration or harness error; the base suites still pass (frontend acceptance 9/9 in the same run).

## Measurement exclusions

Fixed in phase 0 in the tool configuration (`backend/pom.xml` JaCoCo and PIT, `frontend/vite.config.ts` coverage), before any test was written; they equal the defaults of `quality-requirements.md` "Mutation scope".

| Path | Reason |
|---|---|
| `backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | `@SpringBootApplication` entry point, no logic |
| `frontend/src/main.tsx` | bootstrap file that only mounts the app |
| `frontend/src/**/*.d.ts` | type declarations |
| `frontend/src/**/*.test.{ts,tsx}`, `frontend/src/acceptance/**`, `frontend/src/test/**`, `frontend/e2e/**` | test code |

## First complete run (before any fix)

Phase 3 run on the bootstrap skeleton (commit before the freeze), logs in `out/logs/`. The freeze commit `083eb5f` says "25 frozen files" in its body; the manifest lists 27 (the count in the message was wrong, the manifest is authoritative).

| Suite | Passed | Failed | Reason (one line per group) |
|---|---|---|---|
| backend acceptance (`phase3-backend-first-run.log`) | 0 | 102 | the application context cannot start: the skeleton does not read the spec §4 settings (`DB_URL`), so no datasource ("Failed to determine a suitable driver class"); every behaviour is missing |
| frontend acceptance (vitest) | 0 | 9 | the skeleton page has no registration-type radio buttons (`findByRole("radio")` fails) |
| end-to-end (`phase3-e2e-first-run.log`) | 0 | 5 | E2E-01, 03, 04: no registration-type radio on the page; E2E-02: `/api/form-config` answers the proxy error page because the backend does not start; E2E-05: backend not listening on 8080 for the same reason |

No test passes on bootstrap code alone.

Phase 5 first complete run (after the build, before any phase 5 change; `phase5-first-run-*.log`): backend acceptance 102/102, frontend acceptance 9/9, e2e 5/5 passed, 0 failed; nothing to classify. One non-frozen test written in phase 5 was wrong and was fixed before its commit: `MailRegistrationNotifierTest` read MIME part headers before `saveChanges()` (defect in a non-frozen test). Containers, compilation, type check, lint and formatting of all tests succeed, so no failure is a build or harness error.

## Final run (phase 6)

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | unit | 144 / 0 | 94.64 % (742/784) | 96.50 % (276/286) | — | JaCoCo 0.8.12 |
| backend | non-unit (acceptance 102, architecture 5) | 107 / 0 | 99.11 % (777/784) | 97.90 % (280/286) | — | JaCoCo 0.8.12 |
| backend | all levels | 251 / 0 | 99.11 % (777/784) | 97.90 % (280/286) | raw 96.70 %, adjusted 99.10 % (322 killed incl. 1 timed out, 8 equivalent, 333 total; scope `si.konferenca.registration.**` without `RegistrationApplication`; tests: all 251 JUnit tests; full run + 1 survivor round) | PIT 1.30.0 + junit5-plugin 1.2.3, default mutators |
| frontend | unit | 46 / 0 | 100 % (160/160) | 93.28 % (125/134) | — | Vitest 4.1.11, v8 |
| frontend | non-unit (acceptance) | 9 / 0 | 75.00 % (120/160) | 64.92 % (87/134) | — | Vitest 4.1.11, v8 |
| frontend | all levels | 55 / 0 | 100 % (160/160) | 94.03 % (126/134) | raw = adjusted 90.85 % (407 killed of 448 viable; 4 compile-error mutants not viable, 90.04 % if counted; scope `src/**/*.{ts,tsx}` without tests, acceptance harness, `*.d.ts`, `main.tsx`; tests: all 55 Vitest tests; one full run, no rounds) | Stryker 10.0.0 + vitest-runner |
| frontend + backend | end-to-end | 5 / 0 | — | — | not run per mutant (D-15) | Playwright 1.63.0 |

Code size (cloc, image `aldanial/cloc:2.10` reporting 1.98, D-7): backend production 1 920, test 3 445 lines; frontend production 650, test 1 698 (unit 798, acceptance 343, e2e 557). Duplication: backend PMD CPD 0 violations (default 100 tokens); frontend jscpd 5.4.0 0.00 % over production `src`.

### Quality gate (phase 5)

| Component | Line (all) | Gate 95 % | Mutation adjusted | Gate 90 % | Rounds | Tests at first mutation run |
|---|---|---|---|---|---|---|
| backend | 99.11 % | met | 99.10 % (raw 96.70 %) | met | 1 | 237 |
| frontend | 100 % | met | 90.85 % | met | 0 | 55 |

Backend full run (`out/logs/phase5-pit-full.log`, `phase5-pit-full-mutations.xml`): 293 killed (1 timed out) of 332, raw 88.25 %, below the gate, so survivor round 1 followed: tests aimed at the survivors in validation, security and storage code (`FilterLimitsTest`, `ConfigurationBeansTest`, assertions in `MailRegistrationNotifierTest`, `OutboundAdaptersTest`, `RegistrationEntityTest`) and a re-run of only the 13 mutated classes (`phase5-pit-round1.log`, `phase5-pit-round1-mutations.xml`). Recomputed = round-1 classes + the other classes from the full run: 322 / 333 = 96.70 % raw, more than 1 point above the gate, so no confirming full run.

| Class (round 1 set) | Full run killed / total | Round 1 killed / total |
|---|---|---|
| adapter.jsoncopy.FileJsonCopyStore | 7 / 8 | 8 / 8 |
| adapter.mail.MailRegistrationNotifier | 28 / 29 | 29 / 29 |
| adapter.persistence.ConsentEmbeddable | 5 / 6 | 6 / 6 |
| adapter.persistence.JpaRegistrationRepository | 2 / 3 | 2 / 3 |
| adapter.web.BodySizeLimitFilter (incl. inner classes) | 18 / 26 | 25 / 26 |
| adapter.web.RateLimitFilter | 15 / 20 | 21 / 21 |
| adapter.web.RegistrationController | 16 / 19 | 16 / 19 |
| application.FormConfigService | 4 / 5 | 5 / 5 |
| config.ConferenceConfigLoader | 29 / 30 | 30 / 30 |
| config.RegistrationConfiguration | 6 / 12 | 12 / 12 |
| config.SecurityConfig | 15 / 18 | 17 / 18 |
| config.WebFilterConfiguration | 2 / 6 | 6 / 6 |
| domain.RegistrationValidator | 39 / 43 | 38 / 43 |

### Equivalent mutants (backend, 8)

| Class.method (line) | Mutation | Why it cannot change behaviour |
|---|---|---|
| `RegistrationValidator.requiredText` (103) | `return null` → `return ""` on the "required" error path | an error was added, so `validate` throws `ValidationException` before any returned value is used |
| `RegistrationValidator.requiredText` (107) | same, on the "too long" error path | same |
| `RegistrationValidator.email` (116) | same, on the "required" path | same |
| `RegistrationValidator.email` (120) | same, on the "too long" path | same |
| `RegistrationController.text` (76) | missing value `null` → `""` | the validator strips both to the empty string: a required field gets the same error; an ignored field of the other type is dropped; a blank captcha token is rejected either way |
| `RegistrationController.list` (89) | missing list `null` → empty list | the validator treats a missing options or consents list as empty |
| `RegistrationController.list` (96) | `return null` → empty list after a wrong element type | the field error was added, so `parse` throws before the value is used |
| `BodySizeLimitFilter.LimitedStream.read(byte[],int,int)` (99) | `n > 0` → `n >= 0` | `count(0)` adds nothing and cannot pass the limit |

Not counted as equivalent (survivors): `JpaRegistrationRepository.save` without `flush()` (a failure would surface at commit instead, same outcome, but the order changes), `SecurityConfig.apiSecurity` returning `null` (only the shared context covers it), `RegistrationValidator.email` (124) returning `""` for a valid email (killed in the full run, survived in round 1: test assignment under the shared Spring context).

