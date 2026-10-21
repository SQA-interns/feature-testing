# End-to-end scenarios

> Owner: Product owner · Read in: phases 3, 6 · Agent: read-only

The end-to-end tests of this project: exactly one test per scenario below, run against the real local stack (`docker compose up`: frontend, backend, PostgreSQL, Mailpit) through the browser, and the same in every run of this project. The agent writes no other end-to-end test and no scenario is merged or split. Each scenario names the criteria it demonstrates; the acceptance tests (one or more per criterion) carry the detailed cases, so an end-to-end test checks the journey once and does not repeat them.

| ID | Scenario | Steps and observations | Covers |
|---|---|---|---|
| E2E-01 | External participant registers | Open the page, choose "external participant", fill the form with valid data, select at least one option per category, submit. The page shows the confirmation. The database holds the registration, the JSON copy exists, the participant email and the organizer email (with the JSON attachment) are in Mailpit. | AC-001-01, AC-001-08, AC-004-01, AC-005-01, AC-005-02, AC-006-01, AC-007-01, AC-007-02, DoD-P01 to DoD-P03 |
| E2E-02 | Student registers | As E2E-01 with "student": only the student fields and student-available options are shown; the registration is stored and both emails arrive. | AC-002-01, AC-002-07, AC-004-01, DoD-P01 to DoD-P03 |
| E2E-03 | Field errors are shown next to the fields | Submit the form with an empty required field and an invalid email: errors appear next to the fields and no request reaches the backend; then make the backend reject a submission (for example an unavailable option): the backend's field errors appear next to their fields and the entered values remain. | AC-004-02, AC-004-03, NFR-03 |
| E2E-04 | Slovenian characters survive the whole chain | Register with č, š, ž, Č, Š, Ž in the names and institution. The text is unchanged in the page confirmation, the database row, the JSON copy, both emails and the exported workbook. | AC-005-04, AC-008-03, NFR-01 |
| E2E-05 | Organizer exports registrations | Request the export with valid organizer credentials: a valid Excel workbook with one row per registration is downloaded. The same request without credentials is refused (HTTP 401) and returns no data. | AC-008-01, AC-008-02, DoD-P04 |
| E2E-06 | Organizer searches registrations | Register one external and one student participant with known, distinct last names and options. Open the organizer page, enter wrong credentials: an error is shown and no data. Enter valid credentials: both registrations are listed. Search for the student's last name: only the student is listed. Clear the text and filter by type "external": only the external participant is listed. | AC-009-01, AC-009-02, AC-009-03, AC-009-12, DoD-P06 |

Not end-to-end tests (verified by runtime demonstration in phase 6): NFR-02 (data survives recreation of every container) and NFR-04 (health and readiness).
