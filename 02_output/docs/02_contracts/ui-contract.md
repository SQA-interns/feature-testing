# UI contract

> Written in: phase 2 · Source: `project/01_requirements/*`, `docs/02_specification.md` §12 · Agent: writes

The accessible names below are the interface the frontend acceptance tests and the end-to-end tests use. Text is English.

## Registration page (`/`)

| Element | Accessible name / text | Notes |
|---|---|---|
| Page heading | heading "Conference registration" | |
| Type choice | radio group "Registration type" with radios "External participant" and "Student" | nothing else of the form is shown before a choice |
| External fields | text inputs "First name", "Last name", "Email", "Organization / institution" | exactly these for `EXTERNAL` |
| Student fields | text inputs "First name", "Last name", "Email", "Study institution", "Study programme", "Student ID" | exactly these for `STUDENT` |
| Option groups | groups (fieldset/legend) "Workshops", "Events", "Meals", "Other activities", in this order | a group without options for the type is not shown |
| Option | checkbox named by the option's display name | unchecked initially |
| Consent | checkbox named by the consent text | never preselected (BR-05) |
| Captcha | test mode: text "reCAPTCHA test mode"; otherwise the Google widget | |
| Submit | button "Register" | disabled while a request is in flight |
| Field error | text in an element with id `<field>-error`, referenced by the input's `aria-describedby`; the input has `aria-invalid="true"` | option and consent errors sit under their group, id `options-error` / `consents-error` |
| Form error | element with role `alert` and id `form-error` | errors for `type`, `captchaToken`, `body`, and the general failure message |
| General failure | "Your registration could not be completed. Please try again later." | any non-201/non-400 answer or no answer (AC-004-04) |
| Confirmation | heading "Registration received", text "Thank you, <first name> <last name>.", a list of the selected option names (or "No options selected.") | replaces the form (AC-004-01) |

Client validation messages: "This field is required.", "Enter a valid email address.", "This consent is required.".

## Organizer page (`/organizer`)

| Element | Accessible name / text | Notes |
|---|---|---|
| Page heading | heading "Organizer registrations" | |
| Login | text input "Username", password input "Password", button "Sign in" | |
| Wrong credentials | role `alert`: "Invalid username or password." | no registration data shown |
| Search | search input "Search", button "Search" | applies on submit (Enter or button) |
| Type filter | select "Type": "All types", "External", "Student" | applies on change |
| Option filter | select "Option": "All options" then every configured option name | applies on change |
| Results | table "Registrations" with columns "Registered at", "Type", "First name", "Last name", "Email", "Organization / study institution", "Options" | one row per item; all values rendered as text (SR-09) |
| Total | text "<n> registrations found" | |
| Paging | buttons "Previous", "Next"; text "Page <p> of <m>" | Previous disabled on page 1, Next on the last page |
| Load failure | role `alert`: "Registrations could not be loaded." | |
| Sign out | button "Sign out" | forgets the credentials |
