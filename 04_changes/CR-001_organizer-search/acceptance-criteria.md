# Acceptance criteria: CR-001

> Written in: change interview · Source: requester's answers · Agent: writes until sign-off, then read-only

Same rules as the baseline list: one observable behaviour per row (Given / When / Then), each citing its story and rules. Terms the baseline defines ("rejected", "valid … submission") keep their meaning here. The agent does not derive, add, merge or reword criteria.

## US-009 Organizer registration search

| ID | Change | Criterion | Refs |
|---|---|---|---|
| AC-009-01 | ADDED | Given stored registrations, when `/organizer` is opened, then a login form for the organizer username and password is shown and no registration data. | BR-09 |
| AC-009-02 | ADDED | Given the registration page, when it is shown for either registration type, then it contains no link to `/organizer`. | BR-09 |
| AC-009-03 | ADDED | Given stored registrations, when the organizer search API is called without credentials or with wrong credentials, then it is refused (HTTP 401) and no registration data is returned. | BR-08, SR-09 |
| AC-009-04 | ADDED | Given the organizer login form, when wrong credentials are entered, then an error message is shown and no registration data. | BR-09, SR-09 |
| AC-009-05 | ADDED | Given organizer HTTPS-only access is on, when the organizer search API is called with valid credentials over plain HTTP from a client that is not on localhost, then it is refused and no registration data is returned. | SR-06, SR-09 |
| AC-009-06 | ADDED | Given stored registrations, when the organizer logs in with valid credentials, then the page lists every registration, newest registration first, with the number of matches. | BR-13 |
| AC-009-07 | ADDED | Given a listed registration, when the page shows it, then it shows its type, every field, the selected options, the consents with the time given and the registration time, and no other data. | BR-10, SR-07 |
| AC-009-08 | ADDED | Given stored registrations, when the search text is part of the first name, last name, email, organization or study institution of a registration, then that registration is listed and registrations without such a match are not. | BR-11 |
| AC-009-09 | ADDED | Given a registration with last name "Novak", when the organizer searches for "NOVAK" or "novak", then it is listed. | BR-11 |
| AC-009-10 | ADDED | Given registrations with last names "Koš", "Kos" and "Kós", when the organizer searches for "kos", then all three are listed. | BR-11, OQ-07 |
| AC-009-11 | ADDED | Given registrations with last names "Koš" and "Kos", when the organizer searches for "koš" or "KOŠ", then only "Koš" is listed. | BR-11, OQ-07 |
| AC-009-12 | ADDED | Given registrations with last names "Čač", "Ćać" and "Cac", when the organizer searches for "č", then only "Čač" is listed. | BR-11, OQ-07 |
| AC-009-13 | ADDED | Given registrations with last names "Đurić" and "Durić", when the organizer searches for "duric", then both are listed. | BR-11, OQ-07 |
| AC-009-14 | ADDED | Given stored registrations, when the search text has leading and trailing spaces, then the result is the same as for the text without them. | BR-11 |
| AC-009-15 | ADDED | Given stored registrations and no filter set, when the search text is empty or only whitespace, then every registration is listed. | BR-11 |
| AC-009-16 | ADDED | Given stored registrations, when the organizer search API is called with a search text longer than 100 characters, then it is rejected (HTTP 400) with an error for the search field and no registration data. | BR-11 |
| AC-009-17 | ADDED | Given the organizer page, when the organizer searches with a text longer than 100 characters, then an error is shown next to the search bar and no result list. | BR-11, NFR-03 |
| AC-009-18 | ADDED | Given external and student registrations, when the type filter is set to student or to external participant, then only registrations of that type are listed. | BR-12 |
| AC-009-19 | ADDED | Given registrations with and without a given option, when the filter of that option's category is set to it, then only the registrations that selected it are listed. | BR-12 |
| AC-009-20 | ADDED | Given stored registrations, when a search text, the type filter and a category filter are set together, then only registrations matching all three are listed. | BR-12 |
| AC-009-21 | ADDED | Given a configuration with active and inactive options, when the organizer opens a category filter, then it offers "any" and every configured option of that category, active and inactive. | BR-12 |
| AC-009-22 | ADDED | Given stored registrations, when the organizer search API is called with an unknown registration type, an unknown option id or an option of another category in a filter, then it is rejected (HTTP 400) with an error for that filter and no registration data. | BR-12 |
| AC-009-23 | ADDED | Given stored registrations, when no registration matches, then the page shows "No registrations match" and the number of matches is 0. | BR-13 |
| AC-009-24 | ADDED | Given stored registrations, when a result is shown, then every configured option of every category, active and inactive, is listed grouped by category with the number of matching registrations that selected it, zero included. | BR-13 |
| AC-009-25 | ADDED | Given external and student registrations with meal options, when the type filter is set to student, then the option counts include only student registrations. | BR-13 |
| AC-009-26 | ADDED | Given the organizer page, when a filter is changed, then the list and the counts are updated without pressing Search. | BR-13 |
| AC-009-27 | ADDED | Given the organizer page, when a search text is entered and Search or Enter is pressed, then the list and the counts are updated. | BR-13 |
| AC-009-28 | ADDED | Given a registration with č, š, ž, Č, Š, Ž in the names and institution, when the page lists it, then the text is shown unchanged. | NFR-01 |
| AC-009-29 | ADDED | Given a registration whose text fields contain HTML markup or script, when the page lists it, then the markup appears only as literal text and no script runs. | SR-09 |
| AC-009-30 | ADDED | Given the organizer is logged in on the page, when the page is reloaded, then the login form is shown again, and no browser storage or cookie holds the credentials. | SR-09 |

## End-to-end scenarios

Exactly one end-to-end test per scenario; no scenario is merged or split.

| ID | Change | Scenario | Steps and observations | Covers |
|---|---|---|---|---|
| E2E-06 | ADDED | Organizer searches registrations | Register one external participant with last name "Koš" and one student with another name, each with a meal option. Open `/organizer`: wrong credentials show an error and no data; valid credentials list both registrations, newest first, with the count 2 and the per-option counts. Search "kos": only "Koš" is listed, unchanged. Clear the search and set the type filter to student: only the student is listed and the meal counts change accordingly. | AC-009-01, AC-009-04, AC-009-06, AC-009-10, AC-009-18, AC-009-24, AC-009-25, AC-009-28, NFR-01 |
