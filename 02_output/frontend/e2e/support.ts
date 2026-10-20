// Helpers for the end-to-end tests: they observe the running local stack (docker-compose.yml)
// through its public surfaces only: the browser, the REST API, PostgreSQL (psql in the postgres
// container), the JSON copy volume (in the backend container) and the Mailpit API.
import { execFileSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { inflateRawSync } from "node:zlib";
import { expect, type APIRequestContext, type Page } from "@playwright/test";

export const BACKEND_URL = process.env.E2E_BACKEND_URL ?? "http://127.0.0.1:8080";
export const MAILPIT_URL = process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";
const COMPOSE_FILE = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "../../docker-compose.yml",
);

export const XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
export const GROUPS = ["Workshops", "Events", "Meals", "Other activities"];
export const LABELS = {
  firstName: "First name",
  lastName: "Last name",
  email: "Email",
  organization: "Organization / institution",
  studyInstitution: "Study institution",
  studyProgramme: "Study programme",
  studentId: "Student ID",
};

export function uniqueEmail(kind: string): string {
  return `e2e-${kind}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.org`;
}

function startsWith(text: string): RegExp {
  return new RegExp("^" + text.replace(/[.*+?^${}()|[\]\\/]/g, "\\$&"));
}

export function field(page: Page, label: string) {
  return page.getByLabel(startsWith(label));
}

export function group(page: Page, name: string) {
  return page.getByRole("group", { name: startsWith(name) });
}

/** Ticks the first option of every category group and returns the ticked option names. */
export async function tickOnePerCategory(page: Page): Promise<string[]> {
  const names: string[] = [];
  for (const name of GROUPS) {
    const box = group(page, name).getByRole("checkbox").first();
    await box.check();
    const id = await box.getAttribute("id");
    const label = id ? page.locator(`label[for="${id}"]`) : box.locator("xpath=ancestor::label");
    names.push(((await label.textContent()) ?? "").trim());
  }
  return names;
}

export async function tickMandatoryConsentAndCaptcha(page: Page): Promise<void> {
  await page.getByRole("checkbox", { name: /^I agree that my personal data/ }).check();
  await page.getByRole("checkbox", { name: /test mode/i }).check();
}

/** Clicks Register and returns the HTTP status and body of the registration request. */
export async function submitAndCapture(page: Page): Promise<{ status: number; body: unknown }> {
  const response = page.waitForResponse(
    (r) => r.url().endsWith("/api/registrations") && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Register" }).click();
  const r = await response;
  return { status: r.status(), body: await r.json() };
}

// ---- PostgreSQL ----

function compose(args: string[]): Buffer {
  return execFileSync("docker", ["compose", "-f", COMPOSE_FILE, ...args], {
    maxBuffer: 32 * 1024 * 1024,
  });
}

/** Rows of a query as arrays of column strings (psql unaligned, tab separated). */
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
    "-v",
    "ON_ERROR_STOP=1",
    "-c",
    query,
  ]).toString("utf8");
  return out
    .split("\n")
    .map((l) => l.replace(/\r$/, ""))
    .filter((l) => l.length > 0)
    .map((l) => l.split("\t"));
}

export function assertUuid(id: string): string {
  expect(id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i);
  return id;
}

// ---- JSON copy volume ----

export function jsonCopy(id: string): Buffer {
  return compose(["exec", "-T", "backend", "cat", `/data/json-copies/${assertUuid(id)}.json`]);
}

// ---- Mailpit ----

interface Address {
  Address: string;
}
interface Addressed {
  ID: string;
  To: Address[] | null;
  Cc: Address[] | null;
  Bcc: Address[] | null;
  Subject: string;
}
/** Entry of GET /api/v1/messages. */
export interface MailSummary extends Addressed {
  Attachments: number;
}
/** GET /api/v1/message/{ID}. */
export interface Mail extends Addressed {
  Text: string;
  HTML: string;
  Attachments: { PartID: string; FileName: string; ContentType: string }[];
}

async function mailpitJson<T>(request: APIRequestContext, p: string): Promise<T> {
  const r = await request.get(MAILPIT_URL + p);
  expect(r.status()).toBe(200);
  return (await r.json()) as T;
}

export function recipients(m: Addressed): string[] {
  return [...(m.To ?? []), ...(m.Cc ?? []), ...(m.Bcc ?? [])].map((a) => a.Address.toLowerCase());
}

export async function mailsTo(request: APIRequestContext, address: string): Promise<Mail[]> {
  const list = await mailpitJson<{ messages: MailSummary[] }>(
    request,
    "/api/v1/messages?limit=500",
  );
  const summaries = list.messages.filter((m) => recipients(m).includes(address.toLowerCase()));
  return Promise.all(summaries.map((m) => mailpitJson<Mail>(request, `/api/v1/message/${m.ID}`)));
}

/** Organizer notifications: messages with a JSON attachment whose content has the given id. */
export async function notificationsFor(
  request: APIRequestContext,
  id: string,
): Promise<{ mail: Mail; attachment: Buffer }[]> {
  const list = await mailpitJson<{ messages: MailSummary[] }>(
    request,
    "/api/v1/messages?limit=500",
  );
  const result: { mail: Mail; attachment: Buffer }[] = [];
  for (const summary of list.messages.filter((m) => m.Attachments > 0)) {
    const mail = await mailpitJson<Mail>(request, `/api/v1/message/${summary.ID}`);
    for (const a of mail.Attachments) {
      if (!a.ContentType.startsWith("application/json")) continue;
      const part = await request.get(
        `${MAILPIT_URL}/api/v1/message/${summary.ID}/part/${a.PartID}`,
      );
      const content = Buffer.from(await part.body());
      try {
        if ((JSON.parse(content.toString("utf8")) as { id?: string }).id === id) {
          result.push({ mail, attachment: content });
        }
      } catch {
        // not a registration copy
      }
    }
  }
  return result;
}

// ---- Export ----

export function organizerAuth(): string {
  const user = process.env.ORGANIZER_USERNAME;
  const password = process.env.ORGANIZER_PASSWORD;
  if (!user || !password) {
    throw new Error("Set ORGANIZER_USERNAME and ORGANIZER_PASSWORD (from .env) for the e2e run");
  }
  return "Basic " + Buffer.from(`${user}:${password}`, "utf8").toString("base64");
}

/** Entries of a zip archive (stored or deflated), by name. */
export function unzip(zip: Buffer): Map<string, Buffer> {
  let eocd = zip.length - 22;
  while (eocd >= 0 && zip.readUInt32LE(eocd) !== 0x06054b50) eocd--;
  if (eocd < 0) throw new Error("not a zip archive");
  const entries = zip.readUInt16LE(eocd + 10);
  let p = zip.readUInt32LE(eocd + 16);
  const files = new Map<string, Buffer>();
  for (let i = 0; i < entries; i++) {
    if (zip.readUInt32LE(p) !== 0x02014b50) throw new Error("bad central directory");
    const method = zip.readUInt16LE(p + 10);
    const size = zip.readUInt32LE(p + 20);
    const nameLength = zip.readUInt16LE(p + 28);
    const extraLength = zip.readUInt16LE(p + 30);
    const commentLength = zip.readUInt16LE(p + 32);
    const local = zip.readUInt32LE(p + 42);
    const name = zip.toString("utf8", p + 46, p + 46 + nameLength);
    const start = local + 30 + zip.readUInt16LE(local + 26) + zip.readUInt16LE(local + 28);
    const data = zip.subarray(start, start + size);
    files.set(name, method === 8 ? inflateRawSync(data) : Buffer.from(data));
    p += 46 + nameLength + extraLength + commentLength;
  }
  return files;
}

function xmlText(xml: string): string {
  return [...xml.matchAll(/<t(?:\s[^>]*)?>([\s\S]*?)<\/t>/g)]
    .map((m) => m[1])
    .join("")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, "&");
}

/** Rows of the first worksheet as arrays of cell texts (shared, inline and plain values). */
export function sheetRows(workbook: Buffer): string[][] {
  const files = unzip(workbook);
  expect(files.has("[Content_Types].xml")).toBe(true);
  expect(files.has("xl/workbook.xml")).toBe(true);
  const shared = [
    ...(files.get("xl/sharedStrings.xml")?.toString("utf8") ?? "").matchAll(
      /<si>([\s\S]*?)<\/si>/g,
    ),
  ].map((m) => xmlText(m[1]));
  const sheet = files.get("xl/worksheets/sheet1.xml")?.toString("utf8");
  if (!sheet) throw new Error("no first worksheet");
  return [...sheet.matchAll(/<row[^>]*?(?:\/>|>([\s\S]*?)<\/row>)/g)].map((row) => {
    const cells: string[] = [];
    for (const c of (row[1] ?? "").matchAll(/<c\s([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
      const ref = /r="([A-Z]+)\d+"/.exec(c[1])?.[1] ?? "";
      const column = [...ref].reduce((n, ch) => n * 26 + ch.charCodeAt(0) - 64, 0) - 1;
      const typeAttr = /t="([^"]+)"/.exec(c[1])?.[1];
      const inner = c[2] ?? "";
      const value = /<v>([\s\S]*?)<\/v>/.exec(inner)?.[1] ?? "";
      const text =
        typeAttr === "s"
          ? (shared[Number(value)] ?? "")
          : typeAttr === "inlineStr"
            ? xmlText(inner)
            : value;
      cells[column >= 0 ? column : cells.length] = text;
    }
    return Array.from(cells, (v) => v ?? "");
  });
}
