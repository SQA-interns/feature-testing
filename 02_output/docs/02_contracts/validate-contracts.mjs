// Validates the phase 2 contracts: parses every JSON contract, resolves local $refs,
// and compares the OpenAPI document with project/02_design/api-contract.md (AR-09).
// Run from the repository root: node 02_output/docs/02_contracts/validate-contracts.mjs
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, "..", "..", "..");
const problems = [];
const check = (ok, message) => {
  if (!ok) problems.push(message);
};

const load = (name) => JSON.parse(readFileSync(join(here, name), "utf8"));

function resolveRefs(doc, name) {
  const walk = (node) => {
    if (Array.isArray(node)) return node.forEach(walk);
    if (node && typeof node === "object") {
      if (typeof node.$ref === "string" && node.$ref.startsWith("#/")) {
        let target = doc;
        for (const part of node.$ref.slice(2).split("/")) target = target?.[part];
        check(target !== undefined, `${name}: unresolved $ref ${node.$ref}`);
      }
      Object.values(node).forEach(walk);
    }
  };
  walk(doc);
}

const openapi = load("openapi.json");
const copySchema = load("registration-copy.schema.json");
const configSchema = load("conference-config.schema.json");
resolveRefs(openapi, "openapi.json");
resolveRefs(copySchema, "registration-copy.schema.json");
resolveRefs(configSchema, "conference-config.schema.json");

check(openapi.openapi?.startsWith("3."), "openapi.json: not an OpenAPI 3 document");

// Endpoints, methods and status codes fixed by api-contract.md
const expected = {
  "/api/form-config": { get: ["200"] },
  "/api/registrations": { post: ["201", "400"], get: ["200", "400", "401"] },
  "/api/export": { get: ["200", "401"] },
};
const contract = readFileSync(join(root, "01_input/01_project/02_design/api-contract.md"), "utf8");
for (const [path, methods] of Object.entries(expected)) {
  for (const [method, codes] of Object.entries(methods)) {
    check(
      contract.includes(`${method.toUpperCase()} ${path}`),
      `api-contract.md no longer names ${method} ${path}`,
    );
    const op = openapi.paths?.[path]?.[method];
    check(op, `openapi.json: missing ${method} ${path}`);
    for (const code of codes) check(op?.responses?.[code], `openapi.json: ${method} ${path} lacks ${code}`);
    if (method !== "get" || path !== "/api/form-config") {
      check(op?.responses?.["429"], `openapi.json: ${method} ${path} lacks 429`);
    }
  }
}
const documentedPaths = Object.keys(openapi.paths ?? {}).sort().join(",");
check(documentedPaths === Object.keys(expected).sort().join(","), "openapi.json: extra or missing paths");

// Field names
const schemas = openapi.components.schemas;
const requestFields = [
  "type", "firstName", "lastName", "email", "organization", "studyInstitution",
  "studyProgramme", "studentId", "options", "consents", "captchaToken",
];
check(
  Object.keys(schemas.RegistrationRequest.properties).join(",") === requestFields.join(","),
  "RegistrationRequest fields differ from api-contract.md",
);
const itemFields = ["id", "type", "firstName", "lastName", "email", "organization", "studyInstitution", "options", "registeredAt"];
check(Object.keys(schemas.SearchItem.properties).join(",") === itemFields.join(","), "SearchItem fields differ");
check(Object.keys(schemas.SearchResult.properties).join(",") === "total,page,items", "SearchResult fields differ");
check(Object.keys(schemas.FormOption.properties).join(",") === "id,name,category,availableTo", "FormOption fields differ");
check(Object.keys(schemas.FormConsent.properties).join(",") === "id,text,mandatory", "FormConsent fields differ");
check(schemas.RegistrationType.enum.join(",") === "EXTERNAL,STUDENT", "registration types differ");
check(schemas.OptionCategory.enum.join(",") === "WORKSHOP,EVENT,MEAL,OTHER", "option categories differ");
const params = openapi.paths["/api/registrations"].get.parameters.map((p) => p.name).join(",");
check(params === "text,type,option,page", "search parameters differ");
const exportColumns =
  "id, type, firstName, lastName, email, organization, studyInstitution, studyProgramme, studentId, options, consents, registeredAt";
check(
  openapi.paths["/api/export"].get.responses["200"].description.includes(exportColumns),
  "export columns differ",
);
check(
  contract.includes(exportColumns.split(", ").map((c) => "`" + c + "`").join(", ")),
  "api-contract.md export columns changed",
);

// The shipped conference configuration must satisfy its schema's structural rules
const shipped = join(root, "02_output/backend/src/main/resources/conference.json");
try {
  const config = JSON.parse(readFileSync(shipped, "utf8"));
  const idPattern = new RegExp(configSchema.$defs.id.pattern);
  const ids = new Set();
  for (const option of config.options) {
    check(idPattern.test(option.id) && !ids.has(option.id), `conference.json: bad or duplicate id ${option.id}`);
    ids.add(option.id);
    check(configSchema.$defs.option.properties.category.enum.includes(option.category), `conference.json: category ${option.category}`);
    check(typeof option.name === "string" && option.name.trim(), `conference.json: name of ${option.id}`);
  }
  check(config.consents.some((c) => c.id === "privacy" && c.mandatory === true), "conference.json: mandatory privacy consent missing");
} catch (error) {
  if (error.code !== "ENOENT") problems.push(`conference.json: ${error.message}`);
  else console.log("conference.json not written yet (phase 4); skipped");
}

if (problems.length) {
  console.error(problems.map((p) => `FAIL ${p}`).join("\n"));
  process.exit(1);
}
console.log("contracts valid: openapi.json, registration-copy.schema.json, conference-config.schema.json");
