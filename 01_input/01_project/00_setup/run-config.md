# Run configuration

> Owner: Human operator · Read in: phase 0 · Agent: read-only

The only file a human fills before starting the agent; the agent checks everything else in phase 0 (`general/skills/preflight`). `03_statistics/tools/stat.mjs` reads the lines below, so keep the `- Key: value` format and write only the value after the colon.

- Model: claude-opus-5-5
- Effort: medium
- Template version: 1.7
- Run label: kyuhi_single-agent_sdd_v007
- Operator: kyuhi
- Machine: kyuhiPC
- Plan: PRO
- Permission mode: auto
- Run ID:
- Starting commit:

| Line | What to write |
|---|---|
| Model, Effort, Template version, Operator, Machine, Plan, Permission mode | pre-filled for kyuhi; check them, and change them if someone else or another setup does the run |
| Run label | **change it for every run** (the next one is `kyuhi_single-agent_sdd_v008`); use it as the run folder's name; it names the run in reports |
| Run ID | **leave empty**: `stat.mjs init` generates a unique one |
| Starting commit | **leave empty**: `stat.mjs init` records the commit the agent starts from |
| Operator | `kyuhi`, `tanej` or `tjan` (lower case, exactly like this) |
| Machine | a label for the computer, no host names or addresses |
| Plan | `PRO`, `Max 5x`, `API`, … |
| Permission mode | `auto` or `default` (ask first) |
