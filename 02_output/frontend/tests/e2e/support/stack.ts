import { expect, type APIRequestContext, type Locator, type Page } from "@playwright/test";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { inflateRawSync } from "node:zlib";

/** The local stack of api-contract.md ("Local stack"). */
export const BACKEND_URL = process.env.E2E_BACKEND_URL ?? "http://127.0.0.1:8080";
export const MAILPIT_URL = process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";
const COMPOSE_DIR = fileURLToPath(new URL("../../../../", import.meta.url));
const ENV_FILE = fileURLToPath(new URL("../../../../../.env", import.meta.url));

export function organizerCredentials(): { username: string; password: string } {
  const username = process.env.ORGANIZER_USERNAME;
  const password = process.env.ORGANIZER_PASSWORD;
  if (!username || !password) throw new Error("ORGANIZER_USERNAME/ORGANIZER_PASSWORD not set");
  return { username, password };
}

export function organizerEmails(): string[] {
  return (process.env.ORGANIZER_EMAILS ?? "")
    .split(",")
    .map((a) => a.trim().toLowerCase())
    .filter(Boolean);
}

export function basicAuth(): string {
  const { username, password } = organizerCredentials();
  return "Basic " + Buffer.from(`${username}:${password}`, "utf8").toString("base64");
}

/** A random lower-case token of letters, to tag the data of one test. */
export function token(): string {
  let t = "e2e";
  for (let i = 0; i < 10; i++) t += String.fromCharCode(97 + Math.floor(Math.random() * 26));
  return t;
}

// ------------------------------------------------------------------ registration form (UI contract)

export interface Participant {
  type: "External participant" | "Student";
  firstName: string;
  lastName: string;
  email: string;
  organization?: string;
  studyInstitution?: string;
  studyProgramme?: string;
  studentId?: string;
}

export const GROUPS = ["Workshops", "Events", "Meals", "Other activities"];

/** Text of the label of a form control (wrapping label, label[for] or aria-label). */
export async function accessibleLabel(control: Locator): Promise<string> {
  return control.evaluate((el) => {
    const label =
      el.closest("label") ?? (el.id ? document.querySelector(`label[for="${el.id}"]`) : null);
    return (label?.textContent ?? el.getAttribute("aria-label") ?? "").trim();
  });
}

/** Fills the form for the participant, selects the first option of every shown group and gives every consent. */
export async function fillRegistration(page: Page, p: Participant): Promise<string[]> {
  await page.goto("/");
  await page.getByRole("radio", { name: p.type }).check();
  await page.getByRole("textbox", { name: "First name", exact: true }).fill(p.firstName);
  await page.getByRole("textbox", { name: "Last name", exact: true }).fill(p.lastName);
  await page.getByRole("textbox", { name: "Email", exact: true }).fill(p.email);
  if (p.type === "External participant") {
    await page
      .getByRole("textbox", { name: "Organization / institution", exact: true })
      .fill(p.organization ?? "");
  } else {
    await page
      .getByRole("textbox", { name: "Study institution", exact: true })
      .fill(p.studyInstitution ?? "");
    await page
      .getByRole("textbox", { name: "Study programme", exact: true })
      .fill(p.studyProgramme ?? "");
    await page.getByRole("textbox", { name: "Student ID", exact: true }).fill(p.studentId ?? "");
  }
  const selected: string[] = [];
  for (const group of GROUPS) {
    const box = page.getByRole("group", { name: group, exact: true }).getByRole("checkbox").first();
    if ((await box.count()) > 0) {
      await box.check();
      selected.push(await accessibleLabel(box));
    }
  }
  const consents = page.getByRole("group", { name: "Consents", exact: true }).getByRole("checkbox");
  for (let i = 0; i < (await consents.count()); i++) await consents.nth(i).check();
  return selected;
}

export async function submitAndExpectConfirmation(page: Page, p: Participant): Promise<void> {
  await page.getByRole("button", { name: "Register" }).click();
  await expect(page.getByRole("heading", { name: "Registration received" })).toBeVisible();
  await expect(page.getByText(`Thank you, ${p.firstName} ${p.lastName}.`)).toBeVisible();
}

// ------------------------------------------------------------------ database and JSON copy (compose)

function compose(args: string[]): Buffer {
  return execFileSync("docker", ["compose", "--env-file", ENV_FILE, ...args], {
    cwd: COMPOSE_DIR,
    maxBuffer: 16 << 20,
  });
}

/** Runs a query in the PostgreSQL container; rows of "|"-separated columns. */
export function sql(query: string): string[][] {
  const out = compose([
    "exec",
    "-T",
    "-e",
    "PGCLIENTENCODING=UTF8",
    "postgres",
    "psql",
    "-U",
    "registration",
    "-d",
    "registration",
    "-t",
    "-A",
    "-F",
    "|",
    "-c",
    query,
  ]).toString("utf8");
  return out
    .split("\n")
    .map((l) => l.replace(/\r$/, ""))
    .filter((l) => l.length > 0)
    .map((l) => l.split("|"));
}

/** The registration id stored for an email address (ASCII), or undefined. */
export function registrationIdByEmail(email: string): string | undefined {
  const rows = sql(`select id from registration where email = '${email.replace(/'/g, "''")}'`);
  return rows[0]?.[0];
}

/** Bytes of the JSON copy of a registration from the backend container's JSON copy directory. */
export function jsonCopyBytes(id: string): Buffer {
  if (!/^[0-9a-f-]{36}$/.test(id)) throw new Error("not a registration id");
  return compose(["exec", "-T", "backend", "cat", `/data/json-copies/${id}.json`]);
}

// ------------------------------------------------------------------ Mailpit

interface MailSummary {
  ID: string;
  Subject: string;
  To: { Address: string }[];
}

export interface MailMessage extends MailSummary {
  Text: string;
  HTML: string;
  Attachments: { PartID: string; FileName: string; ContentType: string }[];
}

async function mailJson<T>(request: APIRequestContext, path: string): Promise<T> {
  const response = await request.get(MAILPIT_URL + path);
  expect(response.status()).toBe(200);
  return (await response.json()) as T;
}

/** Waits for a message to the address whose full content matches. */
export async function awaitMail(
  request: APIRequestContext,
  address: string,
  matches: (m: MailMessage) => boolean,
): Promise<MailMessage> {
  const deadline = Date.now() + 20_000;
  while (Date.now() < deadline) {
    const list = await mailJson<{ messages: MailSummary[] }>(request, "/api/v1/messages?limit=500");
    for (const s of list.messages) {
      if (!s.To.some((t) => t.Address.toLowerCase() === address.toLowerCase())) continue;
      const m = await mailJson<MailMessage>(request, `/api/v1/message/${s.ID}`);
      if (matches(m)) return m;
    }
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error(`no matching email to ${address}`);
}

export async function attachmentBytes(
  request: APIRequestContext,
  messageId: string,
  partId: string,
): Promise<Buffer> {
  const response = await request.get(`${MAILPIT_URL}/api/v1/message/${messageId}/part/${partId}`);
  expect(response.status()).toBe(200);
  return Buffer.from(await response.body());
}

// ------------------------------------------------------------------ backend API

export async function registerViaApi(
  request: APIRequestContext,
  body: Record<string, unknown>,
): Promise<string> {
  const response = await request.post(`${BACKEND_URL}/api/registrations`, {
    data: { captchaToken: "test-pass", consents: ["privacy"], options: [], ...body },
  });
  expect(response.status(), await response.text()).toBe(201);
  return ((await response.json()) as { id: string }).id;
}

// ------------------------------------------------------------------ minimal .xlsx reader

function unzip(buffer: Buffer): Map<string, Buffer> {
  const files = new Map<string, Buffer>();
  let eocd = -1;
  for (let i = buffer.length - 22; i >= 0; i--) {
    if (buffer.readUInt32LE(i) === 0x06054b50) {
      eocd = i;
      break;
    }
  }
  if (eocd < 0) throw new Error("not a zip file");
  const entries = buffer.readUInt16LE(eocd + 10);
  let p = buffer.readUInt32LE(eocd + 16);
  for (let e = 0; e < entries; e++) {
    if (buffer.readUInt32LE(p) !== 0x02014b50) throw new Error("bad central directory");
    const method = buffer.readUInt16LE(p + 10);
    const compressedSize = buffer.readUInt32LE(p + 20);
    const nameLength = buffer.readUInt16LE(p + 28);
    const extraLength = buffer.readUInt16LE(p + 30);
    const commentLength = buffer.readUInt16LE(p + 32);
    const localOffset = buffer.readUInt32LE(p + 42);
    const name = buffer.toString("utf8", p + 46, p + 46 + nameLength);
    const localNameLength = buffer.readUInt16LE(localOffset + 26);
    const localExtraLength = buffer.readUInt16LE(localOffset + 28);
    const start = localOffset + 30 + localNameLength + localExtraLength;
    const data = buffer.subarray(start, start + compressedSize);
    files.set(name, method === 8 ? inflateRawSync(data) : Buffer.from(data));
    p += 46 + nameLength + extraLength + commentLength;
  }
  return files;
}

function unescapeXml(s: string): string {
  return s
    .replace(/&#x([0-9a-fA-F]+);/g, (_, h) => String.fromCodePoint(parseInt(h, 16)))
    .replace(/&#(\d+);/g, (_, d) => String.fromCodePoint(parseInt(d, 10)))
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, "&");
}

function texts(xml: string): string {
  return [...xml.matchAll(/<t(?:\s[^>]*)?>([\s\S]*?)<\/t>/g)]
    .map((m) => unescapeXml(m[1]))
    .join("");
}

function columnIndex(ref: string): number {
  let n = 0;
  for (const ch of ref.replace(/\d+$/, "")) n = n * 26 + (ch.charCodeAt(0) - 64);
  return n - 1;
}

/** The first worksheet of an .xlsx as rows of cell texts (row 0 is the header). */
export function readXlsx(buffer: Buffer): string[][] {
  const files = unzip(buffer);
  const shared = files.get("xl/sharedStrings.xml")?.toString("utf8") ?? "";
  const strings = [...shared.matchAll(/<si>([\s\S]*?)<\/si>/g)].map((m) => texts(m[1]));
  const sheet = files.get("xl/worksheets/sheet1.xml")?.toString("utf8");
  if (!sheet) throw new Error("no first worksheet");
  const rows: string[][] = [];
  for (const row of sheet.matchAll(/<row[^>]*>([\s\S]*?)<\/row>/g)) {
    const cells: string[] = [];
    for (const c of row[1].matchAll(/<c\s([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
      const attrs = c[1];
      const inner = c[2] ?? "";
      const ref = /r="([A-Z]+\d+)"/.exec(attrs)?.[1];
      const type = /t="([^"]+)"/.exec(attrs)?.[1];
      const v = /<v>([\s\S]*?)<\/v>/.exec(inner)?.[1];
      let value = "";
      if (type === "s" && v !== undefined) value = strings[Number(v)] ?? "";
      else if (type === "inlineStr") value = texts(inner);
      else if (v !== undefined) value = unescapeXml(v);
      const index = ref ? columnIndex(ref) : cells.length;
      while (cells.length < index) cells.push("");
      cells[index] = value;
    }
    rows.push(cells);
  }
  return rows;
}

export const EXPORT_COLUMNS = [
  "id",
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
  "registeredAt",
];
