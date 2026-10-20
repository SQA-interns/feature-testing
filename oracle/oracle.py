#!/usr/bin/env python3
"""Hidden oracle of the feature experiment: black-box checks of a finished run through the fixed API
(`02_design/api-contract.md`), Mailpit and the organizer export. Never show this file to a run's agent.

Scenario groups:
  R  regression: the base project (every run: normal, greenfield and brownfield)
  S  feature `search`   (runs with --feature search)
  G  feature `group`    (runs with --feature group)

Phases (restart the backend with the listed settings before each phase; ORGANIZER_HTTPS_ONLY=false
and CAPTCHA_TEST_MODE=true in both):
  main       RATE_LIMIT_PER_MINUTE=10000   -> R01..R14 and the feature's scenarios
  ratelimit  RATE_LIMIT_PER_MINUTE=3       -> R15 (the 4th request in a minute is answered with 429)

Settings (environment variables):
  ORACLE_BASE_URL      backend URL                default http://127.0.0.1:8080
  ORACLE_MAILPIT_URL   Mailpit URL                default http://127.0.0.1:8025
  ORGANIZER_USERNAME / ORGANIZER_PASSWORD        organizer credentials from the run's .env
  ORGANIZER_EMAILS     organizer addresses from the run's .env (comma separated)

Usage: python oracle.py <main|ratelimit> --feature <none|search|group> [--out <file.json>]
Standard library only (Python 3.10+). All generated names are letters only, so a strict name
validation never blocks the oracle. Each run uses a random tag, so earlier data in the database
does not disturb the checks.
"""
import base64, io, json, os, random, re, string, sys, time, urllib.error, urllib.parse, urllib.request, zipfile
from xml.etree import ElementTree as ET

BASE = os.environ.get("ORACLE_BASE_URL", "http://127.0.0.1:8080").rstrip("/")
MAIL = os.environ.get("ORACLE_MAILPIT_URL", "http://127.0.0.1:8025").rstrip("/")
AUTH = {"Authorization": "Basic " + base64.b64encode(
    f"{os.environ.get('ORGANIZER_USERNAME', '')}:{os.environ.get('ORGANIZER_PASSWORD', '')}".encode()).decode()}
ORGANIZERS = [a.strip().lower() for a in os.environ.get("ORGANIZER_EMAILS", "").split(",") if a.strip()]
TAG = "".join(random.choice(string.ascii_lowercase) for _ in range(6))   # letters only
TOKEN = "test-pass"
EXPORT_COLUMNS = ["id", "type", "firstName", "lastName", "email", "organization", "studyInstitution",
                  "studyProgramme", "studentId", "options", "consents", "registeredAt"]
GROUP_COLUMNS = ["groupId", "contactName", "contactEmail"]
R = {}


# ------------------------------------------------------------------ helpers
def letters(n):
    """A unique letters-only word, e.g. for a last name."""
    return TAG + "".join(random.choice(string.ascii_lowercase) for _ in range(n))


def http(method, path, body=None, headers=None, base=BASE, raw=False):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(base + path, data=data, method=method)
    if body is not None:
        req.add_header("Content-Type", "application/json")
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            content, status, ctype = r.read(), r.status, r.headers.get("Content-Type", "")
    except urllib.error.HTTPError as e:
        content, status, ctype = e.read(), e.code, e.headers.get("Content-Type", "")
    except urllib.error.URLError as e:
        return 0, None, str(e.reason)
    if raw:
        return status, content, ctype
    try:
        return status, json.loads(content) if content else None, ctype
    except ValueError:
        return status, None, ctype


def ok(status):
    return 200 <= status < 300


def error_fields(js):
    return {e.get("field") for e in (js or {}).get("errors", []) if isinstance(e, dict)}


def record(sc, result, detail):
    R[sc] = {"result": result if isinstance(result, str) else ("pass" if result else "fail"), "detail": detail}
    print(f"{sc:5s} {R[sc]['result']:5s} {detail}")


def form_config():
    st, js, _ = http("GET", "/api/form-config")
    return st, js or {}


def options_for(cfg, rtype):
    return [o["id"] for o in cfg.get("options", []) if rtype in (o.get("availableTo") or [rtype])]


def consents(cfg):
    return [c["id"] for c in cfg.get("consents", []) if c.get("mandatory")]


def external(cfg, last, **extra):
    p = {"type": "EXTERNAL", "firstName": "Ana", "lastName": last, "email": f"{last}@example.org",
         "organization": "Primer doo", "studyInstitution": None, "studyProgramme": None, "studentId": None,
         "options": options_for(cfg, "EXTERNAL")[:1], "consents": consents(cfg), "captchaToken": TOKEN}
    p.update(extra)
    return p


def student(cfg, last, **extra):
    p = {"type": "STUDENT", "firstName": "Luka", "lastName": last, "email": f"{last}@example.org",
         "organization": None, "studyInstitution": "Univerza v Mariboru", "studyProgramme": "Informatika",
         "studentId": "E1234567", "options": options_for(cfg, "STUDENT")[:1], "consents": consents(cfg),
         "captchaToken": TOKEN}
    p.update(extra)
    return p


def register(payload):
    st, js, _ = http("POST", "/api/registrations", payload)
    return st, js or {}


# ---- export (xlsx read with the standard library)
def read_xlsx(content):
    z = zipfile.ZipFile(io.BytesIO(content))
    ns = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
    shared = []
    if "xl/sharedStrings.xml" in z.namelist():
        for si in ET.fromstring(z.read("xl/sharedStrings.xml")).findall("m:si", ns):
            shared.append("".join(t.text or "" for t in si.iter("{%s}t" % ns["m"])))
    sheet = sorted(n for n in z.namelist() if re.match(r"xl/worksheets/sheet\d+\.xml$", n))[0]
    rows = []
    for row in ET.fromstring(z.read(sheet)).iter("{%s}row" % ns["m"]):
        cells = {}
        for c in row.findall("m:c", ns):
            col = re.match(r"[A-Z]+", c.get("r", "A")).group(0)
            idx = 0
            for ch in col:
                idx = idx * 26 + ord(ch) - 64
            t, v = c.get("t"), c.find("m:v", ns)
            if t == "s" and v is not None:
                val = shared[int(v.text)]
            elif t == "inlineStr":
                val = "".join(x.text or "" for x in c.iter("{%s}t" % ns["m"]))
            else:
                val = v.text if v is not None else ""
            cells[idx - 1] = val or ""
        rows.append([cells.get(i, "") for i in range(max(cells) + 1)] if cells else [])
    return rows


def export(auth=True):
    st, content, ctype = http("GET", "/api/export", headers=AUTH if auth else None, raw=True)
    if not ok(st):
        return st, None, None
    try:
        rows = read_xlsx(content)
    except Exception as e:  # not a workbook
        return st, None, f"not a workbook: {e}"
    header = rows[0] if rows else []
    return st, header, [dict(zip(header, r)) for r in rows[1:]]


def export_rows_with(text):
    st, header, rows = export()
    return [r for r in (rows or []) if any(text in str(v) for v in r.values())]


# ---- Mailpit
def mails(wait=15, predicate=lambda m: True):
    deadline, found = time.time() + wait, []
    while time.time() < deadline:
        st, js, _ = http("GET", "/api/v1/messages?limit=500", base=MAIL)
        found = [m for m in (js or {}).get("messages", []) if predicate(m)]
        if found:
            time.sleep(2)
            st, js, _ = http("GET", "/api/v1/messages?limit=500", base=MAIL)
            return [m for m in (js or {}).get("messages", []) if predicate(m)]
        time.sleep(1)
    return found


def to_addr(address):
    return lambda m: any((t.get("Address") or "").lower() == address.lower() for t in m.get("To") or [])


def message(mid):
    st, js, _ = http("GET", f"/api/v1/message/{mid}", base=MAIL)
    return js or {}


def message_text(msg):
    return (msg.get("Subject") or "") + "\n" + (msg.get("Text") or "") + "\n" + re.sub("<[^>]+>", " ", msg.get("HTML") or "")


def json_attachments(msg):
    out = []
    for a in msg.get("Attachments") or []:
        if (a.get("FileName") or "").lower().endswith(".json") or "json" in (a.get("ContentType") or ""):
            st, content, _ = http("GET", f"/api/v1/message/{msg['ID']}/part/{a['PartID']}", base=MAIL, raw=True)
            try:
                out.append(json.loads(content))
            except (ValueError, TypeError):
                out.append(None)
    return out


def organizer_mails_mentioning(text, wait=15):
    def pred(m):
        if ORGANIZERS:
            return any((t.get("Address") or "").lower() in ORGANIZERS for t in m.get("To") or [])
        return True
    return [message(m["ID"]) for m in mails(wait, pred) if text in message_text(message(m["ID"]))]


# ------------------------------------------------------------------ regression (base project)
def regression():
    st, cfg = form_config()
    opts = cfg.get("options", [])
    good = ok(st) and opts and all({"id", "name", "category", "availableTo"} <= set(o) for o in opts) \
        and all(o["category"] in ("WORKSHOP", "EVENT", "MEAL", "OTHER") for o in opts) and consents(cfg)
    record("R01", bool(good), f"status {st}, {len(opts)} options, mandatory consents {consents(cfg)}")
    if not good:
        for sc in [f"R{i:02d}" for i in range(2, 15)]:
            record(sc, "harness", "form configuration not usable, no registration possible")
        return cfg

    ext_last, stu_last = letters(6), letters(6)
    st, js = register(external(cfg, ext_last))
    ext_id = js.get("id")
    record("R02", st == 201 and bool(ext_id), f"external: status {st}, id {ext_id}")
    st, js = register(student(cfg, stu_last))
    record("R03", st == 201 and bool(js.get("id")), f"student: status {st}, id {js.get('id')}")

    def rejected(sc, payload, field, what):
        st, js = register(payload)
        stored = bool(export_rows_with(payload["lastName"])) if payload.get("lastName") else False
        record(sc, st == 400 and field in error_fields(js) and not stored,
               f"{what}: status {st}, error fields {sorted(error_fields(js))}, stored {stored}")

    rejected("R04", external(cfg, letters(6), firstName="   "), "firstName", "whitespace-only first name")
    rejected("R05", external(cfg, letters(6), email="not-an-email"), "email", "invalid email")
    rejected("R06", external(cfg, letters(6), options=["no-such-option-" + TAG]), "options", "unknown option")
    rejected("R07", external(cfg, letters(6), consents=[]), "consents", "mandatory consent missing")
    p = external(cfg, letters(6), captchaToken="wrong-token")
    st, js = register(p)
    stored = bool(export_rows_with(p["lastName"]))
    record("R08", st == 400 and not stored, f"rejected captcha token: status {st}, stored {stored}")

    msgs = mails(predicate=to_addr(f"{ext_last}@example.org"))
    text = message_text(message(msgs[0]["ID"])) if msgs else ""
    record("R09", len(msgs) == 1 and ext_last in text, f"participant confirmations {len(msgs)}, names participant {ext_last in text}")

    org = organizer_mails_mentioning(ext_last)
    atts = [a for m in org for a in json_attachments(m)]
    att_ok = any(isinstance(a, dict) and ext_last in json.dumps(a) for a in atts)
    record("R10", bool(org) and att_ok, f"organizer notifications {len(org)}, JSON attachment with the registration {att_ok}")

    st, header, rows = export()
    row = next((r for r in rows or [] if r.get("id") == ext_id), None)
    record("R11", ok(st) and header[:len(EXPORT_COLUMNS)] == EXPORT_COLUMNS and row is not None and row.get("lastName") == ext_last,
           f"status {st}, header {header[:len(EXPORT_COLUMNS)] == EXPORT_COLUMNS}, row found {row is not None}")
    st, _, _ = export(auth=False)
    record("R12", st == 401, f"export without credentials: status {st}")

    sl_last = "Šušteršič" + letters(3)
    st, js = register(external(cfg, sl_last, firstName="Žiga", organization="Čebelarstvo Ščurek", email=f"{TAG}sl@example.org"))
    rows = export_rows_with(sl_last)
    record("R13", st == 201 and bool(rows) and rows[0].get("organization") == "Čebelarstvo Ščurek" and rows[0].get("firstName") == "Žiga",
           f"Slovenian characters: status {st}, unchanged in export {bool(rows) and rows[0].get('organization') == 'Čebelarstvo Ščurek'}")

    pad_last = letters(6)
    st, js = register(external(cfg, "  " + pad_last + "  ", firstName="  Ana  ", organization=" Primer doo "))
    rows = export_rows_with(pad_last)
    record("R14", st == 201 and bool(rows) and rows[0].get("lastName") == pad_last and rows[0].get("firstName") == "Ana",
           f"surrounding spaces removed: status {st}, stored {rows[0].get('lastName') if rows else None!r}")
    return cfg


# ------------------------------------------------------------------ feature `search`
def search(params, auth=True):
    q = urllib.parse.urlencode({k: v for k, v in params.items() if v is not None})
    st, js, _ = http("GET", "/api/registrations" + ("?" + q if q else ""), headers=AUTH if auth else None)
    return st, js or {}


def feature_search(cfg):
    org_token = letters(5)
    ext_last, stu_last = letters(7), letters(7)
    opt_ext = options_for(cfg, "EXTERNAL")
    other = [o for o in opt_ext if o not in options_for(cfg, "STUDENT")[:1]] or opt_ext
    register(external(cfg, ext_last, organization="Firma " + org_token, options=other[:1]))
    register(student(cfg, stu_last, studyInstitution="Fakulteta " + org_token))

    st, js = search({})
    items = js.get("items", [])
    times = [i.get("registeredAt") or "" for i in items]
    fields = {"id", "type", "firstName", "lastName", "email", "organization", "studyInstitution", "options", "registeredAt"}
    record("S01", ok(st) and 0 < len(items) <= 20 and times == sorted(times, reverse=True) and all(fields <= set(i) for i in items)
           and isinstance(js.get("total"), int), f"status {st}, {len(items)} items, total {js.get('total')}, newest first {times == sorted(times, reverse=True)}")

    st, js = search({"text": ext_last.upper()})
    names = [i.get("lastName") for i in js.get("items", [])]
    st2, js2 = search({"text": org_token})
    found2 = {i.get("lastName") for i in js2.get("items", [])}
    record("S02", ok(st) and names == [ext_last] and {ext_last, stu_last} <= found2,
           f"upper-case last name -> {names}; organization/institution text -> {sorted(found2)}")

    st, js = search({"text": org_token, "type": "STUDENT"})
    record("S03", ok(st) and [i.get("lastName") for i in js.get("items", [])] == [stu_last], f"type STUDENT -> {[i.get('lastName') for i in js.get('items', [])]}")

    st, js = search({"text": org_token, "option": other[0]} if other else {})
    got = [i.get("lastName") for i in js.get("items", [])]
    record("S04", ok(st) and ext_last in got and all(other[0] in (i.get("options") or []) for i in js.get("items", [])),
           f"option {other[0] if other else None} -> {got}")

    st, js = search({"text": org_token, "type": "EXTERNAL", "option": other[0]})
    record("S05", ok(st) and [i.get("lastName") for i in js.get("items", [])] == [ext_last], f"text + type + option -> {[i.get('lastName') for i in js.get('items', [])]}")

    st, js = search({"text": "zz" + letters(8)})
    record("S06", ok(st) and js.get("items") == [] and js.get("total") == 0, f"no match: status {st}, items {js.get('items')}, total {js.get('total')}")

    page_token = letters(6)
    for _ in range(25):
        register(external(cfg, letters(6), organization="Stran " + page_token))
    st1, p1 = search({"text": page_token, "page": 1})
    st2, p2 = search({"text": page_token, "page": 2})
    record("S07", ok(st1) and ok(st2) and len(p1.get("items", [])) == 20 and len(p2.get("items", [])) == 5
           and p1.get("total") == 25 and p2.get("total") == 25, f"page 1 {len(p1.get('items', []))}, page 2 {len(p2.get('items', []))}, totals {p1.get('total')}/{p2.get('total')}")

    bad = {"type": {"type": "TEACHER"}, "option": {"option": "no-such-" + TAG}, "page": {"page": 0}, "text": {"text": "a" * 101}}
    res = {k: search(v) for k, v in bad.items()}
    record("S08", all(st == 400 and k in error_fields(js) for k, (st, js) in res.items()),
           ", ".join(f"{k}: {st} {sorted(error_fields(js))}" for k, (st, js) in res.items()))

    st, js = search({}, auth=False)
    record("S09", st == 401 and not js.get("items"), f"without credentials: status {st}")

    sl_last = "Šušteršič" + letters(4)
    register(external(cfg, sl_last, email=f"{TAG}s@example.org"))
    st, js = search({"text": sl_last})
    record("S10", ok(st) and [i.get("lastName") for i in js.get("items", [])] == [sl_last], f"Slovenian last name -> {[i.get('lastName') for i in js.get('items', [])]}")


# ------------------------------------------------------------------ feature `group`
def group_payload(cfg, n, org=None, **extra):
    opts = options_for(cfg, "EXTERNAL")
    p = {"contact": {"firstName": "Maja", "lastName": letters(6), "email": f"{TAG}contact{letters(3)}@example.org"},
         "organization": org or "Skupina " + letters(5),
         "members": [{"firstName": "Clan", "lastName": letters(6), "email": f"{TAG}m{i}{letters(3)}@example.org",
                      "options": opts[i % len(opts):i % len(opts) + 1] if opts else []} for i in range(n)],
         "consents": consents(cfg), "captchaToken": TOKEN}
    p.update(extra)
    return p


def group_register(payload):
    st, js, _ = http("POST", "/api/group-registrations", payload)
    return st, js or {}


def nothing_stored(payload):
    return not export_rows_with(payload["organization"])


def feature_group(cfg):
    p = group_payload(cfg, 3)
    st, js = group_register(p)
    ids = js.get("registrationIds") or []
    record("G01", st == 201 and bool(js.get("groupId")) and len(ids) == 3, f"group of 3: status {st}, groupId {js.get('groupId')}, ids {len(ids)}")

    st_e, header, rows = export()
    mine = [r for r in rows or [] if r.get("id") in ids]
    contact = p["contact"]
    row_ok = len(mine) == 3 and all(r.get("groupId") == js.get("groupId") and r.get("contactEmail") == contact["email"]
                                    and r.get("contactName") == f"{contact['firstName']} {contact['lastName']}"
                                    and r.get("organization") == p["organization"] and r.get("type") == "EXTERNAL" for r in mine)
    single = [r for r in rows or [] if r.get("id") not in ids and r.get("type") == "EXTERNAL"][:5]
    empty_ok = all(not r.get("groupId") and not r.get("contactEmail") for r in single)
    record("G02", ok(st_e) and header[:len(EXPORT_COLUMNS + GROUP_COLUMNS)] == EXPORT_COLUMNS + GROUP_COLUMNS and row_ok and empty_ok,
           f"export: header {header[:15] == EXPORT_COLUMNS + GROUP_COLUMNS}, 3 member rows with group and contact {row_ok}, individual rows empty {empty_ok}")

    member_mails = [len(mails(predicate=to_addr(m["email"]))) for m in p["members"]]
    contact_msgs = mails(predicate=to_addr(contact["email"]))
    ctext = message_text(message(contact_msgs[0]["ID"])) if contact_msgs else ""
    lists_all = all(m["lastName"] in ctext for m in p["members"])
    record("G03", member_mails == [1, 1, 1] and len(contact_msgs) == 1 and lists_all,
           f"member confirmations {member_mails}, contact confirmations {len(contact_msgs)}, lists every member {lists_all}")

    org = organizer_mails_mentioning(p["members"][0]["lastName"])
    atts = [a for a in json_attachments(org[0])] if org else []
    per_member = all(any(isinstance(a, dict) and m["lastName"] in json.dumps(a) for a in atts) for m in p["members"])
    # each organizer address must get exactly one notification for the group (one message to all, or one each)
    per_address = {}
    for m in org:
        for t in m.get("To") or []:
            a = (t.get("Address") or "").lower()
            per_address[a] = per_address.get(a, 0) + 1
    covered = all(per_address.get(a) == 1 for a in ORGANIZERS) if ORGANIZERS else len(org) == 1
    record("G04", covered and len(atts) == 3 and per_member,
           f"notifications per organizer address {per_address}, JSON attachments {len(atts)}, one per member {per_member}")

    def rejected(sc, payload, field_check, what):
        st, js = group_register(payload)
        stored = not nothing_stored(payload)
        record(sc, st == 400 and field_check(error_fields(js)) and not stored, f"{what}: status {st}, error fields {sorted(error_fields(js))}, stored {stored}")

    rejected("G05", group_payload(cfg, 1), lambda f: "members" in f, "1 member")
    rejected("G06", group_payload(cfg, 11), lambda f: "members" in f, "11 members")
    bad = group_payload(cfg, 3)
    bad["members"][1]["email"] = "not-an-email"
    rejected("G07", bad, lambda f: "members[1].email" in f, "invalid email of member 2")
    dup = group_payload(cfg, 3)
    dup["members"][2]["email"] = dup["members"][0]["email"].upper()
    rejected("G08", dup, lambda f: any(x.startswith("members") for x in f), "duplicate member email (other case)")
    badc = group_payload(cfg, 2)
    badc["contact"]["email"] = "not-an-email"
    rejected("G09", badc, lambda f: "contact.email" in f, "invalid contact email")
    rejected("G10", group_payload(cfg, 2, consents=[]), lambda f: True, "mandatory consent missing")
    rejected("G11", group_payload(cfg, 2, captchaToken="wrong-token"), lambda f: True, "rejected captcha token")


# ------------------------------------------------------------------ rate limit phase
def phase_ratelimit(feature):
    codes = [http("GET", "/api/form-config")[0] for _ in range(4)]
    record("R15", codes[:3] == [200, 200, 200] and codes[3] == 429, f"statuses {codes} (expected 200, 200, 200, 429)")


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")  # the Windows console code page cannot show č, š, ž
    args = sys.argv[1:]
    if not args or args[0] not in ("main", "ratelimit") or "--feature" not in args:
        print(__doc__)
        sys.exit(2)
    phase, feature = args[0], args[args.index("--feature") + 1]
    if feature not in ("none", "search", "group"):
        sys.exit("--feature must be none, search or group")
    st, _, err = http("GET", "/api/form-config")
    if st == 0:
        sys.exit(f"backend not reachable at {BASE}: {err}")
    print(f"tag {TAG}, feature {feature}, phase {phase}")
    if phase == "main":
        cfg = regression()
        if feature == "search":
            feature_search(cfg)
        elif feature == "group":
            feature_group(cfg)
    else:
        phase_ratelimit(feature)
    out = args[args.index("--out") + 1] if "--out" in args else f"oracle-{phase}.json"
    with open(out, "w", encoding="utf-8") as f:
        json.dump({"phase": phase, "feature": feature, "tag": TAG, "base_url": BASE, "results": R}, f, indent=2, ensure_ascii=False)
    print(f"written {out}")


if __name__ == "__main__":
    main()
