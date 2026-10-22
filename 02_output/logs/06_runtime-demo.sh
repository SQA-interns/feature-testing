#!/usr/bin/env bash
# Phase 6 runtime demonstration against the local stack. Credentials come from ../.env and are never printed.
set -u
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."
DC="docker compose --env-file ../.env"
U="$(sed -n 's/^ORGANIZER_USERNAME=//p' ../.env | tr -d '\r')"; P="$(sed -n 's/^ORGANIZER_PASSWORD=//p' ../.env | tr -d '\r')"
B=http://127.0.0.1:8080; T=$(date +%s)
code(){ curl -s -o /dev/null -w "%{http_code}" "$@"; }
echo "== NFR-04 health"; curl -s $B/actuator/health; echo; curl -s $B/actuator/health/readiness; echo; curl -s $B/actuator/health/liveness; echo
echo "== form-config: $(curl -s $B/api/form-config | head -c 160)..."
# bodies go through a UTF-8 file: Windows command-line arguments are not UTF-8
reg(){ printf '%s' "$1" > .demo-body.json; curl -s -X POST $B/api/registrations -H 'Content-Type: application/json; charset=utf-8' --data-binary @.demo-body.json; }
E=$(reg '{"type":"EXTERNAL","firstName":"Demo","lastName":"External'$T'","email":"ext'$T'@example.org","organization":"Demo d.o.o.","options":["ws-ai","ev-opening","meal-lunch-1","other-tour"],"consents":["privacy"],"captchaToken":"test-pass"}')
S=$(reg '{"type":"STUDENT","firstName":"Čedomir","lastName":"Študent'$T'","email":"stu'$T'@example.org","studyInstitution":"Univerza v Ljubljani","studyProgramme":"Računalništvo","studentId":"6320","options":["ev-students"],"consents":["privacy"],"captchaToken":"test-pass"}')
echo "== DoD-P01 external -> $E ; student -> $S"
EID=$(echo "$E" | sed 's/.*"id":"\([^"]*\)".*/\1/'); SID=$(echo "$S" | sed 's/.*"id":"\([^"]*\)".*/\1/')
echo "== DoD-P02 database rows"; $DC exec -T -e PGCLIENTENCODING=UTF8 postgres psql -U registration -d registration -t -A -c "select id,type,first_name,last_name,registered_at from registration where id in ('$EID','$SID') order by seq"
echo "   options/consents: $($DC exec -T postgres psql -U registration -d registration -t -A -c "select count(*) from registration_option where registration_id in ('$EID','$SID')") options, $($DC exec -T postgres psql -U registration -d registration -t -A -c "select count(*) from registration_consent where registration_id in ('$EID','$SID')") consents"
echo "   JSON copies:"; $DC exec -T backend ls -l /data/json-copies/$EID.json /data/json-copies/$SID.json
sleep 2
echo "== DoD-P03 Mailpit"; curl -s "http://127.0.0.1:8025/api/v1/messages?limit=50" | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{const ids=process.argv.slice(1);for(const m of JSON.parse(s).messages){if(ids.some(i=>m.Subject.includes(i))||m.To.some(t=>/(ext|stu)'$T'@/.test(t.Address)))console.log("  ",m.To.map(t=>t.Address.replace(/^[^@]+/,"***")).join(","),"|",m.Subject,"| attachments:",m.Attachments)}})' "$EID" "$SID"
echo "== DoD-P04 export"; curl -s -u "$U:$P" -o .demo-export.xlsx -w "  with credentials: %{http_code} %{content_type} %{size_download} bytes\n" $B/api/export; echo "  zip signature: $(head -c 2 .demo-export.xlsx)"; echo "  without credentials: $(code $B/api/export)"; echo "  wrong password: $(code -u "$U:wrong" $B/api/export)"
echo "== DoD-P06 search"; echo "  by last name: $(curl -s -u "$U:$P" "$B/api/registrations?text=External$T" | head -c 200)"; echo "  by type STUDENT, total/first type: $(curl -s -u "$U:$P" "$B/api/registrations?type=STUDENT" | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{const r=JSON.parse(s);console.log(r.total, [...new Set(r.items.map(i=>i.type))].join(","))})')"; echo "  without credentials: $(code "$B/api/registrations?text=External$T")"; echo "  invalid page: $(curl -s -u "$U:$P" "$B/api/registrations?page=0")"
echo "== SB-10 headers backend"; curl -s -D - -o /dev/null $B/api/form-config | grep -iE "content-security|x-frame|x-content-type|referrer|cache-control"
echo "== SB-10 headers frontend"; curl -s -D - -o /dev/null http://127.0.0.1:8081/ | grep -iE "content-security|x-frame|x-content-type|referrer"
echo "== frontend proxy /api with a forged Host header: $(code -H 'Host: evil.example' http://127.0.0.1:8081/api/form-config)"
echo "== SR-03 size limit: $(head -c 20000 /dev/zero | tr '\0' 'a' | curl -s -o /dev/null -w '%{http_code}' -X POST $B/api/registrations -H 'Content-Type: application/json' --data-binary @-)"
echo "== SR-03 rate limit (60/min, /api/form-config):"; n=0; for i in $(seq 1 70); do c=$(code $B/api/form-config); [ "$c" = 429 ] && { n=$((n+1)); }; done; echo "  429 answers among 70 requests: $n"; curl -s -D - -o /dev/null $B/api/form-config | grep -iE "^HTTP|retry-after"
echo "== SB-11 users: backend $($DC exec -T backend id -un) / frontend $($DC exec -T frontend id -un)"
echo "== NFR-02 recreate all containers"; ROWS=$($DC exec -T postgres psql -U registration -d registration -t -A -c "select count(*) from registration")
$DC down >/dev/null 2>&1; $DC up -d >/dev/null 2>&1
for i in $(seq 1 40); do $DC ps --format '{{.Service}}={{.Health}}' | grep -q "backend=healthy" && break; sleep 5; done
echo "  rows before: $ROWS, after: $($DC exec -T postgres psql -U registration -d registration -t -A -c "select count(*) from registration")"; echo "  JSON copy after: $($DC exec -T backend ls /data/json-copies/$EID.json)"
echo "  search after recreation: $(curl -s -u "$U:$P" "$B/api/registrations?text=External$T" | head -c 80)"
rm -f .demo-body.json .demo-export.xlsx
