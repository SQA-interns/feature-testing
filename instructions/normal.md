# Normal run (baseline)

> Shared by both sides · A normal run is the baseline for both features and the starting point of a brownfield run

Follow `Run_Guide.md` on `main` exactly, with these differences:

1. **Clone `main` only**, so the `experiment` branch (feature packages, oracle) never reaches the run folder:
   ```bash
   git clone --single-branch -b main https://github.com/SQA-interns/feature-testing.git <run label>
   cd <run label>
   git switch -c <user>/single-agent/sdd/v001/run-<n>
   ```
2. **`run-config.md`**: Run label `<user>_single-agent_sdd_v001_run-<n>`; Run type `normal`; Feature `none`; Base run empty.
3. Do **not** copy any feature files. Start with `Read AGENTS.md and start.` as in the run guide.
4. **After the run**: commit the statistics as the run guide says, then run the oracle with `--feature none` (`oracle/README.md`) and follow "Oracle results into the statistics" there.
5. Push the branch. Its final commit is a possible starting point for a brownfield run; note the SHA.
