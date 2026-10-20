# Run guide

> Feature experiment (template 1.7-ft): the folder name and branch follow `<user>/single-agent/sdd/v001/run-<n>[-<feature>]`, and greenfield and brownfield runs have extra steps. Follow `instructions/normal.md`, `greenfield.md` or `brownfield.md` on the `experiment` branch; they list only what differs from this guide. Clone with `--single-branch -b main` so the `experiment` branch never reaches the run folder.

This setup is run three times, once each by kyuhi, tanej and tjan, in one shared environment, and the three runs are compared. Everything below is therefore the same for everyone; the only things that may differ are the four lines in the table "If you are not kyuhi".

## 1. Prepare the run folder (clone, branch, .env)
1. **Clone the template** into a new folder named after the run (`kyuhi_single-agent_sdd_v007`). Branch `main` is the template you clone and never commit to it.
   ```bash
   git clone --single-branch -b main https://github.com/SQA-interns/feature-testing.git <user>_single-agent_sdd_<version>
   cd <user>_single-agent_sdd_<version>
   ```
   
2. **Create the run's branch** (it is the only branch you work and push on): `<user>/single-agent/sdd/<version>`, for kyuhi `kyuhi/single-agent/sdd/v007`:
   ```bash
   git switch -c <user>/single-agent/sdd/<version>
   ```
   
3. **Keep `.env` out of git before you create it** (the template has no `.gitignore`; the agent writes one in phase 0):
   - Git Bash: `echo .env >> .git/info/exclude`
   - PowerShell: `Add-Content -Encoding ascii .git\info\exclude ".env"`
     
4. **Check and update values in `01_input/01_project/00_setup/run-config.md`** (change the values to match your environment).
      - operator `kyuhi`,
      - machine `kyuhiPC`,
      - plan `PRO`,
      - permission mode `auto`,
      - model `claude-opus-5-5`,
      - effort `medium`
      - Run label: **change it for every run** (`kyuhi_single-agent_sdd_v001`, then `v002`,...).
      - Run ID: **keep empty it generates by itself**
      - Starting commit: **keep empty it generates by itself**
        
5. **Check the price row in `03_statistics/usage.md`.** It is pre-filled for Opus 5.5; change it only if the prices or the model changed.

6. **Create `.env`**: copy `01_input/01_project/00_setup/secrets.env.example` to `.env` in the run folder and fill every value (database password, organizer login, NVD key). Never paste a secret into the chat.

7. **Commit your changes to `run-config.md` (and `usage.md`, if you changed it)** so the starting commit contains the right user, machine, plan and label: `git add -A`, `git commit -m "run setup <run label>"`. If you changed nothing there is nothing to commit and the starting commit is the commit of `main` you cloned. Check that `git status` does not list `.env` before anything is pushed.

8. **Push the run's branch** (so the start state is on record before the agent starts; `origin` is already set by the clone):
   ```bash
   git push -u origin <user>/single-agent/sdd/<version>
   ```
   Use your own config in the branch. Every user pushes to a branch of its own, never to `main`. Do not push before `.env` is confirmed absent from `git status`.
   
9. **Set the same permission mode** you set in `run-config.md`.
10. **Check the environment** (the same for all three operators): Docker Engine 29.8.1 and Compose 5.5.1, Node 24.13.0, Java 21, internet access for the scanners (NVD key in `.env`), no other heavy load on the machine during the run, one session only.
11. Change these lines in `01_input/01_project/00_setup/run-config.md` according to your environment (the table bellow shows what you should input for yourself):

| Line | kyuhi | tanej | tjan |
|---|---|---|---|
| Operator | `kyuhi` | `tanej` | `tjan` |
| Machine | `kyuhiPC` | `tanejPC` | `tjanPC` |
| Plan | `PRO` | `PRO` | `PRO` |
| Run label | `kyuhi_single-agent_sdd_<version>` | `tanej_single-agent_sdd_<version>` | `tjan_single-agent_sdd_<version>` |

## 2. Start the run
Open a **new agent session in the run folder** (only one session per run; do not open other sessions in this folder, the newest one is taken as the run's transcript) and send exactly:

```
Read AGENTS.md and start.
```

## 3. During the run
- Answer tooling and environment questions with the standard answers below, the same way in every run.
- The acceptance criteria, the end-to-end scenarios and the answers to the open questions are inputs, so the agent should not ask about business behaviour. If it does anyway, answer only: *"No further information is available. Choose the proposed default option and record it as a decision."*
- To **any other question not listed below**, give the same answer in every run: *"Choose the proposed default option and record it as a decision."* A frozen acceptance test that cannot pass (a defect in the test harness or in one assertion): approve the agent's proposed default fix (option 1). Do not add your own ideas or hints; different answers make the runs incomparable.
- The agent works until the **quality gate** is met (mutation score 90 %, line coverage 95 %, per component). Do not tell it to stop earlier or to go further.
- Do not change the input files. Do not leave a question unanswered for more than an hour: the cache expires and the wait is excluded from the results, but the time is lost. If you must stop, tell the agent so it records the wait.

**Standard answers (conference registration project; decision numbers differ, recognise them by subject)**
| Phase | The agent asks about | Answer |
|---|---|---|
| 0 | Host JDK, Node, npm or Docker differs from the pins by a patch version | No question: pre-approved (PA-05). Anything else (a different minor or major version): approve the installed version only if the agent says everything runs. |
| 0 | `vitest` or `jscpd` has Critical or High vulnerabilities | Move to the fixed versions the agent proposes (`jscpd` 5.4.0). `vitest` and `@vitest/coverage-v8` stay on 4.1.11: 5.x does not work with Stryker (see phase 6). No question if `tech-stack.md` already has them. |
| 0 | CVE-2025-7962 on `angus-activation`, CVE-2025-15104 on `hibernate-validator`, dev-only `npm audit` advisories | No question: pre-approved in `01_project/00_setup/pre-approved.md` (PA-01 to PA-03). If the agent asks anyway, answer: *use pre-approved.md*. |
| 6 | Semgrep "basic authentication" (High) on the organizer endpoint | No question: pre-approved (PA-04). If the agent asks anyway, answer: *use pre-approved.md*. |
| 6 | Stryker reports an implausibly low frontend score (about 25 %) or the agent says mutants are not activated | Approve `vitest` and `@vitest/coverage-v8` 4.1.x, the same version for both; `@stryker-mutator/vitest-runner` 10.0.0 does not work with Vitest 5. No question if `tech-stack.md` has 4.1.11. |

## 4. After the run
The measuring is automatic and nothing is typed or copied by hand: cost, tokens, times, context, approval prompts and commits are all calculated from the session transcript and git. `.claude/settings.json` in the run folder holds a hook that builds the report when the session ends (or about 8 seconds after the agent's last reply once the run has ended). Do not edit or delete it. If Claude Code asks whether to trust the folder or its hooks, accept.
1. **Wait for the agent's final message, then about 30 seconds.** You do not need to close the session. Check that `03_statistics/results/run-report.html` exists; the hook has then written `03_statistics/results/` (report, measurements, usage summary, commits, readable transcript) and `03_statistics/data/` (transcript backup), and filled the Run, Time, Commits and Cost sections of `03_statistics/run-summary.md`. `03_statistics/results/auto-report.log` shows what it did. Closing the session with `/exit` is optional: it only repeats the build from the final transcript.
2. **Only if `results/run-report.html` is missing** (hook not run, session killed, folder not trusted): open a new session in the same folder and send exactly:

   ```
   Run node 03_statistics/tools/build-report.mjs --run 03_statistics and report what it prints. Do not change anything else.
   ```

3. Generated report `03_statistics/results/run-report.html` should automatically fill in all necessary run comparison data. 
4. Commit the statistics: `git add -A`, `git commit -m "statistics <label>"`. `git status` must not list `.env`. Then push the statistics: `git push` (the branch is already connected from step 1.8).
