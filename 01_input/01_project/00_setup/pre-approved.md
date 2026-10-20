# Pre-approved findings

> Owner: Security officer · Read in: phases 0, 6 · Agent: read-only

Findings the human already decided for this project, so the agent does not stop to ask. For an item below, record a **non-blocking** decision that cites this file, apply the action and continue. Apply an item only when the evidence matches the row exactly (same finding, same artifact); anything else is a normal decision (`general/working-rules.md`).

| ID | Finding | Evidence that it matches | Action |
|---|---|---|---|
| PA-01 | CVE-2025-7962 (High by `severity-scale.md`) on `org.eclipse.angus:angus-activation` | The CVE is in the mail library before 2.0.4; the shipped `angus-mail` is 2.0.5 or later | Classify Low, false positive; suppress it for that one artifact only |
| PA-02 | CVE-2025-15104 on `hibernate-validator` | The CVE concerns the Nu Html Checker; the match is a name collision on the CPE `validator:validator` | Classify Low, false positive; suppress it for that one artifact only |
| PA-03 | `npm audit`: Moderate advisories (for example `qs`) reached only through `@stryker-mutator/core` | `npm audit --omit=dev` reports none; the package is dev tooling and not in the bundle | Accept; record as Medium, accepted |
| PA-04 | Semgrep "basic authentication" (High) on the organizer endpoint | HTTP Basic is required by `architecture.md` and `security-requirements.md`, used over HTTPS or localhost only, the password is stored only as a BCrypt hash, failed logins are rate limited | Lower to Low and accept |
| PA-05 | Docker Engine, JDK, Node or npm installed with a different patch version than pinned | The installed version is a patch release of the pinned minor version; all pinned images and tools run | Approve the installed version for this run; do not edit `tech-stack.md` |

Not pre-approved: any Critical finding, any finding on a different artifact, and any change to a technology, version or service other than PA-05.
