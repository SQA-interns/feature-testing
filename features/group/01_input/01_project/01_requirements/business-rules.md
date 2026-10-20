# Business rules

> Owner: Product owner · Read in: phases 1, 6 · Agent: read-only

## Rules

| ID | Rule |
|---|---|
| BR-01 | There are two registration types: external participant and student. Each has the fixed fields listed under Data; configuration never changes them. |
| BR-02 | Required fields are never empty; leading and trailing whitespace is not significant. |
| BR-03 | Email has a valid email format. Text accepts Unicode, including Slovenian characters (č, š, ž). |
| BR-04 | Options are grouped as workshops, events, meals and other activities. Only active options can be selected; unknown or inactive options are rejected. |
| BR-05 | Mandatory consents must be given and are never preselected. |
| BR-06 | The in-application confirmation is shown only after the registration was accepted. |
| BR-07 | An accepted registration is stored in the database and as a raw JSON copy on persistent storage. |
| BR-08 | Only organizers can obtain the registration list; it is not reachable without organizer access. |
| BR-09 | A group registration has one contact person (first name, last name, email), one organization shared by all members, and 2 to 10 members. Each member has first name, last name and email and selects their own options; every member is an external participant (BR-01). The contact person gives each mandatory consent for the whole group (BR-05). The contact person is not a member unless they are also listed as one. |
| BR-10 | A group is accepted as a whole or not at all: if the group or any member is invalid, nothing is stored and no email is sent. Two members may not share an email address (compared without regard to upper or lower case). |
| BR-11 | Every member of an accepted group is an accepted registration of type external participant (BR-07): its own database row and its own JSON copy, both naming the group and its contact person. |

## Data

| Form or entity | Fields (all required) |
|---|---|
| External participant | first name, last name, email, organization / institution |
| Student | first name, last name, email, study institution, study programme, student ID |
| Conference option | stable identifier, display name, category (workshop, event, meal, other), active or inactive |
| Consent | which consents exist and their wording: `OQ-02` |
| Group | contact person (first name, last name, email), organization / institution, 2 to 10 members (first name, last name, email, selected options each) |

## Glossary

- Participant: a person who registers, external or student.
- Organizer: conference staff who receive notifications and export registrations.
- Option: a selectable workshop, event, meal or other activity.
- Active option: an option currently offered for selection.
- Raw JSON copy: the registration exactly as accepted, stored as a file for backup.
- Group: several external participants of one organization, registered together by a contact person.
- Contact person: the person who submits a group registration; receives the group confirmation.
