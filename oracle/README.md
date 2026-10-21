# Hidden oracle

> Branch `experiment` · Used after every run (normal, greenfield, brownfield) · **Never copy it into a run folder or mention it to the agent**

Black-box checks of a finished run through the fixed API (`02_design/api-contract.md`), Mailpit and the organizer export. The same oracle judges every run, so the results are comparable.

| Group | Scenarios | Runs |
|---|---|---|
| Regression, the base project | R01–R15: form configuration, external and student registration, rejections leave nothing stored (whitespace-only name, invalid email, unknown option, missing consent, rejected captcha), participant email, organizer email with the JSON attachment, export (columns, rows, no access without credentials), Slovenian characters, trimming, rate limit | every run |
| Feature `search` | S01–S10: list newest first with total, text search across fields and case, type filter, option filter, combined filters, no match, paging 20 + 5, invalid parameters, no access without credentials, Slovenian characters | runs with `--feature search` |
| Feature `group` | G01–G11: a group of three is accepted, export rows with group and contact, member, contact and organizer emails with one JSON attachment per member, rejections (1 or 11 members, invalid member email, duplicate member email, invalid contact email, missing consent, rejected captcha) leave nothing stored | runs with `--feature group` |

All generated names are letters only (a strict name validation must not block the oracle: lesson from the confreg study), and each execution uses a random tag, so earlier data does not disturb it. Standard library only (Python 3.10+).

## How to run it after a run

The stack must be the one the run built (`02_output/docker-compose.yml`). The oracle needs two backend starts with different settings; results go to a folder **outside** the repository.

`<feature>` is `none` for a normal run, `search` or `group` for a feature run.

### PowerShell

```powershell
cd <run folder>\02_output
$env:ORGANIZER_USERNAME = "..."; $env:ORGANIZER_PASSWORD = "..."; $env:ORGANIZER_EMAILS = "..."   # from the run's .env
$O = "C:\path\to\feature-testing-experiment\oracle"      # this folder, outside the run folder
$R = "C:\path\to\results\<run label>"                    # outside the run folder
New-Item -ItemType Directory -Force $R | Out-Null
$env:CAPTCHA_TEST_MODE = "true"; $env:ORGANIZER_HTTPS_ONLY = "false"

$env:RATE_LIMIT_PER_MINUTE = "10000"
docker compose --env-file ..\.env up -d --build --force-recreate --wait
py "$O\oracle.py" main --feature <feature> --out "$R\oracle-main.json"

$env:RATE_LIMIT_PER_MINUTE = "3"
docker compose --env-file ..\.env up -d --force-recreate --wait
py "$O\oracle.py" ratelimit --feature <feature> --out "$R\oracle-ratelimit.json"

py "$O\summarize.py" $R
docker compose --env-file ..\.env down -v
```

### Git Bash, Linux or macOS

```bash
cd <run folder>/02_output
export ORGANIZER_USERNAME=... ORGANIZER_PASSWORD=... ORGANIZER_EMAILS=...   # from the run's .env
O=/path/to/feature-testing-experiment/oracle; R=/path/to/results/<run label>; mkdir -p "$R"
export CAPTCHA_TEST_MODE=true ORGANIZER_HTTPS_ONLY=false

RATE_LIMIT_PER_MINUTE=10000 docker compose --env-file ../.env up -d --build --force-recreate --wait
python3 $O/oracle.py main --feature <feature> --out "$R/oracle-main.json"
RATE_LIMIT_PER_MINUTE=3 docker compose --env-file ../.env up -d --force-recreate --wait
python3 $O/oracle.py ratelimit --feature <feature> --out "$R/oracle-ratelimit.json"
python3 $O/summarize.py "$R"
docker compose --env-file ../.env down -v
```

`summarize.py` prints every scenario and writes `oracle-summary.json` with the regression pass rate and the feature pass rate. Report results as they are: do not fix the run's code, and note any manual step (for example a setting the compose file did not pass through) in the run's notes. If the stack runs on other ports, set `ORACLE_BASE_URL` and `ORACLE_MAILPIT_URL`. `down -v` at the end deletes the run's database, so the next run starts clean.

## Oracle results into the statistics

So the report shows the oracle's pass rates next to every other measure (template 1.7-ft):

1. Copy the three result files into the run folder: `$R/oracle-main.json`, `oracle-ratelimit.json`, `oracle-summary.json` → `<run folder>/03_statistics/results/oracle/`.
2. Rebuild the report in the run folder: `node 03_statistics/tools/build-report.mjs --run 03_statistics`. The report and `run-summary.md` then show "Hidden oracle: base project scenarios passed" and, for a feature run, "Hidden oracle: feature scenarios passed".
3. Commit (`oracle <run label>`) and push the run's branch.

The oracle results are added **after** the run is finished and committed, so the agent never sees them.

## Privacy check before committing statistics

The transcript backup (`03_statistics/data/*.jsonl`) is committed with the run and contains everything the session printed. If the agent ever printed a value from `.env` (it happened once, in tanej run-1, with the organizer address), it would be published. Before every statistics or oracle commit, in the run folder (Git Bash), list the files that contain a `.env` value of 6 or more characters; it prints names only, never values:

```bash
for k in $(sed -n 's/^\([A-Z_]*\)=.*/\1/p' .env); do v="$(sed -n "s/^$k=//p" .env | tr -d '\r')"; [ ${#v} -ge 6 ] || continue; for f in $(git ls-files -co --exclude-standard); do grep -qF -- "$v" "$f" && echo "$f contains $k"; done; done
```

No output: commit. A file listed: replace the value in that committed copy with `[REDACTED]` (never in the original session file under `~/.claude`), check the file is still valid, note it in the commit message and the run notes. Closing the run's session later rebuilds the report and copies the original transcript back into `data/`; do not commit that change, discard it.
