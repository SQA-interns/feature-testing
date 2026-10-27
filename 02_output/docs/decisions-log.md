# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: `.env` missing, required secrets absent
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: blocking
- Trigger: Preflight step 4. `.env` does not exist at the repository root (checked with `ls -la` and `test -e .env`). Keys marked "provided" in `project/00_setup/secrets.env.example` therefore have no value: `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS`, `NVD_API_KEY`. Without `NVD_API_KEY` the backend dependency scan (OWASP Dependency-Check, preflight step 6) cannot run.
- Options: 1. (proposed) Human copies `secrets.env.example` to `.env` and fills the five keys above (the reCAPTCHA and SMTP keys may stay empty: test-mode only / not needed); 2. none: the run cannot pass phase 0 without them.
- Human response: "updated .env" (2026-10-27T10:06:23Z); re-check: all five keys non-empty, organizer password at least 16 characters, `.env` ignored by git
- Resolution: option 1

## D-02: Docker Engine and Compose below the pinned minor versions
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: blocking
- Trigger: Preflight step 2. `tech-stack.md` pins Docker Engine 29.8.1 and Docker Compose 5.5.1. Installed (Docker Desktop, context `desktop-linux`): Engine server 29.3.1 (`docker version`, `docker info --format '{{.ServerVersion}}'`), Compose 5.1.1 (`docker compose version`, `docker compose version --short`). These are different minor versions, so `pre-approved.md` PA-05 (patch only, and it does not list Compose) does not apply. Every pinned image resolves and the tool containers (gitleaks, semgrep, cloc) run on the installed versions.
- Options: 1. (proposed) Approve Engine 29.3.1 and Compose 5.1.1 for this run; `tech-stack.md` stays authoritative; 2. Human upgrades Docker Desktop to a release with Engine 29.8.1 and Compose 5.5.1.
- Human response: "lets use these docker versions we have now" (2026-10-27T10:06:23Z)
- Resolution: option 1: Engine 29.3.1 and Compose 5.1.1 approved for this run

## D-03: Host JDK is Ubuntu OpenJDK 21.0.12.1, not Eclipse Temurin 21.0.10+7
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: blocking
- Trigger: Preflight step 2. `java -version` and `java -XshowSettings:properties` report `21.0.12.1+1-1-24.04.4-Ubuntu`, vendor Ubuntu; `javac -version` reports 21.0.12.1. The patch difference matches PA-05, but the vendor differs from the pin (Eclipse Temurin), which PA-05 does not cover. No Temurin JDK is installed under `/usr/lib/jvm`. The runtime image `eclipse-temurin:21.0.10_7-jre-alpine` is unaffected.
- Options: 1. (proposed) Approve the installed Ubuntu OpenJDK 21.0.12.1 for host builds and tests in this run (PA-05 for the patch, this record for the vendor); 2. Human installs Eclipse Temurin JDK 21.0.10+7.
- Human response: "same for jdk" (2026-10-27T10:06:23Z)
- Resolution: option 1: Ubuntu OpenJDK 21.0.12.1 approved for host builds in this run

## D-04: High vulnerability in pinned `vite` 6.4.3
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: blocking
- Trigger: Preflight step 6. `npm audit` (npm 11.6.2) over the pinned frontend set reports `vite` 6.4.3 as High: GHSA-9jrq-w75r-8gcw (cross-origin script execution through custom middleware), GHSA-rq7h-c2jc-7f22 (files outside `server.fs.allow` served), and launch-editor advisories GHSA-698v-4r3x-3vcv, GHSA-r825-w8mm-5pxw, GHSA-587h-7wmj-h5pm. `npm audit --omit=dev` reports 0. Not covered by `pre-approved.md`. Full output: `out/logs/00_npm-audit.json`. Re-audit with `vite` 6.4.5 (same minor, patch release) reports no High.
- Options: 1. (proposed) Approve pinning `vite` 6.4.5 instead of 6.4.3 for this run; 2. Keep 6.4.3 and lower to Medium with the evidence that vite is build and dev-server tooling only and the production bundle is served by nginx (lowering a High needs this approval).
- Human response: "do what you would recommend" (2026-10-27T10:06:23Z)
- Resolution: option 1: `vite` 6.4.5 replaces 6.4.3 for this run

## D-05: Moderate `qs` advisories through `@stryker-mutator/core` (PA-03)
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports `qs` 6.15.1 (GHSA-q8mj-m7cp-5q26, GHSA-x5fp-wj9c-mxmx, GHSA-4mjr-xmp4-gh2g) and `typed-rest-client` 2.3.1 as Moderate; the lock file shows `typed-rest-client` required only by `@stryker-mutator/core` 10.0.0. `npm audit --omit=dev` reports none. Matches `pre-approved.md` PA-03.
- Options: 1. (applied) Accept per PA-03, recorded as Medium, accepted.
- Human response: none (pre-approved, PA-03)
- Resolution: option 1

## D-06: Moderate advisories in `vitest` / `@vitest/mocker` / `@vitest/coverage-v8` 4.1.11
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports `@vitest/mocker` 4.1.11 Moderate (GHSA-qxg6-89mg-rqv8, GHSA-xp26-3454-mjgp), carried to `vitest` and `@vitest/coverage-v8` 4.1.11. Test tooling only; `npm audit --omit=dev` reports none. Not covered by PA-03 (different artifact), but Moderate does not block preflight. 4.1.12 fixes it (verified by re-audit).
- Options: 1. (default) Keep the 4.1.11 pins and record as Medium, accepted (dev-only, no production exploit path); 2. Human approves 4.1.12 for both packages.
- Human response: "use that vitest, even if it has a vulnerability" (2026-10-27T10:06:23Z)
- Resolution: option 1: keep `vitest` and `@vitest/coverage-v8` 4.1.11; Medium, accepted

## D-07: `aldanial/cloc:2.10` image reports cloc 1.98
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: non-blocking
- Trigger: `docker run --rm aldanial/cloc:2.10 --version` prints 1.98. The pin resolves and runs; `tech-stack.md` rules make a differing reported version non-blocking.
- Options: 1. (applied) Keep the pinned tag `2.10` as authoritative and record the reported version.
- Human response: none
- Resolution: option 1

## D-08: OSS Index analyser of Dependency-Check disabled
- Timestamp: 2026-10-27T10:13:28Z
- Phase: 0
- Type: non-blocking
- Trigger: Preflight step 6. `dependency-check-maven` 12.1.0 failed: Sonatype OSS Index returned 401 Unauthorized. OSS Index credentials are not listed in `secrets.env.example`. The NVD analyser (with `NVD_API_KEY`) ran.
- Options: 1. (applied) Set `ossindexAnalyzerEnabled=false` in `02_output/backend/pom.xml` and re-run; the scan completes with the NVD analyser.
- Human response: none
- Resolution: option 1

## D-09: CVE-2025-7962 on `angus-activation` 2.0.3 (PA-01)
- Timestamp: 2026-10-27T10:13:28Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check (NVD) reports CVE-2025-7962, CVSS 7.5 (High by `severity-scale.md`), on `org.eclipse.angus:angus-activation` 2.0.3. The resolved `angus-mail` is 2.0.5 (`out/logs/00_backend-dependency-list.txt`), so the evidence of `pre-approved.md` PA-01 matches.
- Options: 1. (applied) Classify Low, false positive; suppress for that one artifact only (`02_output/backend/dependency-check-suppressions.xml`).
- Human response: none (pre-approved, PA-01)
- Resolution: option 1

## D-10: CVE-2025-15104 on `hibernate-validator` 9.1.3.Final (PA-02)
- Timestamp: 2026-10-27T10:13:28Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check (NVD) reports CVE-2025-15104, CVSS 5.3, on `hibernate-validator`; a CPE name collision with the Nu Html Checker, matching `pre-approved.md` PA-02.
- Options: 1. (applied) Classify Low, false positive; suppress for that one artifact only.
- Human response: none (pre-approved, PA-02)
- Resolution: option 1

## D-11: Response to organizer credentials over plain HTTP (AC-008-04, SR-06)
- Timestamp: 2026-10-27T10:15:06Z
- Phase: 1
- Type: non-blocking
- Trigger: AC-008-04 requires refusal but does not say how; SR-06 says credentials are never accepted over plain HTTP except on localhost.
- Options: 1. (chosen, conservative) Answer HTTP 403 with a general error and no data, without evaluating the credentials and without a redirect; 2. Redirect (308) to HTTPS, which invites clients to resend the credentials.
- Human response: none
- Resolution: pending review

## D-12: Shipped conference options configuration
- Timestamp: 2026-10-27T10:15:06Z
- Phase: 1
- Type: non-blocking
- Trigger: G-04 in `docs/01_requirements-check.md`; the inputs define the option fields (BR-04, OQ-01) but no shipped option set, while E2E-01 and E2E-02 depend on one.
- Options: 1. (chosen) Ship an example configuration with at least one active option per category available to both types, at least one active option available only to external participants, and one inactive option; the operator replaces it through configuration (AR-04); 2. Ship an empty option set.
- Human response: none
- Resolution: pending review

## D-13: User-facing language
- Timestamp: 2026-10-27T10:15:06Z
- Phase: 1
- Type: non-blocking
- Trigger: G-05; no language is stated for the form, confirmation, errors or emails. The only given wording, the OQ-02 consent text, is English.
- Options: 1. (chosen) English throughout, matching the given consent wording; Slovenian characters are supported in all data (NFR-01); 2. Slovenian user interface, which would need wording nobody has approved.
- Human response: none
- Resolution: pending review

## D-14: Maximum lengths of text fields
- Timestamp: 2026-10-27T10:15:06Z
- Phase: 1
- Type: non-blocking
- Trigger: G-06; no field length is given, but SR-03 needs a body size limit and the database needs column sizes.
- Options: 1. (chosen) 200 characters per name, institution and programme field, 254 for email (RFC 5321 path limit), 50 for student ID, counted after trimming; longer input is rejected with a field error (HTTP 400); request body limit 16 KB; 2. No field limits, body limit only.
- Human response: none
- Resolution: pending review

## D-15: Contract validation with the host's Python parsers
- Timestamp: 2026-10-27T10:41:40Z
- Phase: 2
- Type: non-blocking
- Trigger: The phase 2 gate needs the contracts validated with a parser; `tech-stack.md` lists no OpenAPI or JSON Schema validator.
- Options: 1. (chosen) Validate with the host's existing python3 PyYAML 6.0.1 and jsonschema 4.10.3 (Draft 2020-12): `openapi.yaml` against the official OAS 3.1 schema (2022-10-07) plus $ref resolution, each JSON schema against its meta-schema with its examples and negative cases, and `database-schema.sql` applied to the pinned `postgres:16.15-alpine`. Script `02_output/tools/validate-contracts.py`; nothing installed and no project dependency added; 2. Add an npm OpenAPI validator to `tech-stack.md` tooling as dev-only tooling.
- Human response: none
- Resolution: option 1

## D-16: Frozen harness ignores per-class configuration overrides
- Timestamp: 2026-10-27T11:22:00Z
- Phase: 4
- Type: blocking
- Trigger: First backend run in phase 4 (`out/logs/04_backend-run-1.log`): 85/89 acceptance tests pass; the 4 failures are in `RecaptchaVerificationAcceptanceTest` (AC-001-07, AC-002-06, DoD-P05), whose context still runs in reCAPTCHA test mode. Cause: with Spring Framework 7.0.9 the base class `AcceptanceTestBase` registers its `@DynamicPropertySource` values after the subclass's, so the base value wins for the same key (reproduced outside the repository: base "X=base", subclass "X=sub" gives "base"; a subclass method of the same name does not hide it). The same defect makes `MailServerDownAcceptanceTest` (AC-006-03) pass without the mail server being down (no "email not sent" log line). Both files and the base are listed in `docs/03_acceptance-manifest.sha256`. A correct implementation cannot make the 4 tests pass, and AC-006-03 is not really tested.
- Options: 1. (proposed) Make the harness apply overrides explicitly: `AcceptanceTestBase` no longer carries a `@DynamicPropertySource`; it offers `register(registry, overrides)`, and each of the 9 concrete acceptance classes declares one `@DynamicPropertySource` that calls it (`Map.of()` for none; the reCAPTCHA and mail-down classes pass their current overrides unchanged). No assertion changes; the manifest is regenerated for the 10 changed files by a commit the human approves; 2. Smaller change in 3 files (`AcceptanceTestBase`, `RecaptchaVerificationAcceptanceTest`, `MailServerDownAcceptanceTest`): subclasses hand their overrides to the base through a static map that the base merges in; it relies on Spring calling the subclass method first, which is undocumented; 3. Leave the tests frozen as they are: the phase 4 gate cannot pass (4 tests fail) and AC-006-03 stays untested.
- Human response: "do the recommended fix and document that frozen files have been modified. re freeze the files when done." (2026-10-27T11:32:48Z)
- Resolution: option 1; the 10 changed harness and test files are re-frozen in a new manifest commit, listed in docs/03_test-strategy.md

## D-17: End-to-end tests are outside the coverage and mutation runs
- Timestamp: 2026-10-27T15:31:31Z
- Phase: 5
- Type: non-blocking
- Trigger: `general/quality/measurement-scope.md` asks for each component's full automated suite, at every level, under the coverage and mutation tools. The 5 Playwright end-to-end tests drive the containers of `docker-compose.yml`: the backend runs from the packaged jar inside its image and the frontend as the nginx-served production build, so neither JaCoCo/PIT (which instrument or mutate classes in the Maven test JVM) nor Vitest v8/Stryker (which instrument or mutate the sources run by Vitest) can see them. Mutating would need an image rebuild and stack restart per mutant.
- Options: 1. (chosen) Run every JUnit level (acceptance, unit, slice, architecture) under JaCoCo and PIT, and every Vitest level (acceptance, unit/component) under Vitest coverage and Stryker; record the end-to-end tests as passed/failed only, and name the tests run with each figure; 2. Instrument the containers (JaCoCo agent in the backend image, an istanbul build of the frontend) for coverage only; mutation stays infeasible, and it changes the shipped images or needs a second build.
- Human response: none
- Resolution: option 1; non-unit coverage is the JUnit/Vitest acceptance tests, and the mutation figures name their tests

## D-18: Semgrep rates HTTP Basic for the organizer export as High (F-01)
- Timestamp: 2026-10-27T16:04:47Z
- Phase: 6
- Type: blocking
- Trigger: Semgrep `p/default` rule `use-of-basic-authentication` (ERROR, High by `severity-scale.md`) on `docs/02_contracts/openapi.yaml:108` (`organizerBasic`, implemented in `web.SecurityConfiguration`). Lowering a High needs written evidence and a blocking decision; AGENTS.md forbids finishing with an open High.
- Evidence for lowering: Basic is the mechanism chosen in phase 2 (specification S7.2) for the single organizer role and single export operation that `security-requirements.md` names; identity providers are out of scope (`scope.md`). The credentials are never accepted over plain HTTP outside localhost (SR-06: 403 before the credentials are read, AC-008-04; production TLS at the external nginx, SB-04). The password is kept only as a BCrypt-12 hash (SB-03); the API is stateless with no session or cookie, so there is no CSRF or session-fixation surface; failed logins count against the export rate limit (SR-03, default 10 per minute per client). ASVS 5.0 Level 1 (the chosen level) does not exclude Basic over TLS. The frozen tests AC-008-01/02 and E2E-05 specify Basic (`WWW-Authenticate: Basic`).
- Options: 1. (recommended) Lower F-01 to Low on this evidence: an accepted design choice, not an exploitable weakness; the release notes name Basic and the need for TLS and a strong password; 2. Replace Basic with another scheme (for example a signed bearer token or form login with a session): changes the API contract and needs a change to the frozen tests AC-008-01/02 and E2E-05, plus new design and phases 4–6 for that part; 3. Keep F-01 High: the run cannot be released (DoD-05, AGENTS.md).
- Human response: "do recomended" (2026-10-27T16:14:49Z)
- Resolution: option 1; F-01 lowered to Low and accepted; the release notes name HTTP Basic with the TLS and strong-password requirements
