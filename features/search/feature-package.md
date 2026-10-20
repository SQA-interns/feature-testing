# Feature package: `search` (organizer search and filter)

> Owner: Product owner · Status: approved 2026-10-20 · Shared by greenfield and brownfield runs, word for word

This file is the complete and only specification of the feature. Both run types use exactly this text:

- **Greenfield:** before the run, every block below is applied to the named file in `01_input/01_project/` (added or replaced exactly as written), then the inputs are committed and the run starts as a normal run.
- **Brownfield:** the same blocks are the change request given to the agent on top of a finished normal run (procedure: `brownfield-instructions.md`, to be written).

IDs continue the project's numbering: `US-009`, `AC-009-nn`, `BR-09`, `BR-10`, `SR-09`, `E2E-06`, `DoD-P06`; the endpoint is added to the fixed API contract (`api-contract.md`, AR-09). Nothing else in the project changes.

Every feature package of this experiment uses the next free IDs of the base project, because a run gets exactly one feature: the inputs of each run then have no gaps and do not hint at other features.

---

## 1. `01_requirements/user-stories.md`: add at the end

```markdown
### US-009 Organizer registration search

As an organizer, I want to search and filter the registrations in the application, so that I can find a participant or a group of participants without exporting every registration (BR-09, BR-10).
In scope: an organizer page and API that list registrations with a text search and filters by registration type and selected option, paged. Out of scope: editing or deleting registrations from the list, exporting the filtered list, saved searches.
```

## 2. `01_requirements/business-rules.md`: add two rows to the Rules table

```markdown
| BR-09 | Only organizers can search and list registrations; the same organizer access as the export (BR-08) applies. |
| BR-10 | The search text matches, without regard to upper or lower case, any part of the first name, last name, email, organization or study institution; leading and trailing whitespace is ignored and an empty text matches every registration. Filters combine: a registration is listed only if it matches the text, the type filter and the option filter. Results are listed newest first, 20 per page, with the total number of matches. |
```

## 3. `01_requirements/scope.md`

In "In scope", add the line:

```markdown
- Organizer search and filter page for registrations (US-009)
```

In "Out of scope", replace the line `- An administration UI or dashboard, including option configuration` with:

```markdown
- An administration UI or dashboard, including option configuration; the organizer search page of US-009 is the only organizer page
```

## 4. `01_requirements/acceptance-criteria.md`

Insert after the US-008 section and before "Traceability":

```markdown
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
```

In the Traceability table, add the row and change the total:

```markdown
| US-009 | AC-009-01 … AC-009-13 |
```

```markdown
Total: 48 criteria.
```

## 5. `01_requirements/e2e-scenarios.md`: add a row to the table

```markdown
| E2E-06 | Organizer searches registrations | Register one external and one student participant with known, distinct last names and options. Open the organizer page, enter wrong credentials: an error is shown and no data. Enter valid credentials: both registrations are listed. Search for the student's last name: only the student is listed. Clear the text and filter by type "external": only the external participant is listed. | AC-009-01, AC-009-02, AC-009-03, AC-009-12, DoD-P06 |
```

## 6. `02_design/security-requirements.md`

In "Authentication and authorization", replace the line starting `- One organizer role may export registrations (BR-08).` with:

```markdown
- One organizer role may export and search registrations (BR-08, BR-09). The mechanism is decided in phase 2, using the organizer credentials from `secrets.env.example`; identity providers are out of scope.
```

In "Personal data", the column "Shown/exported to" stays; organizers now also see these items on the search page (no table change needed).

In "Project requirements", replace SR-03 and add SR-09:

```markdown
| SR-03 | Every public `/api` endpoint (form configuration, registration, export, search) is rate limited per client, and request bodies have a size limit. |
```

```markdown
| SR-09 | Participant data shown on the organizer search page is encoded for its context; markup in stored data is never executed. |
```

## 7. `02_design/architecture.md`: Interfaces table

Replace the row `| Options, registration, export | frontend or organizer and backend | REST (OpenAPI) |` with:

```markdown
| Options, registration, export, search | frontend or organizer and backend | REST (OpenAPI) |
| Organizer search page | organizer and frontend | UI |
```

## 8. `02_design/quality-requirements.md`: Additional done criteria

```markdown
| DoD-P06 | Against the running local stack: the organizer finds a registration by last name and by type on the search page; the same search without organizer access is refused. | runtime demonstration log |
```

## 9. `02_design/api-contract.md`: add at the end

```markdown
### `GET /api/registrations` (organizer)

Query parameters, all optional: `text` (search text, at most 100 characters), `type` (`EXTERNAL` or `STUDENT`), `option` (an option id), `page` (1 or more, default 1).

Response 200:

    {
      "total": 25,
      "page": 1,
      "items": [
        {"id": "…", "type": "EXTERNAL", "firstName": "Ana", "lastName": "Novak", "email": "ana@example.org",
         "organization": "Example d.o.o.", "studyInstitution": null, "options": ["ws-ai"],
         "registeredAt": "2026-10-20T10:15:00Z"}
      ]
    }

`items` holds at most 20 registrations, newest first (BR-10). Response 400 with an error for the invalid parameter (`text`, `type`, `option` or `page`); 401 without valid organizer credentials.
```

---

## Not changed by this feature

`tech-stack.md` (no new dependency is needed), `environments.md`, `pre-approved.md`, `secrets.env.example`, the quality gate, the mutation scope, all general files and `03_statistics/`.
