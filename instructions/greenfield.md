# Greenfield run

> Owner: tanej · The feature is part of the inputs from the start; everything else is a normal run

Follow `Run_Guide.md` on `main` exactly, with these differences:

1. **Have this branch in its own folder** (once), outside every run folder:
   ```bash
   git clone -b experiment https://github.com/SQA-interns/feature-testing.git feature-testing-experiment
   ```
2. **Clone `main` only** into the run folder, so this branch never reaches it:
   ```bash
   git clone --single-branch -b main https://github.com/SQA-interns/feature-testing.git <run label>
   cd <run label>
   git switch -c <user>/single-agent/sdd/v001/run-<n>-<feature>
   ```
3. **Copy the feature's input files** over the run's inputs (merge, overwrite):
   - PowerShell: `Copy-Item -Recurse -Force ..\feature-testing-experiment\features\<feature>\01_input\* .\01_input\`
   - Git Bash: `cp -r ../feature-testing-experiment/features/<feature>/01_input/. ./01_input/`

   Check: `git status` lists only files under `01_input/01_project/` (9 files), and `git diff --stat` matches the package (`feature-package.md`). Nothing else is changed by hand.
4. **`run-config.md`**: Run label `<user>_single-agent_sdd_v001_run-<n>-<feature>`; Run type `greenfield`; Feature `<feature>`; Base run empty.
5. Commit ("inputs <feature>"), push the branch, and start with `Read AGENTS.md and start.` as in the run guide. Answer questions with the run guide's standard answers; never add information about the feature beyond the package.
6. **After the run**: commit the statistics as the run guide says, then run the oracle with `--feature <feature>` (`oracle/README.md`) and follow "Oracle results into the statistics" there.

What makes a greenfield run comparable: the inputs differ from a normal run only by the generated feature files, which are identical to the brownfield change request.
