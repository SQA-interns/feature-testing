/// <reference types="node" />
import { execFileSync } from "node:child_process";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

/**
 * Access to the running local stack (`02_output/docker-compose.yml`): PostgreSQL and the JSON copy
 * volume through `docker compose exec`, Mailpit through its API, and the backend API.
 * Organizer settings come from the environment (`.env`, exported by the e2e command).
 */
const here = dirname(fileURLToPath(import.meta.url));
const composeFile = process.env.E2E_COMPOSE_FILE ?? resolve(here, "../../../docker-compose.yml");

export const BACKEND_URL = process.env.E2E_BACKEND_URL ?? "http://127.0.0.1:8080";
export const MAILPIT_URL = process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";
export const GROUPS = ["Workshops", "Events", "Meals", "Other activities"];
export const CAPTCHA = "I am not a robot (test mode)";
export const XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

function required(name: string): string {
  const value = process.env[name];
  if (!value) throw new Error(`Environment variable ${name} is not set (export it from .env)`);
  return value;
}

export function organizerEmails(): string[] {
  return required("ORGANIZER_EMAILS")
    .split(",")
    .map((e) => e.trim())
    .filter((e) => e.length > 0);
}

export function organizerAuthorization(): string {
  const token = `${required("ORGANIZER_USERNAME")}:${required("ORGANIZER_PASSWORD")}`;
  return `Basic ${Buffer.from(token, "utf8").toString("base64")}`;
}

export function uniqueEmail(prefix: string): string {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1e6)}@example.org`;
}

function compose(args: string[]): string {
  return execFileSync("docker", ["compose", "-f", composeFile, ...args], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
}

function literal(value: string): string {
  return `'${value.replace(/'/g, "''")}'`;
}

/** Runs a query with psql and returns the rows as arrays of column texts. */
export function sql(query: string): string[][] {
  const out = compose([
    "exec",
    "-T",
    "postgres",
    "psql",
    "-U",
    "registration",
    "-d",
    "registration",
    "-At",
    "-F",
    "\t",
    "-c",
    query,
  ]);
  return out
    .split(/\r?\n/)
    .filter((line) => line.length > 0)
    .map((line) => line.split("\t"));
}

export interface StoredRegistration {
  id: string;
  type: string;
  firstName: string;
  lastName: string;
  email: string;
  organization: string;
  studyInstitution: string;
  studyProgramme: string;
  studentId: string;
}

/** The registration rows with this email (normally exactly one). */
export function registrationsByEmail(email: string): StoredRegistration[] {
  return sql(
    "select id, type, first_name, last_name, email, coalesce(organization, ''), " +
      "coalesce(study_institution, ''), coalesce(study_programme, ''), coalesce(student_id, '') " +
      `from registration where email = ${literal(email)}`,
  ).map(([id, type, firstName, lastName, mail, organization, institution, programme, student]) => ({
    id,
    type,
    firstName,
    lastName,
    email: mail,
    organization,
    studyInstitution: institution,
    studyProgramme: programme,
    studentId: student,
  }));
}

export function storedOptions(id: string): string[] {
  return sql(
    `select option_id from registration_option where registration_id = ${literal(id)} order by position`,
  ).map(([option]) => option);
}

export function registrationCount(): number {
  return Number(sql("select count(*) from registration")[0][0]);
}

/** The JSON copy of the registration, read from the backend container's volume. */
export function jsonCopy(id: string): Record<string, unknown> {
  if (!/^[0-9a-fA-F-]{36}$/.test(id)) throw new Error(`Not a registration id: ${id}`);
  return JSON.parse(compose(["exec", "-T", "backend", "cat", `/data/json-copies/${id}.json`]));
}

export interface Mail {
  id: string;
  to: string[];
  subject: string;
  text: string;
  attachments: { partId: string; fileName: string; contentType: string }[];
}

async function getJson(path: string): Promise<Record<string, unknown>> {
  const response = await fetch(MAILPIT_URL + path);
  if (!response.ok) throw new Error(`Mailpit ${path} answered ${response.status}`);
  return (await response.json()) as Record<string, unknown>;
}

function addresses(list: unknown): string[] {
  return Array.isArray(list) ? list.map((a) => String((a as { Address: string }).Address)) : [];
}

/** Every caught message addressed to the address whose text contains the marker. */
export async function mailsTo(address: string, marker = ""): Promise<Mail[]> {
  const page = await getJson("/api/v1/messages?limit=1000");
  const result: Mail[] = [];
  for (const summary of page.messages as Record<string, unknown>[]) {
    if (!addresses(summary.To).includes(address)) continue;
    const m = await getJson(`/api/v1/message/${String(summary.ID)}`);
    const mail: Mail = {
      id: String(m.ID),
      to: addresses(m.To),
      subject: String(m.Subject ?? ""),
      text: String(m.Text ?? ""),
      attachments: ((m.Attachments as Record<string, unknown>[]) ?? []).map((a) => ({
        partId: String(a.PartID),
        fileName: String(a.FileName),
        contentType: String(a.ContentType),
      })),
    };
    if (mail.text.includes(marker)) result.push(mail);
  }
  return result;
}

/** Waits up to 20 s until at least `count` messages to the address contain the marker. */
export async function awaitMailsTo(address: string, marker = "", count = 1): Promise<Mail[]> {
  const deadline = Date.now() + 20_000;
  for (;;) {
    const mails = await mailsTo(address, marker);
    if (mails.length >= count || Date.now() > deadline) return mails;
    await new Promise((r) => setTimeout(r, 500));
  }
}

export async function attachment(mail: Mail, partId: string): Promise<Buffer> {
  const response = await fetch(`${MAILPIT_URL}/api/v1/message/${mail.id}/part/${partId}`);
  if (!response.ok) throw new Error(`Mailpit attachment answered ${response.status}`);
  return Buffer.from(await response.arrayBuffer());
}

/** The JSON attachments of a message, parsed. */
export async function jsonAttachments(mail: Mail): Promise<Record<string, unknown>[]> {
  const parts = mail.attachments.filter(
    (a) => a.contentType.startsWith("application/json") || a.fileName.endsWith(".json"),
  );
  return Promise.all(
    parts.map(async (a) => JSON.parse((await attachment(mail, a.partId)).toString("utf8"))),
  );
}
