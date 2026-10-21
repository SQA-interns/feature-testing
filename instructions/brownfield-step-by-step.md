# Brownfield run: step by step

> For the brownfield owner · Complete walkthrough from an empty PC folder to the pushed results · Background and the shared rules: `brownfield.md`

Each step says **what** to do, **where**, and **why**. Commands are given for PowerShell (Windows); Git Bash works the same with `/` paths. Replace `<you>` with your name in lower case (`kyuhi`, `tjan`, …) and `<feature>` with `search` or `group`.

---

## A. Once, before your first run

### A1. Check the tools on your machine

Docker Desktop running; Docker Engine 29.8.x and Compose 5.5 or later; Node.js 24.13.0 with npm 11.6.2; Java 21; Python 3.10 or later (`py --version`); Git; internet access (the scanners download vulnerability data). You need write access to https://github.com/SQA-interns/feature-testing.

**Why:** the runs must use the same tools as the greenfield and normal runs; a different patch version is pre-approved (PA-05), anything else costs a question during the run.

### A2. Choose one working folder for the experiment

For example `C:\Users\<you>\Documents\LAB`. Everything below lives there, side by side:

```
C:\Users\<you>\Documents\LAB\
├── feature-testing-experiment\            ← A3: feature files, oracle, instructions (never a run folder)
├── <you>_single-agent_sdd_v001_run-1-search\   ← B: one folder per brownfield run
└── results\                                ← D: oracle results, outside every run folder
```

**Why:** the agent of a run sees its own folder only. The feature package, the decisions and the oracle must never be inside a run folder, so they live next to it. Keep the folder path short: the project has long file names, and Windows refuses paths over 260 characters.

### A3. Clone the experiment branch into its own folder

```powershell
cd C:\Users\<you>\Documents\LAB
git clone -b experiment https://github.com/SQA-interns/feature-testing.git feature-testing-experiment
```

Before every run, update it: `cd feature-testing-experiment; git pull`.

**Why:** this folder holds what both sides share: `features\<feature>\` (the feature's input files), `oracle\` (the hidden tests), `DECISIONS.md` and these instructions. It is a separate clone so it never mixes with a run.

### A4. Decide how the agent is told it is a change (open decision, see `brownfield.md`)

Template 1.7-ft describes building a system from an empty `02_output\`. A brownfield run starts from a finished system, so the agent needs to be told that. Decide **once**, before your first run, and use the same choice in every brownfield run. Proposal: a fixed start prompt (section C2), so no template file changes. Record your choice in `DECISIONS.md` (on the `experiment` branch) as a new row and tell tanej.

---

## B. Prepare one brownfield run

### B1. Find the base run

The base is a finished **normal** run on template 1.7-ft, for example tanej's `tanej/single-agent/sdd/v001/run-1`. Agree with tanej which normal run is the base for `search` and for `group` (the same one for both is simplest). Note its branch name and its **final commit SHA** (the last commit on that branch, after its statistics and oracle commits): on GitHub, open the branch and copy the SHA of the latest commit.

**Why:** a brownfield run must start from a system built with exactly the same inputs as the greenfield run, minus the feature. Older 1.7 runs do not follow the fixed API contract, so they cannot be the base (decision E-15).

### B2. Clone the base run into a new run folder

```powershell
cd C:\Users\<you>\Documents\LAB
git -c core.longpaths=true clone --single-branch -b tanej/single-agent/sdd/v001/run-1 https://github.com/SQA-interns/feature-testing.git <you>_single-agent_sdd_v001_run-<n>-<feature>
cd <you>_single-agent_sdd_v001_run-<n>-<feature>
git config core.longpaths true
git switch -c <you>/single-agent/sdd/v001/run-<n>-<feature> <base SHA>
```

`<n>` counts per feature: your first `search` run is `run-1-search`. Check with `git log -1` that you are on the base SHA.

**Why:** `--single-branch` brings only the base run's branch (no `experiment` branch, so no oracle in the folder); the new branch starts exactly at the finished system.

### B3. Keep `.env` out of git

```powershell
Add-Content -Encoding ascii .git\info\exclude ".env"
```

**Why:** the folder has no rule yet that ignores `.env` until you check; a commit must never contain secrets.

### B4. Reset the statistics, keep the system

The base run's statistics must not be counted as yours. Delete them (they stay in the base run's git history):

```powershell
Remove-Item 03_statistics\run-log.json
Remove-Item -Recurse -Force 03_statistics\results, 03_statistics\data
git checkout <base run's "run setup" commit> -- 03_statistics/run-summary.md
```

The base run's "run setup" commit is the first commit on its branch after the template (`git log --oneline` and look for `run setup tanej_single-agent_sdd_v001_run-1`). Restoring `run-summary.md` from it gives you the empty skeleton again.

Leave everything else as it is: `02_output\` (code, tests, documents, the frozen-test manifest), `01_input\`, `03_statistics\usage.md` (prices).

**Why:** the brownfield run measures the change only, starting from the finished system.

### B5. Put the feature into the inputs

```powershell
Copy-Item -Recurse -Force ..\feature-testing-experiment\features\<feature>\01_input\* .\01_input\
git status
```

`git status` must list exactly 9 changed files, all under `01_input\01_project\`. Do not edit them.

**Why:** these are the same generated files the greenfield run gets, so both sides work from the identical feature text (decision E-06).

### B6. Fill in the run configuration

Open `01_input\01_project\00_setup\run-config.md` in VS Code and set:

| Line | Value |
|---|---|
| Run label | `<you>_single-agent_sdd_v001_run-<n>-<feature>` |
| Operator | `<you>` |
| Machine | `<you>PC` |
| Plan | your plan, e.g. `PRO` |
| Permission mode | `auto` |
| Run type | `brownfield` |
| Feature | `<feature>` |
| Base run | `tanej_single-agent_sdd_v001_run-1 @ <base SHA>` |
| Run ID, Starting commit | leave empty (generated) |

Leave Model `claude-opus-5-5`, Effort `medium` and Template version `1.7-ft` as they are.

**Why:** the report uses Run type, Feature and Base run to place your run next to the greenfield and normal runs.

### B7. Create `.env`

Copy `01_input\01_project\00_setup\secrets.env.example` to `.env` in the run folder and fill every required value (database password, organizer user, organizer password of at least 16 characters, organizer email addresses, NVD key). Never paste a secret into the chat.

### B8. Commit and push the start state

```powershell
git add -A
git status          # .env must NOT be listed
git commit -m "change request <feature> on tanej_single-agent_sdd_v001_run-1"
git push -u origin <you>/single-agent/sdd/v001/run-<n>-<feature>
```

**Why:** the start of the run is on record before the agent starts; the report measures from this commit.

### B9. Start with an empty database

```powershell
docker volume ls
```

If volumes from an earlier run of this project exist (names starting with the base run's compose project, e.g. `registration_` or `confreg_`), remove them with `docker volume rm <name>`.

**Why:** a stack with the same project name would otherwise start on old data.

---

## C. Run the agent

### C1. Open the run folder

In VS Code: **File → New Window → Open Folder…** → `C:\Users\<you>\Documents\LAB\<you>_single-agent_sdd_v001_run-<n>-<feature>`. The branch at the bottom left must be `<you>/single-agent/sdd/v001/run-<n>-<feature>`. If asked to trust the folder or its hooks, accept (the hook builds the report at the end).

Do not run the agent in `feature-testing-experiment`, and do not talk about the experiment in a Claude session in the run folder: sessions of one folder share Claude's memory.

### C2. Start one session

Open Claude Code in that window, choose **Opus 5.5**, **Medium** effort, permission mode **Auto**, and send the start prompt you decided in A4. Proposal:

```
Read AGENTS.md and start. This is a change to a finished system: 02_output/ holds a complete, verified implementation of the inputs without US-009. The inputs now also contain US-009 and the lines added with it. Run every phase for the change: keep the existing frozen acceptance tests and their manifest unchanged, add acceptance and end-to-end tests only for the new criteria and freeze them in a separate manifest, and make the whole system, old and new, pass every gate.
```

Use only this one new session for the run. Never continue or fork the normal run's chat (decision E-16): the run must start with an agent that knows only the files, and its statistics must contain this run only.

### C3. During the run

Answer exactly as `Run_Guide.md` (section 3) says, the same as every other run: tooling questions with the standard answers, anything else with *"Choose the proposed default option and record it as a decision."*, and never add information about the feature beyond the inputs. Do not leave a question unanswered for more than an hour.

### C4. After the agent's final message

Wait about 30 seconds: the hook builds `03_statistics\results\run-report.html`. Run the privacy check from `oracle/README.md` (section "Privacy check before committing statistics"). Then commit the statistics:

```powershell
git add -A
git status          # .env must NOT be listed
git commit -m "statistics <you>_single-agent_sdd_v001_run-<n>-<feature>"
git push
```

---

## D. Run the oracle

### D1. Set up the results folder and the settings

In a **new PowerShell window** (the settings below stay set for the whole window):

```powershell
cd C:\Users\<you>\Documents\LAB\<you>_single-agent_sdd_v001_run-<n>-<feature>\02_output
$env:ORGANIZER_USERNAME = "..."; $env:ORGANIZER_PASSWORD = "..."; $env:ORGANIZER_EMAILS = "..."   # the values from this run's .env
$env:CAPTCHA_TEST_MODE = "true"; $env:ORGANIZER_HTTPS_ONLY = "false"
$O = "C:\Users\<you>\Documents\LAB\feature-testing-experiment\oracle"
$R = "C:\Users\<you>\Documents\LAB\results\<you>_single-agent_sdd_v001_run-<n>-<feature>"
New-Item -ItemType Directory -Force $R | Out-Null
```

**Why:** the oracle reads the organizer login from these variables; the results go to a folder outside the run folder so the run's git history never contains oracle output before the run is closed.

### D2. Main phase (rate limit 10000)

```powershell
$env:RATE_LIMIT_PER_MINUTE = "10000"
docker compose --env-file ..\.env up -d --build --force-recreate --wait
py "$O\oracle.py" main --feature <feature> --out "$R\oracle-main.json"
```

It prints one line per scenario (R01–R14 and S01–S10 or G01–G11) with `pass`, `fail` or `harness`.

### D3. Rate-limit phase (rate limit 3)

```powershell
$env:RATE_LIMIT_PER_MINUTE = "3"
docker compose --env-file ..\.env up -d --force-recreate --wait
py "$O\oracle.py" ratelimit --feature <feature> --out "$R\oracle-ratelimit.json"
```

### D4. Summary, then stop the stack

```powershell
py "$O\summarize.py" $R
docker compose --env-file ..\.env down -v
```

`summarize.py` writes `oracle-summary.json` with the base-project pass rate (R01–R15) and the feature pass rate. `down -v` deletes this run's database so the next run starts clean.

Report the results as they are: do not change the run's code. If the compose file did not pass a setting through and you had to add it for the test, write that down.

### D5. Put the results into the statistics

```powershell
cd ..
New-Item -ItemType Directory -Force 03_statistics\results\oracle | Out-Null
Copy-Item "$R\oracle-main.json","$R\oracle-ratelimit.json","$R\oracle-summary.json" 03_statistics\results\oracle\
node 03_statistics\tools\build-report.mjs --run 03_statistics
git add -A
git status          # .env must NOT be listed
git commit -m "oracle <you>_single-agent_sdd_v001_run-<n>-<feature>"
git push
```

Open `03_statistics\results\run-report.html` in a browser: under "Product quality" it now shows "Hidden oracle: base project scenarios passed" and "Hidden oracle: feature scenarios passed".

---

## E. Tell tanej

Send: the branch name, the base run and SHA, the oracle summary (the two pass rates and any failed scenario), and anything unusual (a question you had to answer differently, a usage-limit stop, a manual compose change, a network interruption).
