# Interview log: CR-001

> Written in: change interview · Source: the conversation · Read in: post-run report only, never during a change run · Agent: appends only

One row per question, appended after each round. Source: `human` (decided by the requester), `default` (the recommended answer was accepted), `looked up` (answered from the baseline or the code, not asked).

| # | Question | Recommended | Answer | Source | Affects |
|---|---|---|---|---|---|
| 0 | Is the baseline complete and unchanged? | yes | Clean tree at 4d664d2, phase 7 complete, final run 0 failed, gate met, no open Critical or High, D-01..D-18 resolved or pending review, acceptance manifest matches; input manifest differs only in 03_statistics/tools (build-report.mjs, metrics-catalog.json, stat.mjs), changed by the requester in a82fcea and accepted by them; run label changed to tjan/single-agent/sdd/v002/run-2-search (committed as 4d664d2) | looked up | Baseline |
| 1 | Is this an add, a change or a remove? | add | add | default | Type |
| 2 | Does the baseline have an organizer or admin UI? | n/a | No: scope.md lists "an administration UI or dashboard" as out of scope; organizers have only the export endpoint (US-008, BR-08) with HTTP Basic | looked up | Intent, US-009 |
| 3 | Where does the search live? | a new organizer page in the frontend listing registrations, with search and filters, only with the organizer credentials | new organizer page | default | US-009 |
| 4 | What does each row in the result list show? | the same data as the export (type, fields, options, consents, registration time), nothing internal (SR-07) | same as export | default | US-009, BR-10 |
| 5 | Can the filtered result be exported? | no; the export stays the full list (AC-008-01 unchanged) | no | default | Intent, Must not change |
| 6 | What does the search bar match? | part of first name, last name, email, organization or study institution; case-insensitive; surrounding spaces ignored; empty search shows all | as recommended, and it must work for č, š, ž | human | BR-11 |
| 7 | Do Slovenian characters have to match exactly? | accent-sensitive | accent-insensitive | human | BR-11 |
| 8 | Which filters are offered? | registration type and selected option | registration type, workshop and meal; requester asked what else could help (e.g. the lunch crew counting meals) | human | BR-12 |
| 9 | How is the result list presented? | all matches on one page, newest first, number of matches, "No registrations match" when empty | as recommended | default | BR-13 |
| 10 | How are accents and case matched? | c/č/ć, s/š, z/ž, d/đ the same in both directions, case-insensitive | one direction: a letter without accent matches it with or without accent (c finds c, č, ć; d finds d, đ), a letter with accent matches only itself (č does not find c); case-insensitive; all accented letters work like this | human | BR-11, OQ-07 |
| 11 | Which option categories get a filter? | one per category (workshop, event, meal, other), one option each, AND; no date range | recommended, unless a date range is useful (asked back) | human | BR-12 |
| 12 | Does the page show per-option counts for the matching registrations? | yes, grouped by category, following search and filters | yes | default | BR-13 |
| 13 | Which options appear in a category filter? | every configured option of the category, active or inactive | active and inactive | default | BR-12 |
| 14 | Is a date-range filter useful? | no: it only answers "who registered since day X"; the counts serve the lunch crew | no | default | BR-12, Intent, OQ-08 |
| 15 | Does the one-direction accent rule hold for every accented letter? | yes, every accented letter | every accented letter | default | BR-11 |
| 16 | How does the organizer get access? | existing organizer credentials as for the export; 401 and no data when missing or wrong; SR-06 and SR-03 apply; credentials only in memory of the tab | as recommended | default | SR-09, Security impact, OQ-09 |
| 17 | What is the limit on the search text? | 100 characters; longer is rejected (HTTP 400, error for the search field) shown next to the search bar | 100 characters | default | BR-11 |
| 18 | What is out of scope? | editing or deleting registrations, column sorting, filtered export, date-range filter, several options per filter | as recommended | default | Intent, US-009 |
| 19 | How does the organizer reach the page? | own address /organizer, not linked from the registration page; login form first | as recommended | default | BR-09, AC-009-01 |
| 20 | When are results updated? | on Search or Enter, and on every filter change; all registrations after login | as recommended | default | BR-13 |
| 21 | Do the counts list options with no match? | yes: every configured option of each category, zeros shown | yes | default | BR-13 |
| 22 | How is an unknown filter value treated? | rejected (HTTP 400) with an error for that filter, no data | as recommended | default | BR-12 |
| 23 | Which baseline items stop being true or complete? | n/a | SR-03 lists the rate-limited endpoints, SR-07 the places exposing registration data, NFR-01 the places where text must survive; each gains the organizer search. No AC or E2E becomes false; scope.md's "no administration UI" exclusion is lifted for this page (not an id) | looked up | SR-03, SR-07, NFR-01 |
| 24 | Are the drafted criteria AC-009-01..30 the definition of done, in one CR? | yes, one CR (one page) | yes | default | AC-009-01..30 |
| 25 | Is registration text shown on the page only as literal text, never run as markup or script? | yes | yes | default | SR-09, AC-009-29, Security impact |
| 26 | Is E2E-06 the one new end-to-end scenario? | yes | yes | default | E2E-06 |
| 27 | Which baseline behaviour must not change? | AC-001-01..AC-008-04, E2E-01..05, SR-06, BR-08, the full export | yes | default | Must not change |
