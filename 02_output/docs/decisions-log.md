# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Host JDK and Docker Engine differ from the pins by a patch version
- Timestamp: 2026-10-21T11:06:00Z
- Phase: 0
- Type: non-blocking
- Trigger: `tech-stack.md` pins JDK 21.0.10+7 and Docker Engine 29.8.1; the host has JDK 21.0.11+9 (Oracle HotSpot build, not Temurin) and Docker Engine 29.8.2. Both are patch releases of the pinned minor version and every pinned image and tool runs. Matches PA-05 (`project/00_setup/pre-approved.md`). The runtime image stays `eclipse-temurin:21.0.10_7-jre-alpine` (verified: `openjdk version "21.0.10"`).
- Options: 1. Approve the installed versions for this run per PA-05, keep `tech-stack.md` unchanged (default); 2. Install the exact pins.
- Human response: none (pre-approved, PA-05)
- Resolution: option 1

## D-02: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-21T11:08:00Z
- Phase: 0
- Type: non-blocking
- Trigger: `aldanial/cloc:2.10` (tech-stack `tooling`) resolves and runs, but `cloc --version` prints `1.98`. Rule in `tech-stack.md`: a pin that resolves and runs but reports another version is non-blocking; the listed pin stays authoritative.
- Options: 1. Keep the pinned tag `2.10` and record the reported version (default); 2. Ask for another tag.
- Human response: none
- Resolution: option 1

## D-03: OWASP Dependency-Check OSS Index analyser disabled
- Timestamp: 2026-10-21T11:15:00Z
- Phase: 0
- Type: non-blocking
- Trigger: `dependency-check-maven` 12.1.0 failed with HTTP 401 from `ossindex.sonatype.org`; the OSS Index analyser needs credentials that `secrets.env.example` does not list (preflight step 6).
- Options: 1. Disable the OSS Index analyser (`ossindexAnalyzerEnabled=false`) and scan with the NVD analysers using `NVD_API_KEY` (default); 2. Add OSS Index credentials (needs a change to `secrets.env.example`).
- Human response: none
- Resolution: option 1; the re-run completed (67 dependencies, NVD data) — `out/logs/00_backend-dependency-check.log`

## D-04: CVE-2025-7962 on angus-activation (PA-01)
- Timestamp: 2026-10-21T11:16:00Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-7962 (CVSS 7.5, High by `severity-scale.md`) on `org.eclipse.angus:angus-activation` 2.0.3. The shipped `angus-mail` is 2.0.5 (dependency tree, `out/logs/00_backend-dependency-tree.txt`), so the evidence of PA-01 matches.
- Options: 1. Classify Low, false positive; suppress for that artifact only (default, PA-01).
- Human response: none (pre-approved, PA-01)
- Resolution: option 1 — `02_output/backend/dependency-check-suppressions.xml`

## D-05: CVE-2025-15104 on hibernate-validator (PA-02)
- Timestamp: 2026-10-21T11:16:00Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-15104 (CVSS 5.3) on `hibernate-validator` 9.1.3.Final; the CVE concerns the Nu Html Checker (CPE `validator:validator` name collision). Matches PA-02.
- Options: 1. Classify Low, false positive; suppress for that artifact only (default, PA-02).
- Human response: none (pre-approved, PA-02)
- Resolution: option 1 — `02_output/backend/dependency-check-suppressions.xml`

## D-06: npm audit Moderate advisories on qs via @stryker-mutator/core (PA-03)
- Timestamp: 2026-10-21T11:17:00Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports Moderate advisories on `qs` 6.15.1 and `typed-rest-client` 2.3.1, reached only through `@stryker-mutator/core` 10.0.0 (`npm ls qs`); `npm audit --omit=dev` reports 0 vulnerabilities. Matches PA-03.
- Options: 1. Accept; record as Medium, accepted (default, PA-03).
- Human response: none (pre-approved, PA-03)
- Resolution: option 1

## D-07: vite 6.4.3 has a High npm advisory
- Timestamp: 2026-10-21T11:18:00Z
- Phase: 0
- Type: blocking
- Trigger: `npm audit` reports High GHSA-587h-7wmj-h5pm (plus Moderate GHSA-698v-4r3x-3vcv and GHSA-r825-w8mm-5pxw) on `vite` <= 6.4.3, through its bundled `launch-editor` (dev server only; not in the production bundle, `npm audit --omit=dev` is clean). Confirmed twice (`npm audit --json`, `npm audit --audit-level=high` exit 1). Preflight step 6: any Critical or High result is blocking; changing a `tech-stack.md` entry needs human approval.
- Options: 1. Use `vite` 6.4.4 (released 2026-10-06, first fixed patch; a scratch install with 6.4.4 audits with no High, only the D-06 Moderates; `vitest` 4.1.11 and `@vitejs/plugin-react` 4.7.0 dedupe onto it) (default); 2. Use `vite` 6.4.5 (released 2026-10-21, today); 3. Keep 6.4.3 and accept the High as dev-only (needs written evidence, lowering a High).
- Human response: pending
- Resolution: pending

## D-08: Docker Compose 5.6.0 installed, 5.5.1 pinned
- Timestamp: 2026-10-21T11:19:00Z
- Phase: 0
- Type: blocking
- Trigger: `docker compose version` reports v5.6.0 (confirmed with `--short`); `tech-stack.md` pins 5.5.1. This is a minor-version difference, not covered by PA-05 (patch only). Compose 5.6.0 ran a pinned image (`axllent/mailpit:v1.31.1`) with `up --wait` and a health check, then `down`, without errors.
- Options: 1. Approve Compose 5.6.0 for this run, keep `tech-stack.md` unchanged (default; everything runs); 2. Install Compose 5.5.1.
- Human response: pending
- Resolution: pending

## D-09: Human answers to D-07 and D-08
- Timestamp: 2026-10-21T11:23:43Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-07 (vite High advisory) and D-08 (Docker Compose 5.6.0).
- Options: as listed in D-07 and D-08.
- Human response: D-07: "vite 6.4.4"; D-08: "Approve 5.6.0" (2026-10-21T11:23:43Z)
- Resolution: D-07 option 1 — `vite` 6.4.4 replaces the `tech-stack.md` entry 6.4.3 for this run; D-08 option 1 — Docker Compose 5.6.0 approved for this run. `tech-stack.md` is not edited.

## D-10: Stray duplicate of the OQ answers in business-rules.md
- Timestamp: 2026-10-21T11:27:28Z
- Phase: 1
- Type: non-blocking
- Trigger: `project/01_requirements/business-rules.md` ends, after the glossary entry "Raw JSON copy", with the answers to OQ-01..OQ-06 repeated without IDs (gap G-1 in `docs/01_requirements-check.md`). The text is identical to the answers in `scope.md`.
- Options: 1. Use `scope.md` as the source for the answers and treat the stray text as an editorial duplicate (default); 2. Ask the product owner.
- Human response: none
- Resolution: option 1 (no behaviour depends on it)

## D-11: Consent id not in the configuration
- Timestamp: 2026-10-21T11:27:28Z
- Phase: 1
- Type: non-blocking
- Trigger: BR-05 and AC-001-06 / AC-002-05 cover a missing mandatory consent; an unknown consent id in `consents` is not specified (gap G-3).
- Options: 1. Reject with HTTP 400 and an error for `consents` (conservative, default); 2. Ignore unknown ids.
- Human response: none
- Resolution: pending review (option 1 applied)

## D-12: Maximum lengths of text fields
- Timestamp: 2026-10-21T11:27:28Z
- Phase: 1
- Type: non-blocking
- Trigger: no maximum length is stated for the participant text fields (gap G-4); unbounded input conflicts with SR-03 (request-size limit) and SB-01.
- Options: 1. At most 200 characters per text field and 254 for the email, longer values rejected with a field error (conservative, default); 2. No per-field limit, only the request-size limit.
- Human response: none
- Resolution: pending review (option 1 applied)

## D-13: Missing or unknown registration type
- Timestamp: 2026-10-21T11:27:28Z
- Phase: 1
- Type: non-blocking
- Trigger: `api-contract.md` defines the types `EXTERNAL` and `STUDENT`; a missing or other value is not covered by an AC (gap G-5).
- Options: 1. Reject with HTTP 400 and an error for `type`, nothing stored or sent (conservative, default); 2. Default to `EXTERNAL`.
- Human response: none
- Resolution: pending review (option 1 applied)

## D-14: Add @types/node 24.13.6 as a dev dependency of the frontend
- Timestamp: 2026-10-21T11:54:31Z
- Phase: 3
- Type: non-blocking
- Trigger: the end-to-end helpers (`frontend/e2e/support`) use Node APIs (`child_process`, `zlib`, `process`) and the frontend type check (`tsc --noEmit`, ES-05) covers `e2e/`; no `@types/node` is listed in `tech-stack.md` or installed transitively.
- Options: 1. Add `@types/node` 24.13.6 (exact, MIT, published 2026-09-19, matches the Node 24.13 runtime), dev scope only (default); 2. Exclude `e2e/` from the type check.
- Human response: none
- Resolution: option 1 (rule in `tech-stack.md`: an unlisted tool with an exact version and a non-blocking decision; scanned in phase 6; `npm audit --audit-level=high` passes)

## D-15: End-to-end tests are not part of the mutation runs
- Timestamp: 2026-10-21T13:25:49Z
- Phase: 5
- Type: non-blocking
- Trigger: `general/quality/measurement-scope.md` ("Mutation score") asks for each component's full automated suite at every level, and a decision record when that is infeasible. The five e2e tests (`frontend/e2e`) drive both components through the browser against `docker compose up`; a mutant would need a rebuilt jar or bundle, new images and a restarted stack for each run.
- Options: 1. Run PIT with every backend test (unit, integration, architecture, acceptance) and Stryker with every Vitest test (unit, component, acceptance); leave out only the e2e level, recorded in `docs/03_test-strategy.md` (default); 2. Rebuild and restart the stack per mutant.
- Human response: none
- Resolution: option 1
