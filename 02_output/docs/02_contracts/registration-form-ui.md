# Registration form UI contract

> Written in: phase 2 · Source: US-001, US-002, US-004, BR-01..06, NFR-03, D-13 · Agent: writes

Interface between the participant and the frontend. Text is English (D-13). Element names below are the accessible names that tests use (`getByRole`, `getByLabel`), so they are part of the contract.

## Page states

| State | Shown |
|---|---|
| Loading | `Loading…` while `GET /api/form-config` runs |
| Configuration error | Alert `The registration form is not available right now. Please try again later.` |
| Choose type | Heading `Conference registration` with the conference name; radio group `Registration type` with `External participant` and `Student`, none selected; no form fields |
| Form | Fields of the chosen type, options, consents, captcha, button `Register` |
| Submitting | `Register` disabled |
| Confirmation (AC-004-01) | Heading `Registration received`; text `Thank you, <first name> <last name>.`; list `Selected options` with each option name, or `No options selected.`; the form is not rendered |

## Fields (BR-01)

| Label | Type | External | Student | Max length (D-14) |
|---|---|---|---|---|
| `First name` | text | yes | yes | 200 |
| `Last name` | text | yes | yes | 200 |
| `Email` | email | yes | yes | 254 |
| `Organization / institution` | text | yes | no | 200 |
| `Study institution` | text | no | yes | 200 |
| `Study programme` | text | no | yes | 200 |
| `Student ID` | text | no | yes | 50 |

Switching type keeps the shared fields (names, email) and clears the selected options that the new type cannot use.

## Options (BR-04, OQ-01)

One group (`fieldset`) per category, in this order and with these legends: `Workshops`, `Events`, `Meals`, `Other activities`. Each active option available to the chosen type is a checkbox labelled with its display name, unchecked at first. A category with no option for the type is not rendered.

## Consents (BR-05, OQ-02)

One checkbox per configured consent, labelled with its text, never preselected (AC-001-08, AC-002-07). A mandatory consent is marked `(required)`.

## Captcha (SR-01, SR-02)

- Mode `RECAPTCHA`: the Google reCAPTCHA v2 checkbox widget, rendered with the site key from the form configuration.
- Mode `TEST`: a checkbox labelled `I am not a robot (test mode)`; checked sends token `test-pass`.

## Validation and errors (NFR-03, AC-004-02..04)

- Before sending, the frontend checks every required field of the type (after trimming), the email format, the lengths, the mandatory consents and that a captcha token exists. On any error it sends no request (AC-004-03).
- Each field error is shown under its field as text linked with `aria-describedby`, and the field gets `aria-invalid="true"`. Option errors are shown under the options, consent errors under that consent, captcha errors under the captcha.
- Backend field errors (400) are mapped by `field` to the same places; the entered values stay (AC-004-02).
- Any other failure (5xx, 413, 429, network error, unreadable response) shows the alert `Your registration could not be sent. Please try again later.` with no technical detail and keeps the values (AC-004-04, SB-07).

| Code | Message shown |
|---|---|
| REQUIRED | `This field is required.` |
| INVALID_FORMAT | `Enter a valid email address.` |
| TOO_LONG | `This value is too long.` |
| UNKNOWN_OPTION, INACTIVE_OPTION, DUPLICATE_OPTION, OPTION_NOT_AVAILABLE, NOT_ALLOWED | `One or more selected options are not available.` (options) / `This field is not allowed for this registration type.` (fields) |
| CONSENT_REQUIRED | `This consent is required.` |
| UNKNOWN_CONSENT | `This consent is not available.` |
| CAPTCHA_FAILED | `Please confirm that you are not a robot.` |
