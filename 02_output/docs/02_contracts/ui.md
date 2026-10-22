# UI contract

> Written in: phase 2 · Source: `docs/02_specification.md` 7, AC-001-08, AC-002-07, AC-004-*, AC-009-12, AC-009-13 · Agent: writes

Accessible names below are what tests use (`getByRole`, `getByLabel`).

## Registration page `/`

| Element | Accessible name / text |
|---|---|
| Type choice (radio group "Registration type") | radios `External participant`, `Student`; none selected at first, no fields shown |
| External fields | text boxes `First name`, `Last name`, `Email`, `Organization / institution` |
| Student fields | text boxes `First name`, `Last name`, `Email`, `Study institution`, `Study programme`, `Student ID` |
| Option groups | one group (`fieldset` with legend) per non-empty category in this order: `Workshops`, `Events`, `Meals`, `Other activities`; one checkbox per active option available to the chosen type, labelled with the option name, unchecked |
| Consents | one checkbox per consent labelled with its text, unchecked; mandatory ones marked "(required)" |
| Captcha | reCAPTCHA widget, or the text `reCAPTCHA test mode` |
| Submit | button `Register` |
| Field error | text next to its field, linked to the field by `aria-describedby`; options error next to the option groups, consent error next to the consents, captcha error next to the captcha |
| General error | `role="alert"`: `Your registration could not be sent. Please try again later.` |
| Confirmation | heading `Registration received`, text `Thank you, <first name> <last name>.`, a list of the selected options' names (or `No options selected`); the form is removed |

Client-side messages: `This field is required.`, `Enter a valid email address.`, `This consent is required.`. Backend messages are shown as returned.

## Organizer page `/organizer`

| Element | Accessible name / text |
|---|---|
| Login | text box `Username`, password box `Password`, button `Sign in` |
| Login error | `role="alert"`: `Wrong username or password.`; no registration data shown |
| Search | search box `Search`, select `Type` (`All types`, `External participant`, `Student`), select `Option` (`All options` plus every configured option name), button `Search` |
| Result | text `<total> registrations`, a table with columns `Registered`, `Type`, `First name`, `Last name`, `Email`, `Organization / institution`, `Options`; one row per item |
| Paging | buttons `Previous`, `Next` (disabled at the ends), text `Page <n> of <m>` |
| Empty result | `No registrations found.` |

Every value is rendered as text, never as HTML (SR-09).
