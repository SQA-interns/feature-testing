// Helpers for the end-to-end tests: they drive the browser against the local stack
// (`docker compose up` in 02_output) and look into PostgreSQL, the JSON copy volume and Mailpit.
import { execFileSync } from "node:child_process";
import path from "node:path";
import { inflateRawSync } from "node:zlib";
import { expect, type APIRequestContext, type Page } from "@playwright/test";

export const BACKEND_URL = process.env.E2E_BACKEND_URL ?? "http://127.0.0.1:8080";
export const MAILPIT_URL = process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";
const COMPOSE_FILE =
  process.env.E2E_COMPOSE_FILE ?? path.resolve(process.cwd(), "..", "docker-compose.yml");
const ENV_FILE = process.env.E2E_ENV_FILE ?? path.resolve(process.cwd(), "..", "..", ".env");

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

export const OPTION_GROUPS = ["Workshops", "Events", "Meals", "Other activities"];

export interface Person {
  firstName: string;
  lastName: string;
  email: string;
  organization?: string;
  studyInstitution?: string;
  studyProgramme?: string;
  studentId?: string;
}

/** A token unique to this test run, letters and digits only (safe in emails and searches). */
export function uniqueToken(prefix: string): string {
  return `${prefix}x${Date.now()}${Math.floor(Math.random() * 1000)}`;
}

export function organizerCredentials(): { username: string; password: string } {
  const username = process.env.ORGANIZER_USERNAME;
  const password = process.env.ORGANIZER_PASSWORD;
  if (!username || !password) {
    throw new Error("Set ORGANIZER_USERNAME and ORGANIZER_PASSWORD (from .env) for the e2e tests");
  }
  return { username, password };
}

export function basicAuth(): string {
  const { username, password } = organizerCredentials();
  return "Basic " + Buffer.from(`${username}:${password}`, "utf8").toString("base64");
}

function compose(...args: string[]): string {
  return execFileSync("docker", ["compose", "--env-file", ENV_FILE, "-f", COMPOSE_FILE, ...args], {
    encoding: "utf8",
  });
}

function checkedId(id: string): string {
  expect(id).toMatch(/^[0-9a-f-]{36}$/);
  return id;
}

/** Runs a query in the `db` service; columns separated by `|`, rows by newlines. */
export function sql(query: string): string {
  return compose(
    "exec",
    "-T",
    "db",
    "psql",
    "-U",
    "registration",
    "-d",
    "registration",
    "-tAc",
    query,
  ).trim();
}

export function registrationRow(id: string): string {
  return sql(
    "select type, first_name, last_name, email, coalesce(organization, ''), " +
      "coalesce(study_institution, '') from registration where id = '" +
      checkedId(id) +
      "'",
  );
}

/** The JSON copy of a registration, as stored on the backend's volume. */
export function jsonCopy(id: string): string {
  return compose("exec", "-T", "backend", "cat", `/data/json-copies/${checkedId(id)}.json`);
}

export async function chooseType(page: Page, label: "External participant" | "Student") {
  await page.getByRole("radio", { name: label }).check();
}

export async function fillPerson(page: Page, person: Person) {
  await page.getByLabel("First name", { exact: true }).fill(person.firstName);
  await page.getByLabel("Last name", { exact: true }).fill(person.lastName);
  await page.getByLabel("Email", { exact: true }).fill(person.email);
  const optional: [string, string | undefined][] = [
    ["Organization / institution", person.organization],
    ["Study institution", person.studyInstitution],
    ["Study programme", person.studyProgramme],
    ["Student ID", person.studentId],
  ];
  for (const [label, value] of optional) {
    if (value !== undefined) {
      await page.getByLabel(label, { exact: true }).fill(value);
    }
  }
}

async function labelOf(box: ReturnType<Page["getByRole"]>): Promise<string> {
  return box.evaluate((el) => ((el as HTMLInputElement).labels?.[0]?.textContent ?? "").trim());
}

/** Checks the first option of every shown group; returns the checked options' names. */
export async function checkFirstOptionPerGroup(page: Page): Promise<string[]> {
  const names: string[] = [];
  for (const legend of OPTION_GROUPS) {
    const group = page.getByRole("group", { name: legend });
    if ((await group.count()) === 0) continue;
    const box = group.getByRole("checkbox").first();
    names.push(await labelOf(box));
    await box.check();
  }
  return names;
}

/** Names of every option checkbox on the page. */
export async function shownOptionNames(page: Page): Promise<string[]> {
  const names: string[] = [];
  for (const legend of OPTION_GROUPS) {
    const boxes = page.getByRole("group", { name: legend }).getByRole("checkbox");
    for (let i = 0; i < (await boxes.count()); i++) {
      names.push(await labelOf(boxes.nth(i)));
    }
  }
  return names;
}

export async function checkMandatoryConsents(page: Page) {
  const boxes = page.getByRole("checkbox", { name: /\(required\)/ });
  expect(await boxes.count()).toBeGreaterThan(0);
  for (let i = 0; i < (await boxes.count()); i++) {
    await boxes.nth(i).check();
  }
}

/** Clicks Register, expects HTTP 201 and returns the new registration id. */
export async function submitAndGetId(page: Page): Promise<string> {
  const [response] = await Promise.all([
    page.waitForResponse(
      (r) => r.url().endsWith("/api/registrations") && r.request().method() === "POST",
    ),
    page.getByRole("button", { name: "Register" }).click(),
  ]);
  expect(response.status()).toBe(201);
  const body = (await response.json()) as { id: string };
  await expect(page.getByRole("heading", { name: "Registration received" })).toBeVisible();
  return body.id;
}

/** Registers through the page with one option per group and the mandatory consents. */
export async function registerViaPage(
  page: Page,
  type: "External participant" | "Student",
  person: Person,
): Promise<string> {
  await page.goto("/");
  await chooseType(page, type);
  await fillPerson(page, person);
  await checkFirstOptionPerGroup(page);
  await checkMandatoryConsents(page);
  return submitAndGetId(page);
}

export interface MailSummary {
  ID: string;
  To: { Address: string }[];
  Subject: string;
}

/** Waits up to 15 s for at least `count` messages matching `text` in Mailpit. */
export async function mailsMatching(
  request: APIRequestContext,
  text: string,
  count: number,
): Promise<MailSummary[]> {
  const deadline = Date.now() + 15_000;
  let messages: MailSummary[] = [];
  while (Date.now() < deadline) {
    const response = await request.get(
      `${MAILPIT_URL}/api/v1/search?query=${encodeURIComponent(text)}&limit=50`,
    );
    messages = ((await response.json()) as { messages: MailSummary[] }).messages ?? [];
    if (messages.length >= count) break;
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  return messages;
}

export interface MailMessage {
  ID: string;
  To: { Address: string }[];
  Subject: string;
  Text: string;
  HTML: string;
  Attachments: { PartID: string; FileName: string; ContentType: string }[];
}

export async function mailMessage(request: APIRequestContext, id: string): Promise<MailMessage> {
  return (await (await request.get(`${MAILPIT_URL}/api/v1/message/${id}`)).json()) as MailMessage;
}

export async function mailPart(request: APIRequestContext, id: string, partId: string) {
  return (await request.get(`${MAILPIT_URL}/api/v1/message/${id}/part/${partId}`)).body();
}

export function isAddressedTo(message: { To: { Address: string }[] }, address: string): boolean {
  return message.To.some((to) => to.Address.toLowerCase() === address.toLowerCase());
}

/** Rows of the first worksheet of an .xlsx file, as text (empty cells are ""). */
export function readWorkbook(file: Buffer): string[][] {
  const entries = unzip(file);
  const shared = [...(entries["xl/sharedStrings.xml"] ?? "").matchAll(/<si>([\s\S]*?)<\/si>/g)].map(
    (m) => textRuns(m[1]),
  );
  const sheet = entries["xl/worksheets/sheet1.xml"];
  if (!sheet) throw new Error("workbook has no first worksheet");
  const rows: string[][] = [];
  for (const row of sheet.matchAll(/<row\b[^>]*>([\s\S]*?)<\/row>/g)) {
    const cells: string[] = [];
    for (const cell of row[1].matchAll(/<c\b([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
      const attributes = cell[1];
      const column = columnIndex(/\br="([A-Z]+)\d+"/.exec(attributes)?.[1] ?? "A");
      const kind = /\bt="([a-zA-Z]+)"/.exec(attributes)?.[1];
      const inner = cell[2] ?? "";
      const value = /<v>([\s\S]*?)<\/v>/.exec(inner)?.[1];
      let text = "";
      if (kind === "s" && value !== undefined) text = shared[Number(value)] ?? "";
      else if (kind === "inlineStr") text = textRuns(inner);
      else if (value !== undefined) text = unescapeXml(value);
      while (cells.length < column) cells.push("");
      cells[column] = text;
    }
    rows.push(cells);
  }
  return rows;
}

function textRuns(xml: string): string {
  return [...xml.matchAll(/<t\b[^>]*>([\s\S]*?)<\/t>/g)].map((m) => unescapeXml(m[1])).join("");
}

function columnIndex(letters: string): number {
  return [...letters].reduce((n, ch) => n * 26 + ch.charCodeAt(0) - 64, 0) - 1;
}

function unescapeXml(text: string): string {
  return text
    .replace(/&#x([0-9a-fA-F]+);/g, (_, hex: string) => String.fromCodePoint(parseInt(hex, 16)))
    .replace(/&#(\d+);/g, (_, dec: string) => String.fromCodePoint(Number(dec)))
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, "&");
}

/** Minimal ZIP reader (stored and deflated entries), enough for an .xlsx file. */
function unzip(file: Buffer): Record<string, string> {
  let end = file.length - 22;
  while (end >= 0 && file.readUInt32LE(end) !== 0x06054b50) end--;
  if (end < 0) throw new Error("not a ZIP file");
  const entries: Record<string, string> = {};
  let pointer = file.readUInt32LE(end + 16);
  const count = file.readUInt16LE(end + 10);
  for (let i = 0; i < count; i++) {
    if (file.readUInt32LE(pointer) !== 0x02014b50) throw new Error("bad ZIP central directory");
    const method = file.readUInt16LE(pointer + 10);
    const compressedSize = file.readUInt32LE(pointer + 20);
    const nameLength = file.readUInt16LE(pointer + 28);
    const extraLength = file.readUInt16LE(pointer + 30);
    const commentLength = file.readUInt16LE(pointer + 32);
    const localHeader = file.readUInt32LE(pointer + 42);
    const name = file.toString("utf8", pointer + 46, pointer + 46 + nameLength);
    const dataStart =
      localHeader + 30 + file.readUInt16LE(localHeader + 26) + file.readUInt16LE(localHeader + 28);
    const data = file.subarray(dataStart, dataStart + compressedSize);
    entries[name] = (method === 0 ? data : inflateRawSync(data)).toString("utf8");
    pointer += 46 + nameLength + extraLength + commentLength;
  }
  return entries;
}
