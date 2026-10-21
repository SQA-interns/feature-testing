# Acceptance criteria

> Owner: Product owner · Read in: phases 1, 3, 6 · Agent: read-only

This list is an input, the same in every run of this project: the agent does not derive, add, merge or reword criteria. Each criterion states one observable behaviour (Given / When / Then) and cites its story and rules. "Rejected" always means: the API answers with an error status and field errors, no database row and no JSON copy are created, and no email is sent. The open questions are answered in `scope.md` (OQ-01 to OQ-06).

Common terms used below:

- *valid external submission*: registration type `EXTERNAL`; first name, last name, email, organization filled; every mandatory consent given; a reCAPTCHA token the verifier accepts; zero or more distinct active option ids available to the type.
- *valid student submission*: as above with type `STUDENT` and study institution, study programme and student ID instead of organization.

## US-001 External participant registration

| ID | Criterion | Refs |
|---|---|---|
| AC-001-01 | Given active options are configured, when a valid external submission is sent, then it is accepted (HTTP 201) and the response contains the new registration id. | BR-01, BR-04 |
| AC-001-02 | Given the external form, when any one required field (first name, last name, email, organization) is missing, empty or only whitespace, then the submission is rejected (HTTP 400) with an error for that field. | BR-01, BR-02 |
| AC-001-03 | Given a valid external submission whose text fields have leading and trailing spaces, when it is accepted, then the stored values have the spaces removed. | BR-02 |
| AC-001-04 | Given the external form, when the email does not have a valid email format, then the submission is rejected (HTTP 400) with an error for the email field. | BR-03 |
| AC-001-05 | Given the external form, when an option id is unknown, inactive or listed twice, then the submission is rejected (HTTP 400) with an error for the options. | BR-04, SR-04, OQ-04 |
| AC-001-06 | Given the external form, when a mandatory consent is not given, then the submission is rejected (HTTP 400) with an error for that consent. | BR-05, OQ-02 |
| AC-001-07 | Given the external form, when the reCAPTCHA token is missing or the verifier rejects it, then the submission is rejected (HTTP 400) and nothing is stored. | SR-01, scope (anti-automation) |
| AC-001-08 | Given the registration page, when the participant chooses "external participant", then the page shows exactly the external fields, the active options grouped as workshops, events, meals and other activities, and every consent checkbox unchecked. | BR-01, BR-04, BR-05 |

## US-002 Student registration

| ID | Criterion | Refs |
|---|---|---|
| AC-002-01 | Given active options are configured, when a valid student submission is sent, then it is accepted (HTTP 201) and the response contains the new registration id. | BR-01, BR-04 |
| AC-002-02 | Given the student form, when any one required field (first name, last name, email, study institution, study programme, student ID) is missing, empty or only whitespace, then the submission is rejected (HTTP 400) with an error for that field. | BR-01, BR-02 |
| AC-002-03 | Given the student form, when the email does not have a valid email format, then the submission is rejected (HTTP 400) with an error for the email field. | BR-03 |
| AC-002-04 | Given an active option configured as not available to students, when a student submission selects it, then the submission is rejected (HTTP 400) with an error for the options. | BR-04, OQ-01 |
| AC-002-05 | Given the student form, when a mandatory consent is not given, then the submission is rejected (HTTP 400) with an error for that consent. | BR-05, OQ-02 |
| AC-002-06 | Given the student form, when the reCAPTCHA token is missing or the verifier rejects it, then the submission is rejected (HTTP 400) and nothing is stored. | SR-01 |
| AC-002-07 | Given the registration page, when the participant chooses "student", then the page shows exactly the student fields and only the active options available to students, grouped by category, with every consent checkbox unchecked. | BR-01, BR-04, BR-05, OQ-01 |

## US-003 Configurable conference options

| ID | Criterion | Refs |
|---|---|---|
| AC-003-01 | Given a configuration with active and inactive options, when the form configuration is requested, then it lists only the active options, each with its identifier, display name, category and the registration types it is available to. | BR-04 |
| AC-003-02 | Given the backend is started with a different options configuration (an option added, one deactivated), when the form configuration is requested, then it reflects the new configuration, with no code change. | AR-04 |
| AC-003-03 | Given an options configuration that is invalid (duplicate identifier, unknown category, missing name), when the backend starts, then it refuses to start. | AR-04 |

## US-004 Registration confirmation

| ID | Criterion | Refs |
|---|---|---|
| AC-004-01 | Given a filled form, when the backend accepts the registration, then the page shows a confirmation with the participant's name and the selected options, and the form is no longer shown. | BR-06 |
| AC-004-02 | Given a filled form, when the backend rejects the registration with field errors, then no confirmation is shown, each error is shown next to its field, and the entered values remain. | BR-06, NFR-03 |
| AC-004-03 | Given a form with an empty required field or an invalid email, when the participant submits, then the page shows the field errors and sends no request to the backend. | NFR-03 |
| AC-004-04 | Given a filled form, when the backend fails or cannot be reached, then no confirmation is shown and a general error message without technical details is shown. | BR-06, SB-07 |

## US-005 Reliable registration storage

| ID | Criterion | Refs |
|---|---|---|
| AC-005-01 | Given a valid submission, when it is accepted, then the database holds one registration with the type, every submitted field, the selected options, each consent with the time it was given, and the time of registration. | BR-07 |
| AC-005-02 | Given a valid submission, when it is accepted, then a JSON copy named after the registration id exists in the JSON copy directory and contains the registration as accepted. | BR-07 |
| AC-005-03 | Given the JSON copy cannot be written, when a valid submission is sent, then the API answers with an error (not 201) and no registration is left in the database. | BR-07, AR-05 |
| AC-005-04 | Given a valid submission with Slovenian characters (č, š, ž, Č, Š, Ž) in the names and institution, when it is accepted, then the database row and the JSON copy contain the text unchanged. | BR-03, NFR-01 |

## US-006 Participant email confirmation

| ID | Criterion | Refs |
|---|---|---|
| AC-006-01 | Given a valid submission, when it is accepted, then one email is sent to the participant's address naming the conference, the participant and the selected options. | US-006 |
| AC-006-02 | Given a submission whose text fields contain line breaks, header-like text or HTML markup, when the confirmation email is sent, then the email has no additional headers or recipients and the markup appears only as literal text. | SR-05 |
| AC-006-03 | Given the mail server is unavailable, when a valid submission is sent, then the registration is still accepted (HTTP 201) and stored. | OQ-03, OQ-03 |

## US-007 Organizer notification

| ID | Criterion | Refs |
|---|---|---|
| AC-007-01 | Given organizer addresses are configured, when a registration is accepted, then each organizer address receives a notification email containing the submitted registration data. | US-007 |
| AC-007-02 | Given a registration is accepted, when the organizer notification is sent, then it has a JSON attachment whose content equals the stored JSON copy. | US-007, BR-07, SR-07 |

## US-008 Registration export

| ID | Criterion | Refs |
|---|---|---|
| AC-008-01 | Given stored registrations, when an organizer requests the export with valid organizer credentials, then the response is an Excel workbook (`.xlsx`) with one row per registration, containing its type, fields, selected options, consents and registration time. | BR-08 |
| AC-008-02 | Given stored registrations, when the export is requested without credentials or with wrong credentials, then it is refused (HTTP 401) and no registration data is returned. | BR-08, SB-02 |
| AC-008-03 | Given a registration with Slovenian characters, when the organizer exports, then the workbook cell contains the text unchanged. | NFR-01 |
| AC-008-04 | Given organizer HTTPS-only access is on, when the export is requested with valid credentials over plain HTTP from a client that is not on localhost, then it is refused and no registration data is returned. | SR-06 |

## US-009 Organizer registration search

*Search request*: an organizer request to list registrations with an optional text, an optional registration type (`EXTERNAL` or `STUDENT`), an optional option id and an optional page number (default 1).

| ID | Criterion | Refs |
|---|---|---|
| AC-009-01 | Given stored registrations, when an organizer sends a search request with valid credentials and no text or filters, then the response lists the registrations newest first, at most 20, each with its id, type, first name, last name, email, organization or study institution, selected option ids and registration time, together with the total number of registrations. | BR-09, BR-10 |
| AC-009-02 | Given stored registrations, when the search text is part of a registration's first name, last name, email, organization or study institution in any upper or lower case, then that registration is listed and registrations without such a match are not. | BR-10 |
| AC-009-03 | Given stored external and student registrations, when the type filter is `STUDENT`, then only student registrations are listed. | BR-10 |
| AC-009-04 | Given stored registrations, when the option filter names an option id, then only registrations that selected that option are listed. | BR-10 |
| AC-009-05 | Given stored registrations, when a text, a type filter and an option filter are given together, then only registrations matching all three are listed. | BR-10 |
| AC-009-06 | Given stored registrations, when no registration matches, then the response is successful with an empty list and a total of 0. | BR-10 |
| AC-009-07 | Given 25 matching registrations, when page 1 and page 2 are requested, then page 1 lists the newest 20 and page 2 the remaining 5, and both report a total of 25. | BR-10 |
| AC-009-08 | Given the search request, when the type is unknown, the option id is not a configured option, the page number is below 1 or the text is longer than 100 characters, then the request is rejected (HTTP 400) with an error for that parameter. | BR-10, SB-01 |
| AC-009-09 | Given stored registrations, when the search is requested without credentials or with wrong credentials, then it is refused (HTTP 401) and no registration data is returned. | BR-09, SB-02 |
| AC-009-10 | Given organizer HTTPS-only access is on, when the search is requested with valid credentials over plain HTTP from a client that is not on localhost, then it is refused and no registration data is returned. | SR-06 |
| AC-009-11 | Given a registration with Slovenian characters (č, š, ž, Č, Š, Ž) in the last name, when an organizer searches for that last name, then the registration is listed and the text is unchanged. | BR-03, NFR-01 |
| AC-009-12 | Given the organizer page, when the organizer enters valid credentials, then the page shows the registration list with a search field, a type filter, an option filter and page navigation; with wrong credentials it shows an error and no registration data. | BR-09, BR-10 |
| AC-009-13 | Given a registration whose text fields contain HTML markup or script, when it is shown on the organizer page, then the markup appears only as literal text and is not executed. | SR-09 |

## Traceability

| Story | Criteria |
|---|---|
| US-001 | AC-001-01 … AC-001-08 |
| US-002 | AC-002-01 … AC-002-07 |
| US-003 | AC-003-01 … AC-003-03 |
| US-004 | AC-004-01 … AC-004-04 |
| US-005 | AC-005-01 … AC-005-04 |
| US-006 | AC-006-01 … AC-006-03 |
| US-007 | AC-007-01, AC-007-02 |
| US-008 | AC-008-01 … AC-008-04 |
| US-009 | AC-009-01 … AC-009-13 |

Total: 48 criteria.

Requirements without their own criterion, covered in the specification and verified in phase 6: SR-03 rate and size limits, NFR-02 persistence across container recreation, NFR-04 health and readiness (runtime demonstration).
