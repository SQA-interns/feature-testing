# Feature package: `group` (group registration)

> Owner: Product owner · Status: draft for review · Shared by greenfield and brownfield runs, word for word

This file is the complete and only specification of the feature. Both run types use exactly this text:

- **Greenfield:** before the run, every block below is applied to the named file in `01_input/01_project/` (added or replaced exactly as written), then the inputs are committed and the run starts as a normal run.
- **Brownfield:** the same blocks are the change request given to the agent on top of a finished normal run (procedure: `brownfield-instructions.md`).

IDs continue the project's numbering: `US-009`, `AC-009-nn`, `BR-09` to `BR-11`, `AR-08`, `E2E-06`, `DoD-P06`; the endpoint and export columns are added to the fixed API contract (`api-contract.md`, AR-09). Every feature package of this experiment uses the next free IDs of the base project, because a run gets exactly one feature: the inputs of each run then have no gaps and do not hint at other features. Nothing else in the project changes.

---

## 1. `01_requirements/user-stories.md`: add at the end

```markdown
### US-009 Group registration

As a contact person of an organization, I want to register several colleagues as external participants in one submission, so that I do not have to fill the form once per person (BR-09 to BR-11).
In scope: a group form with the contact person, the shared organization and 2 to 10 members, each member with their own option selection; storage, emails and export for every member. Out of scope: student groups or mixed groups, adding or removing members after submission, payment or invoicing for the group.
```

## 2. `01_requirements/business-rules.md`

Add three rows to the Rules table:

```markdown
| BR-09 | A group registration has one contact person (first name, last name, email), one organization shared by all members, and 2 to 10 members. Each member has first name, last name and email and selects their own options; every member is an external participant (BR-01). The contact person gives each mandatory consent for the whole group (BR-05). The contact person is not a member unless they are also listed as one. |
| BR-10 | A group is accepted as a whole or not at all: if the group or any member is invalid, nothing is stored and no email is sent. Two members may not share an email address (compared without regard to upper or lower case). |
| BR-11 | Every member of an accepted group is an accepted registration of type external participant (BR-07): its own database row and its own JSON copy, both naming the group and its contact person. |
```

Add a row to the Data table:

```markdown
| Group | contact person (first name, last name, email), organization / institution, 2 to 10 members (first name, last name, email, selected options each) |
```

Add to the Glossary:

```markdown
- Group: several external participants of one organization, registered together by a contact person.
- Contact person: the person who submits a group registration; receives the group confirmation.
```

## 3. `01_requirements/scope.md`

In "In scope", add the line:

```markdown
- Group registration of several external participants by a contact person (US-009)
```

## 4. `01_requirements/acceptance-criteria.md`

Insert after the US-008 section and before "Traceability":

```markdown
## US-009 Group registration

*Valid group submission*: contact person (first name, last name, email), organization and 2 to 10 members, each with first name, last name, a valid email unique within the group and zero or more distinct active option ids available to external participants; every mandatory consent given; a reCAPTCHA token the verifier accepts.

| ID | Criterion | Refs |
|---|---|---|
| AC-009-01 | Given active options are configured, when a valid group submission with three members is sent, then it is accepted (HTTP 201) and the response contains the group id and one registration id per member. | BR-09, BR-11 |
| AC-009-02 | Given a valid group submission, when it is accepted, then the database holds one registration of type external per member with that member's names, email and options, the group's organization, each consent with the time it was given and the time of registration, each linked to the group, and the group with its contact person. | BR-09, BR-11, BR-07 |
| AC-009-03 | Given a valid group submission, when it is accepted, then for each member a JSON copy named after the member's registration id exists in the JSON copy directory and contains the member's registration and the group id and contact person. | BR-11, BR-07 |
| AC-009-04 | Given a group submission, when it has fewer than 2 or more than 10 members, then it is rejected (HTTP 400) with an error for the members and nothing is stored. | BR-09, BR-10 |
| AC-009-05 | Given a group submission, when any one member has a missing, empty or only-whitespace required field, an invalid email, or an unknown, inactive, duplicate or not-available option, then it is rejected (HTTP 400) with an error naming that member and field, and no member is stored. | BR-02, BR-03, BR-04, BR-10 |
| AC-009-06 | Given a group submission, when two members have the same email address in any upper or lower case, then it is rejected (HTTP 400) with an error for the members and nothing is stored. | BR-10 |
| AC-009-07 | Given a group submission, when a contact person field or the organization is missing, empty or only whitespace, or the contact person's email is invalid, then it is rejected (HTTP 400) with an error for that field and nothing is stored. | BR-02, BR-03, BR-09 |
| AC-009-08 | Given a group submission, when a mandatory consent is not given or the reCAPTCHA token is missing or rejected, then it is rejected (HTTP 400) and nothing is stored. | BR-05, SR-01 |
| AC-009-09 | Given the JSON copy of any member cannot be written, when a valid group submission is sent, then the API answers with an error (not 201) and no member and no group is left in the database. | BR-10, AR-05, AR-08 |
| AC-009-10 | Given a valid group submission, when it is accepted, then each member receives one confirmation email naming the conference, the member and their options, and the contact person receives one group confirmation listing every member with their options. | BR-11, US-006 |
| AC-009-11 | Given organizer addresses are configured, when a group is accepted, then each organizer address receives one notification for the group containing every member's data, with one JSON attachment per member whose content equals that member's stored JSON copy. | US-007, BR-11, SR-07 |
| AC-009-12 | Given stored individual and group registrations, when an organizer exports, then the workbook has one row per registration, and each group member's row also shows the group id and the contact person's name and email; individual registrations leave these cells empty. | BR-08, BR-11 |
| AC-009-13 | Given the registration page, when the participant chooses "group of external participants", then the page shows the contact person fields, the organization once, two member rows that can be added up to ten and removed down to two, each with its own active options grouped by category, and every consent checkbox unchecked; after acceptance it shows a confirmation listing every member. | BR-09, BR-05, BR-06 |
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
| E2E-06 | Contact person registers a group | Open the page, choose "group of external participants", fill the contact person and organization, add a third member, give each member different options, submit. The page shows a confirmation listing all three members. The database holds three external registrations linked to one group, three JSON copies exist, Mailpit holds three member confirmations, one group confirmation to the contact person and one organizer notification with three JSON attachments, and the organizer export shows the three rows with the group id and contact person. | AC-009-01, AC-009-02, AC-009-03, AC-009-10, AC-009-11, AC-009-12, AC-009-13, DoD-P06 |
```

## 6. `02_design/security-requirements.md`

In "Personal data", add a row:

```markdown
| Contact person name and email | confirm a group registration and contact the group | `OQ-06` | contact person (group confirmation), organizers (email, export) |
```

In "Project requirements", replace SR-03 with:

```markdown
| SR-03 | Every public `/api` endpoint (form configuration, registration, group registration, export) is rate limited per client, and request bodies have a size limit that admits a group of 10 members. |
```

## 7. `02_design/architecture.md`

In "Constraints", add:

```markdown
| AR-08 | A group is stored as one unit: a success response is sent only after the database rows and JSON copies of every member are written; if any of them fails, none of the group remains (BR-10, AR-05). |
```

In "Interfaces", replace the row `| Options, registration, export | frontend or organizer and backend | REST (OpenAPI) |` with:

```markdown
| Options, registration, group registration, export | frontend or organizer and backend | REST (OpenAPI) |
```

## 8. `02_design/quality-requirements.md`: Additional done criteria

```markdown
| DoD-P06 | Against the running local stack: a group of three external participants registers; every member is in PostgreSQL with a JSON copy, the member and contact confirmations and the organizer notification with three attachments arrive in Mailpit, and the export shows the group. | runtime demonstration log, Mailpit API output |
```

## 9. `02_design/api-contract.md`

Replace the export's column line (the line starting `` `id`, `type`, ``) with:

```markdown
`id`, `type`, `firstName`, `lastName`, `email`, `organization`, `studyInstitution`, `studyProgramme`, `studentId`, `options`, `consents`, `registeredAt`, `groupId`, `contactName`, `contactEmail`
```

Add at the end:

```markdown
### `POST /api/group-registrations` (public)

Request:

    {
      "contact": {"firstName": "Maja", "lastName": "Kranjc", "email": "maja@example.org"},
      "organization": "Example d.o.o.",
      "members": [
        {"firstName": "Ana", "lastName": "Novak", "email": "ana@example.org", "options": ["ws-ai"]},
        {"firstName": "Luka", "lastName": "Zupan", "email": "luka@example.org", "options": []}
      ],
      "consents": ["privacy"],
      "captchaToken": "test-pass"
    }

Responses: 201 `{"groupId": "<group id>", "registrationIds": ["<id of member 1>", "<id of member 2>"]}`, the ids in the order of `members`; 400 with field errors, where a member's field is named `members[<index from 0>].<field>` (for example `members[1].email`), the member list as a whole `members`, the contact person's fields `contact.<field>`; any other error status when the group could not be stored (AC-009-09).

In the export, `contactName` is the contact person's first and last name separated by a space; `groupId`, `contactName` and `contactEmail` are empty for individual registrations.
```

---

## Not changed by this feature

`tech-stack.md` (no new dependency is needed), `environments.md`, `pre-approved.md`, `secrets.env.example`, the quality gate, the mutation scope, all general files and `03_statistics/`.
