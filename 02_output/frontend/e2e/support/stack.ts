import { execFileSync } from "node:child_process";
import { inflateRawSync } from "node:zlib";
import path from "node:path";
import { expect, type APIRequestContext } from "@playwright/test";

export const MAILPIT_URL =
  process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";
const COMPOSE_FILE =
  process.env.E2E_COMPOSE_FILE ??
  path.resolve(process.cwd(), "../docker-compose.yml");

export function organizerCredentials(): { username: string; password: string } {
  const username = process.env.ORGANIZER_USERNAME;
  const password = process.env.ORGANIZER_PASSWORD;
  if (!username || !password) {
    throw new Error(
      "ORGANIZER_USERNAME and ORGANIZER_PASSWORD must be exported from .env",
    );
  }
  return { username, password };
}

export function basicAuth(): string {
  const { username, password } = organizerCredentials();
  return (
    "Basic " + Buffer.from(`${username}:${password}`, "utf8").toString("base64")
  );
}

/** A unique suffix so each test finds its own registration in the shared stack. */
export function unique(): string {
  return `${Date.now()}${Math.floor(Math.random() * 1000)}`;
}

function compose(args: string[]): string {
  return execFileSync("docker", ["compose", "-f", COMPOSE_FILE, ...args], {
    encoding: "utf8",
    env: {
      ...process.env,
      POSTGRES_PASSWORD: "unused",
      ORGANIZER_USERNAME: "unused",
      ORGANIZER_PASSWORD: "unused",
      ORGANIZER_EMAILS: "unused",
    },
  });
}

/** Runs a query in the stack's PostgreSQL; rows as arrays of column strings ("|" separated). */
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
    "-tA",
    "-F",
    "|",
    "-c",
    query,
  ]);
  return out
    .split("\n")
    .filter((line) => line.length > 0)
    .map((line) => line.split("|"));
}

/** Quotes a value for a SQL string literal. */
export function literal(value: string): string {
  return "'" + value.replaceAll("'", "''") + "'";
}

/** The JSON copy of a registration as stored in the backend container (BR-07). */
export function jsonCopy(id: string): string {
  if (!/^[0-9a-f-]{36}$/.test(id)) {
    throw new Error("not a registration id: " + id);
  }
  return compose([
    "exec",
    "-T",
    "backend",
    "cat",
    `/data/json-copies/${id}.json`,
  ]);
}

export interface MailMessage {
  ID: string;
  Subject: string;
  To: { Address: string }[];
  Text: string;
  Attachments: { PartID: string; FileName: string; ContentType: string }[];
}

/** Waits for messages to `address` that contain `marker` in their text and returns them in full. */
export async function mailsTo(
  request: APIRequestContext,
  address: string,
  marker: string,
  count = 1,
): Promise<MailMessage[]> {
  let found: MailMessage[] = [];
  await expect
    .poll(
      async () => {
        const search = await request.get(`${MAILPIT_URL}/api/v1/search`, {
          params: { query: `to:"${address}"`, limit: "200" },
        });
        const list = (await search.json()) as { messages: { ID: string }[] };
        const full: MailMessage[] = [];
        for (const m of list.messages ?? []) {
          const detail = (await (
            await request.get(`${MAILPIT_URL}/api/v1/message/${m.ID}`)
          ).json()) as MailMessage;
          if (detail.Text.includes(marker)) {
            full.push(detail);
          }
        }
        found = full;
        return full.length;
      },
      { timeout: 20_000, intervals: [500] },
    )
    .toBeGreaterThanOrEqual(count);
  return found;
}

export async function mailAttachment(
  request: APIRequestContext,
  messageId: string,
  partId: string,
): Promise<string> {
  const res = await request.get(
    `${MAILPIT_URL}/api/v1/message/${messageId}/part/${partId}`,
  );
  return (await res.body()).toString("utf8");
}

/** Reads one entry of a zip archive (an .xlsx workbook) through its central directory. */
export function zipEntry(zip: Buffer, name: string): string | undefined {
  let eocd = -1;
  for (let i = zip.length - 22; i >= Math.max(0, zip.length - 65557); i--) {
    if (zip.readUInt32LE(i) === 0x06054b50) {
      eocd = i;
      break;
    }
  }
  if (eocd < 0) {
    throw new Error("not a zip archive");
  }
  const entries = zip.readUInt16LE(eocd + 10);
  let p = zip.readUInt32LE(eocd + 16);
  for (let n = 0; n < entries; n++) {
    const method = zip.readUInt16LE(p + 10);
    const compressedSize = zip.readUInt32LE(p + 20);
    const nameLength = zip.readUInt16LE(p + 28);
    const extraLength = zip.readUInt16LE(p + 30);
    const commentLength = zip.readUInt16LE(p + 32);
    const localOffset = zip.readUInt32LE(p + 42);
    const entryName = zip.toString("utf8", p + 46, p + 46 + nameLength);
    if (entryName === name) {
      const dataStart =
        localOffset +
        30 +
        zip.readUInt16LE(localOffset + 26) +
        zip.readUInt16LE(localOffset + 28);
      const data = zip.subarray(dataStart, dataStart + compressedSize);
      return (method === 0 ? data : inflateRawSync(data)).toString("utf8");
    }
    p += 46 + nameLength + extraLength + commentLength;
  }
  return undefined;
}

function decodeXml(text: string): string {
  return text
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replaceAll("&quot;", '"')
    .replaceAll("&apos;", "'")
    .replaceAll("&amp;", "&");
}

/** Rows of the first worksheet as arrays of cell texts (shared or inline strings). */
export function workbookRows(xlsx: Buffer): string[][] {
  const shared = zipEntry(xlsx, "xl/sharedStrings.xml") ?? "";
  const strings = [...shared.matchAll(/<si>([\s\S]*?)<\/si>/g)].map((m) =>
    decodeXml(
      [...m[1].matchAll(/<t[^>]*>([\s\S]*?)<\/t>/g)].map((t) => t[1]).join(""),
    ),
  );
  const sheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
  if (sheet === undefined) {
    throw new Error("workbook has no first worksheet");
  }
  return [...sheet.matchAll(/<row[^>]*>([\s\S]*?)<\/row>/g)].map((row) =>
    [...row[1].matchAll(/<c([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)].map((cell) => {
      const attrs = cell[1];
      const body = cell[2] ?? "";
      if (/t="s"/.test(attrs)) {
        return strings[Number(/<v>(\d+)<\/v>/.exec(body)?.[1] ?? -1)] ?? "";
      }
      const inline = /<t[^>]*>([\s\S]*?)<\/t>/.exec(body);
      if (inline) {
        return decodeXml(inline[1]);
      }
      return decodeXml(/<v>([\s\S]*?)<\/v>/.exec(body)?.[1] ?? "");
    }),
  );
}
