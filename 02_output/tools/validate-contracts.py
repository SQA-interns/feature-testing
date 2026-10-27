"""Validate the machine-readable contracts in docs/02_contracts with a parser.

Usage: python3 -I 02_output/tools/validate-contracts.py 02_output/docs/02_contracts <oas-3.1-schema.json>
The OAS 3.1 schema is https://spec.openapis.org/oas/3.1/schema/2022-10-07.
Needs PyYAML and jsonschema (Draft 2020-12); see decision D-15.
"""
import json, sys, yaml, jsonschema
from jsonschema import Draft202012Validator
C = sys.argv[1]; oas = json.load(open(sys.argv[2]))
ok = True
def report(name, errs):
    global ok
    if errs: ok = False; print("FAIL", name); [print("   ", e) for e in errs]
    else: print("OK  ", name)
# OpenAPI: parse, validate against the official OAS 3.1 schema, resolve every $ref
doc = yaml.safe_load(open(f"{C}/openapi.yaml"))
v = Draft202012Validator(oas)
report("openapi.yaml vs OAS 3.1 schema", [e.message for e in v.iter_errors(doc)])
refs = []
def walk(n):
    if isinstance(n, dict):
        for k, x in n.items():
            if k == "$ref": refs.append(x)
            else: walk(x)
    elif isinstance(n, list):
        for x in n: walk(x)
walk(doc)
bad = []
for r in refs:
    node = doc
    try:
        for p in r.lstrip("#/").split("/"): node = node[p]
    except Exception: bad.append(r)
report(f"openapi.yaml $refs resolve ({len(refs)})", bad)
for name, s in doc["components"]["schemas"].items():
    try: Draft202012Validator.check_schema(s)
    except jsonschema.SchemaError as e: report(f"schema {name}", [e.message])
# JSON schemas: meta-schema check, examples validate
for f in ["conference-config.schema.json", "registration-copy.schema.json", "recaptcha-siteverify.schema.json"]:
    s = json.load(open(f"{C}/{f}"))
    try: Draft202012Validator.check_schema(s); report(f + " meta-schema", [])
    except jsonschema.SchemaError as e: report(f + " meta-schema", [e.message])
    vv = Draft202012Validator(s, format_checker=Draft202012Validator.FORMAT_CHECKER)
    for i, ex in enumerate(s.get("examples", [])):
        report(f"{f} example {i}", [e.message for e in vv.iter_errors(ex)])
# negative checks: the schemas reject what they must
cc = Draft202012Validator(json.load(open(f"{C}/conference-config.schema.json")))
for desc, bad_doc in [("unknown category", {"options":[{"id":"a","name":"A","category":"X","active":True}],"consents":[]}),
                      ("missing name", {"options":[{"id":"a","category":"MEAL","active":True}],"consents":[]}),
                      ("blank name", {"options":[{"id":"a","name":"  ","category":"MEAL","active":True}],"consents":[]})]:
    report(f"conference-config rejects {desc}", [] if list(cc.iter_errors(bad_doc)) else ["accepted"])
rc = Draft202012Validator(json.load(open(f"{C}/registration-copy.schema.json")))
ex = json.load(open(f"{C}/registration-copy.schema.json"))["examples"][0]
s2 = json.loads(json.dumps(ex)); s2["participant"]["studentId"] = "1"
report("registration-copy rejects student field on EXTERNAL", [] if list(rc.iter_errors(s2)) else ["accepted"])
sys.exit(0 if ok else 1)
