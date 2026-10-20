 Kept until the operator deletes them; no automatic deletion. The release notes tell the operator to delete the database rows and JSON copies no later than 12 months after the conference. |Allowed; each registration is stored separately and the export shows both. |No limit per category; each option at most once per registration, duplicates are rejected. |The registration stays accepted (HTTP 201, confirmation shown). Emails are sent only after the database row and the JSON copy exist; an email failure is logged with the registration id only, no personal data. |Consents are part of the conference configuration (id, wording, mandatory). The shipped configuration has one mandatory consent `privacy`: "I agree that my personal data is processed for organizing the conference and that it is shared with the conference organizers." |Every option carries a configured list of registration types it is available to (default: both); the backend rejects an option that is not available to the registration type. |# Scope

> Owner: Product owner · Read in: phases 1, 6 · Agent: read-only

## In scope

- Registration forms for external participants and students (US-001, US-002)
- Configurable conference options (US-003)
- In-application confirmation, participant confirmation email, organizer notification email with the JSON attachment (US-004, US-006, US-007)
- Storage in the database plus a raw JSON copy (US-005)
- Excel export for organizers (US-008)
- Anti-automation protection of the registration form

## Out of scope

- Participant accounts and login
- Payment processing
- Editing or cancelling a submitted registration
- Verifying student status externally
- An administration UI or dashboard, including option configuration
- Integration with an identity provider

## Priorities

1. A registration is never lost: storage (BR-07) comes before any notification.
2. Security and privacy (`project/02_design/security-requirements.md`) come before convenience.
3. The participant flow (US-001 to US-006) comes before organizer features (US-007, US-008).

## Open questions

| ID | Question | Owner | Answer |
|---|---|---|---|
| OQ-01 | Are all options available to students, or are some only for external participants? | Product owner | Every option carries a configured list of registration types it is available to (default: both); the backend rejects an option that is not available to the registration type. |
| OQ-02 | Which mandatory consents exist, and what is their wording? | Product owner | Consents are part of the conference configuration (id, wording, mandatory). The shipped configuration has one mandatory consent `privacy`: "I agree that my personal data is processed for organizing the conference and that it is shared with the conference organizers." |
| OQ-03 | What happens when storage succeeds but an email fails? | Product owner | The registration stays accepted (HTTP 201, confirmation shown). Emails are sent only after the database row and the JSON copy exist; an email failure is logged with the registration id only, no personal data. |
| OQ-04 | How many options may a participant select per category? | Product owner | No limit per category; each option at most once per registration, duplicates are rejected. |
| OQ-05 | Is a second registration with the same email allowed? | Product owner | Allowed; each registration is stored separately and the export shows both. |
| OQ-06 | How long are registrations and JSON copies kept? | Product owner | Kept until the operator deletes them; no automatic deletion. The release notes tell the operator to delete the database rows and JSON copies no later than 12 months after the conference. |

The answers are the product owner's decisions and hold for every run of this project; the agent records no decision for them. A new question that arises later becomes a decision record as usual.
