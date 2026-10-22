// Validates the phase 2 contracts: JSON parse, local $ref resolution, and the
// fixed paths, methods, status codes and export columns of api-contract.md (AR-09).
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const errors = [];

function load(name) {
  try {
    return JSON.parse(readFileSync(join(here, name), "utf8"));
  } catch (e) {
    errors.push(`${name}: ${e.message}`);
    return null;
  }
}

function checkRefs(name, doc) {
  const walk = (node, path) => {
    if (Array.isArray(node)) node.forEach((v, i) => walk(v, `${path}/${i}`));
    else if (node && typeof node === "object") {
      for (const [k, v] of Object.entries(node)) {
        if (k === "$ref") {
          if (!v.startsWith("#/")) errors.push(`${name}${path}: non-local $ref ${v}`);
          else {
            let target = doc;
            for (const part of v.slice(2).split("/")) target = target?.[part];
            if (target === undefined) errors.push(`${name}${path}: unresolved $ref ${v}`);
          }
        } else walk(v, `${path}/${k}`);
      }
    }
  };
  walk(doc, "");
}

const api = load("openapi.json");
const copy = load("registration-copy.schema.json");
const config = load("conference-config.schema.json");

if (api) {
  checkRefs("openapi.json", api);
  if (!/^3\.1\./.test(api.openapi)) errors.push("openapi.json: not OpenAPI 3.1");
  const expected = {
    "/api/form-config": { get: ["200", "429"] },
    "/api/registrations": {
      post: ["201", "400", "429"],
      get: ["200", "400", "401", "429"],
    },
    "/api/export": { get: ["200", "401", "429"] },
  };
  const paths = Object.keys(api.paths ?? {});
  for (const p of paths) if (!expected[p]) errors.push(`openapi.json: path not in api-contract.md: ${p}`);
  for (const [p, methods] of Object.entries(expected)) {
    for (const [m, codes] of Object.entries(methods)) {
      const op = api.paths?.[p]?.[m];
      if (!op) {
        errors.push(`openapi.json: missing ${m.toUpperCase()} ${p}`);
        continue;
      }
      for (const c of codes) if (!op.responses?.[c]) errors.push(`openapi.json: ${m} ${p} lacks ${c}`);
    }
  }
  const params = (api.paths?.["/api/registrations"]?.get?.parameters ?? []).map((x) => x.name);
  for (const q of ["text", "type", "option", "page"])
    if (!params.includes(q)) errors.push(`openapi.json: search lacks parameter ${q}`);
  const req = api.components?.schemas?.RegistrationRequest?.properties ?? {};
  for (const f of [
    "type",
    "firstName",
    "lastName",
    "email",
    "organization",
    "studyInstitution",
    "studyProgramme",
    "studentId",
    "options",
    "consents",
    "captchaToken",
  ])
    if (!req[f]) errors.push(`openapi.json: RegistrationRequest lacks ${f}`);
  const columns =
    "id, type, firstName, lastName, email, organization, studyInstitution, studyProgramme, studentId, options, consents, registeredAt";
  if (!api.paths?.["/api/export"]?.get?.responses?.["200"]?.description?.includes(columns))
    errors.push("openapi.json: export columns differ from api-contract.md");
}
for (const [name, schema] of [
  ["registration-copy.schema.json", copy],
  ["conference-config.schema.json", config],
]) {
  if (!schema) continue;
  checkRefs(name, schema);
  if (schema.$schema !== "https://json-schema.org/draft/2020-12/schema")
    errors.push(`${name}: not JSON Schema 2020-12`);
  for (const r of schema.required ?? [])
    if (!schema.properties?.[r]) errors.push(`${name}: required ${r} has no property`);
}

if (errors.length) {
  console.error(errors.join("\n"));
  process.exit(1);
}
console.log("contracts valid: openapi.json, registration-copy.schema.json, conference-config.schema.json");
