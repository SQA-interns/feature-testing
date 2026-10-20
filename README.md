# Feature experiment: branch `experiment`

> Shared by greenfield (tanej) and brownfield · **Never clone this branch into a run folder** (run folders are cloned with `--single-branch -b main`)

The same conference-registration project (template 1.7, on `main`) gets one extra feature per run, built two ways:

| Run type | Feature | Starts from |
|---|---|---|
| **normal** | none (baseline) | `main` |
| **greenfield** | in the inputs from the start | `main` plus the feature's input files |
| **brownfield** | added as a change request | the finished state of a normal run plus the same feature text |

Features: `search` (organizer search and filter) and `group` (group registration). The results of all three run types are compared with the same statistics, the same HTML report and the same hidden oracle.

## Contents

| Path | What |
|---|---|
| `DECISIONS.md` | every decision of the experiment and its status |
| `features/<feature>/feature-package.md` | the complete specification of a feature: the text blocks added to the project files, word for word the same for greenfield and brownfield |
| `features/<feature>/01_input/01_project/` | the project files with the package applied (generated, never edited by hand) |
| `features/build-inputs.py` | regenerates those files from the package and `main`, and checks every block is used verbatim |
| `oracle/` | the hidden oracle: regression scenarios for every run, feature scenarios per feature; how to run it |
| `instructions/greenfield.md` | how a greenfield run is done |
| `instructions/brownfield.md` | what brownfield shares with greenfield, and the proposed brownfield procedure |
| `instructions/normal.md` | how a normal (baseline) run is done |

## Naming

Branch `<user>/single-agent/sdd/v001/run-<n>[-<feature>]`, the run number counted per feature: `tanej/single-agent/sdd/v001/run-1-search`, `…/run-1-group`, normal runs `…/run-1`, `…/run-2`. Run label (in `run-config.md`, used as the folder name): the branch name with `/` replaced by `_`, for example `tanej_single-agent_sdd_v001_run-1-search`.
