# CR-001: Organizer registration search

> Written in: change interview · Source: requester's answers and the baseline · Agent: writes until sign-off, then read-only

- Type: add
- Baseline: tjan/single-agent/sdd/v002/run-2-search @ 4d664d2
- Requested by: tjan · Approved: 2026-10-28T21:47:37Z
- Security impact: the personal data of every registration (SR-07 data only) is shown to organizers on a new frontend page, read through a new organizer-only API with the existing organizer credentials (BR-08, SB-02, SR-06, SR-09); the API is rate and size limited (SR-03); user-entered text is displayed only as literal text; no participant-facing change.

## Intent

Organizers can open a page in the application, log in with the organizer credentials, and search and filter the registrations: by name, email or institution (working for č, š, ž and other accents), by registration type and by the selected workshop, event, meal or other activity. Per-option counts for the result let, for example, the lunch crew see how many of each meal to prepare. This lifts the scope exclusion of an administration UI for this read-only page only.
Out of scope: editing or deleting registrations, sorting by column, exporting the filtered result, a date-range filter, several options in one filter, option configuration in the UI.

## Stories

### US-009 Organizer registration search

As an organizer I want to search and filter the registrations on a page and see how many selected each option so that I can find participants and plan workshops, events and meals.
In scope: the organizer page with login, search bar, type and category filters, result list and per-option counts. Out of scope: the export (US-008), which stays the full list.

## Rules and data

| ID | Change | Rule |
|---|---|---|
| BR-09 | ADDED | Organizers have a read-only registration search page at `/organizer` in the frontend. It is not linked from the registration page and shows no registration data before the organizer credentials are accepted (BR-08). |
| BR-10 | ADDED | Each listed registration shows the data its export row holds: type, every field, the selected options, the consents with the time given, and the registration time; nothing else (SR-07). |
| BR-11 | ADDED | The search text has at most 100 characters after leading and trailing whitespace is removed; an empty search text restricts nothing. A registration matches when the search text is part of its first name, last name, email, organization or study institution. Case is ignored. A letter without accent in the search text matches that letter with or without any accent (c matches c, č, ć; d matches d, đ; e matches e, é); a letter with accent matches only the same accented letter (č matches č and Č, not c or ć). |
| BR-12 | ADDED | Filters: registration type (all, external participant, student) and one filter per option category (workshop, event, meal, other), each either "any" or one configured option of that category, active or inactive. A registration is listed only when it matches the search text and every filter. An unknown registration type, an unknown option id or an option of another category is rejected (HTTP 400) with an error for that filter and no registration data. |
| BR-13 | ADDED | The result lists every matching registration on one page, newest registration first, with the number of matches, or "No registrations match" when there is none. Next to it every configured option of every category, active or inactive, is shown grouped by category with the number of matching registrations that selected it, zero included. After login all registrations are shown; the result is updated when the organizer presses Search or Enter in the search bar, and when a filter changes. |

## Design and quality

| ID | Change | Requirement | Checked by |
|---|---|---|---|
| SR-03 | MODIFIED | Every public `/api` endpoint (form configuration, registration, export, organizer search) is rate limited per client, and request bodies have a size limit. | acceptance and integration tests, phase 6 demonstration |
| SR-07 | MODIFIED | The export, the organizer email and the organizer search expose only registration data, never internal fields or other system data. | acceptance tests |
| SR-09 | ADDED | The organizer search API requires the organizer role with the same credentials as the export; missing or wrong credentials give 401 and no registration data. The frontend keeps the credentials only in the memory of the open page, never in browser storage or cookies, so a reload asks for them again. Registration text is displayed only as literal text, never interpreted as markup. | acceptance tests, Semgrep |
| NFR-01 | MODIFIED | Names and other text with Slovenian characters (č, š, ž) survive the form, database, JSON copy, emails, Excel export and organizer search page unchanged. | an end-to-end test with such input |

## Baseline items changed

| ID | Action | Reason |
|---|---|---|
| SR-03 | MODIFIED | The list of rate-limited endpoints gains the organizer search. |
| SR-07 | MODIFIED | The organizer search is a new place where registration data is exposed. |
| NFR-01 | MODIFIED | The organizer search page is a new place where the text must appear unchanged. |

## Must not change

AC-001-01..08, AC-002-01..07, AC-003-01..03, AC-004-01..04, AC-005-01..04, AC-006-01..03, AC-007-01..02, AC-008-01..04, E2E-01..05, SR-06, BR-08.

## Open questions

| ID | Question | Answer |
|---|---|---|
| OQ-07 | How do accents match in the search? | One direction: a letter without accent matches it with or without any accent, a letter with accent matches only itself; case is ignored (BR-11). |
| OQ-08 | Is a date-range filter offered? | No; the type and option-category filters with the per-option counts cover the need. |
| OQ-09 | Does the organizer page use its own credentials? | No; it uses the existing organizer credentials of the export (SR-09). |

The answers are the requester's decisions; the agent records no decision for them.
