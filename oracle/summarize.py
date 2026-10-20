#!/usr/bin/env python3
"""Combine oracle-main.json and oracle-ratelimit.json of one run into a summary.

    python summarize.py <folder with the oracle JSON files>

Prints one line per scenario and writes oracle-summary.json with the regression pass rate (R01-R15)
and the feature pass rate (S01-S10 or G01-G11). "harness" means the scenario could not be checked
(for example the form configuration was not usable); it counts as not passed and is reported as is.
"""
import json, sys
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8", errors="replace")  # the Windows console code page cannot show č, š, ž
folder = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
results, feature = {}, "none"
for phase in ("main", "ratelimit"):
    f = folder / f"oracle-{phase}.json"
    if f.exists():
        data = json.loads(f.read_text(encoding="utf-8"))
        results.update(data["results"])
        feature = data.get("feature", feature) if phase == "main" else feature


def rate(prefix):
    ids = sorted(k for k in results if k.startswith(prefix))
    passed = sum(results[k]["result"] == "pass" for k in ids)
    return {"passed": passed, "total": len(ids), "rate": round(passed / len(ids), 3) if ids else None,
            "failed": [k for k in ids if results[k]["result"] == "fail"],
            "harness": [k for k in ids if results[k]["result"] == "harness"]}


summary = {"feature": feature, "regression": rate("R"),
           "featureScenarios": rate({"search": "S", "group": "G"}[feature]) if feature in ("search", "group") else None,
           "scenarios": {k: results[k]["result"] for k in sorted(results)}}
for k in sorted(results):
    print(f"{k:5s} {results[k]['result']}")
reg = summary["regression"]
print(f"regression: {reg['passed']} of {reg['total']}")
if summary["featureScenarios"]:
    fs = summary["featureScenarios"]
    print(f"feature {feature}: {fs['passed']} of {fs['total']}")
(folder / "oracle-summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
print(f"written {folder / 'oracle-summary.json'}")
