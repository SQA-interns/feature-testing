# Release notes: CR-001 Organizer registration search

> Written in: phase 7 of the change run · Agent: writes

Version 0.2.0 of the conference registration: version 0.1.0 (`02_output/docs/release-notes.md`, which still holds) plus CR-001, from run `tjan/single-agent/sdd/v002/run-2-search` (2026-10-29).

## Delivered

- **Organizer search page (US-009):** at `/organizer`, not linked from the registration form. It asks for the existing organizer credentials (the same as for the export) and keeps them only in the open tab, so a reload asks again. After login it lists every registration, newest first, with the number of matches.
- **Search bar (BR-11):** finds text in the first name, last name, email, organization or study institution. Case is ignored; a letter without an accent also finds its accented forms (`kos` finds Koš, Kos and Kós; `duric` finds Đurić), while a letter with an accent finds only itself (`koš` finds only Koš). At most 100 characters. It searches on Search or Enter.
- **Filters (BR-12):** registration type, and one option per category (workshop, event, meal, other activity), active and inactive options. A filter change updates the result at once, with the text in the search bar (D-25).
- **Per-option counts (BR-13):** for every configured option, grouped by category, how many of the listed registrations selected it, zeros included (for example how many lunches to prepare).
- **API:** `GET /api/organizer/registrations` (organizer only, HTTPS-only outside localhost, own rate limit of 30 requests per minute per client, D-23). Each row carries only the data of the export row (SR-07); the page does not show the registration id (D-24).

Quality at release: 462 automated tests pass (backend 335, frontend 121, end-to-end 6). Line coverage 99.5 % (backend) and 99.6 % (frontend); mutation scores 95.1 % (PIT) and 91.3 % (Stryker). No Critical or High finding is open (`06_verification-report.md`).

## Data migrations

None. The search reads the existing tables; no schema change.

## Configuration changes

| Variable | New | Default | Meaning |
|---|---|---|---|
| `RATE_LIMIT_ORGANIZER_SEARCH_PER_MINUTE` | yes | 30 | Organizer search calls per client and minute, failed logins included (D-23) |

`ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` and `ORGANIZER_HTTPS_ONLY` now also apply to the organizer search. No secret was added.

## Known limitations

- **HTTP Basic, as for the export** (F-01, D-18): use the organizer page only over HTTPS in production. A failed login on the page answers 401 without a browser login dialog (D-26).
- **The search filters in memory** after reading every registration; meant for one conference's numbers of registrations.
- **One option per category filter,** no date range, no sorting, no export of the filtered result, no editing (out of scope in CR-001).
- **The rate limit counter** lives in one backend instance, as for the other endpoints.
- **`03_statistics/` holds the organizer email address** in the committed transcript of the previous run (F-07), accepted by the human (D-30).

## Must be tested manually by a human

1. **Organizer page over HTTPS in production:** open `https://<host>/organizer` behind the production nginx. The login form appears, valid credentials list the registrations, and wrong credentials show "Wrong username or password." with no browser login dialog. Over plain HTTP from another host the search is refused.
2. **Real data and accents:** with real registrations, search a few names with and without č, š, ž, ć, đ, and check that the results follow the rule above and that the names are shown unchanged.
3. **Counts for the lunch crew:** set the meal filter or the type filter and check that the option counts match the number of listed registrations that chose each option.
4. **Browsers and phone:** use the page in the browsers the organizers use, including a narrow screen: the filters, the list and the counts stay readable.
5. **Reload and other tabs:** after login, reload the page and open it in a second tab; both ask for the credentials again, and nothing about the login stays in the browser's storage.
6. **Rate limit in practice:** click through several filters quickly; normal use stays under 30 calls per minute. If a 429 appears, the page shows the general error and recovers after a minute.

## Decisions

D-19..D-22 and D-27..D-30 are resolved; D-23..D-26 were approved by the human (D-27). Pending review: D-28 (the signed lint rejects `cr/docs/`, a template tooling defect). See `decisions-log.md`.
