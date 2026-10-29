# Organizer search UI contract

> Written in: phase 2 of CR-001 · Source: US-009, BR-09..13, SR-09, NFR-01, D-24..D-26 · Agent: writes

Interface between the organizer and the frontend page at `/organizer`. Text is English (D-13). Element names below are the accessible names that tests use (`getByRole`, `getByLabel`), so they are part of the contract. The registration page has no link to this page (BR-09, AC-009-02).

## Page states

| State | Shown |
|---|---|
| Login (AC-009-01) | Heading `Organizer registrations`; form `Organizer login` with `Username` (text), `Password` (password) and button `Log in`. No registration data and no API call before `Log in` |
| Logging in | `Log in` disabled |
| Wrong credentials (AC-009-04) | The login form stays; alert `Wrong username or password.`; no registration data |
| Other failure | Alert `The registrations could not be loaded. Please try again later.` (5xx, 429, 403, network error) |
| Result (AC-009-06) | Search form, filters, `Matches: <n>`, the list `Registrations` (or `No registrations match`, AC-009-23) and the region `Option counts` |

The credentials live only in the page's memory (React state). Nothing is written to `localStorage`, `sessionStorage` or a cookie, so a reload shows the login form again (SR-09, AC-009-30). A 401 on a later search returns to the login form with the alert above.

## Search and filters (BR-11, BR-12, BR-13)

- Search form (`role="search"`, name `Search registrations`): field `Search` and button `Search`. Submitting the form (button or Enter) searches with the current text and filters (AC-009-27).
- Selects, each changing the result at once with the text currently in the field (AC-009-26, D-25):

| Label | Options |
|---|---|
| `Registration type` | `All`, `External participant`, `Student` |
| `Workshop` | `Any`, then every configured WORKSHOP option, active and inactive, by name (AC-009-21) |
| `Event` | `Any`, then every EVENT option |
| `Meal` | `Any`, then every MEAL option |
| `Other activity` | `Any`, then every OTHER option |

  The option lists come from `optionCounts` of the first result.
- A search text with more than 100 characters after trimming (code points) shows `The search text can have at most 100 characters.` under the field, linked by `aria-describedby`, with `aria-invalid="true"`; no request is sent and no result list is shown (AC-009-17). A 400 from the API for a field shows the same place for `search`, and the general alert for a filter.

## Result

- `Matches: <count>`; when the count is 0 also `No registrations match` and no list (AC-009-23).
- List `Registrations` (`ol`), newest first, one item per registration with a description list (`dl`) of label and value (BR-10, AC-009-07). The id is not shown (D-24):

| Label | Value |
|---|---|
| `Type` | `External participant` or `Student` |
| `Registered at` | `registeredAt` as received (UTC, ISO 8601) |
| `First name`, `Last name`, `Email` | the values |
| `Organization / institution` | external only |
| `Study institution`, `Study programme`, `Student ID` | student only |
| `Workshops`, `Events`, `Meals`, `Other activities` | option names joined with `; `; a category without a selected option is left out |
| `Consents` | `<id> (<givenAt>)` joined with `; ` |

- Region `Option counts` (`section`, `aria-label`): per category a heading `Workshops`, `Events`, `Meals`, `Other activities` and a list of `<option name>: <count>` for every configured option, zero included (AC-009-24).
- Every value is rendered as React text, never as markup; no `dangerouslySetInnerHTML` (SR-09, AC-009-29). Text is shown unchanged (NFR-01, AC-009-28).
