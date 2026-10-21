/// <reference types="node" />
import { inflateRawSync } from "node:zlib";

/** Reads the entries of a zip archive (stored or deflated), as written by Apache POI. */
export function unzip(archive: Buffer): Map<string, Buffer> {
  let end = -1;
  for (let i = archive.length - 22; i >= 0; i--) {
    if (archive.readUInt32LE(i) === 0x06054b50) {
      end = i;
      break;
    }
  }
  if (end < 0) throw new Error("Not a zip archive");
  const count = archive.readUInt16LE(end + 10);
  let offset = archive.readUInt32LE(end + 16);
  const entries = new Map<string, Buffer>();
  for (let n = 0; n < count; n++) {
    if (archive.readUInt32LE(offset) !== 0x02014b50) throw new Error("Bad central directory");
    const method = archive.readUInt16LE(offset + 10);
    const compressedSize = archive.readUInt32LE(offset + 20);
    const nameLength = archive.readUInt16LE(offset + 28);
    const extraLength = archive.readUInt16LE(offset + 30);
    const commentLength = archive.readUInt16LE(offset + 32);
    const localOffset = archive.readUInt32LE(offset + 42);
    const name = archive.toString("utf8", offset + 46, offset + 46 + nameLength);
    const localName = archive.readUInt16LE(localOffset + 26);
    const localExtra = archive.readUInt16LE(localOffset + 28);
    const start = localOffset + 30 + localName + localExtra;
    const data = archive.subarray(start, start + compressedSize);
    entries.set(name, method === 8 ? inflateRawSync(data) : Buffer.from(data));
    offset += 46 + nameLength + extraLength + commentLength;
  }
  return entries;
}

function decode(xml: string): string {
  return xml
    .replace(/&#x([0-9a-fA-F]+);/g, (_, h: string) => String.fromCodePoint(parseInt(h, 16)))
    .replace(/&#(\d+);/g, (_, d: string) => String.fromCodePoint(parseInt(d, 10)))
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, "&");
}

function texts(xml: string): string {
  return [...xml.matchAll(/<t\b[^>]*>([\s\S]*?)<\/t>/g)].map((m) => decode(m[1])).join("");
}

function columnIndex(reference: string): number {
  const letters = reference.replace(/\d+$/, "");
  let index = 0;
  for (const ch of letters) index = index * 26 + (ch.charCodeAt(0) - 64);
  return index - 1;
}

/** The first worksheet as rows of cell texts; a missing cell reads as "". */
export function readFirstSheet(xlsx: Buffer): string[][] {
  const entries = unzip(xlsx);
  const shared = entries.get("xl/sharedStrings.xml")?.toString("utf8") ?? "";
  const strings = [...shared.matchAll(/<si>([\s\S]*?)<\/si>/g)].map((m) => texts(m[1]));
  const sheetName = [...entries.keys()]
    .filter((k) => /^xl\/worksheets\/sheet\d+\.xml$/.test(k))
    .sort()[0];
  if (!sheetName) throw new Error("Workbook has no worksheet");
  const sheet = entries.get(sheetName)!.toString("utf8");
  const rows: string[][] = [];
  for (const row of sheet.matchAll(/<row\b[^>]*?(?:\/>|>([\s\S]*?)<\/row>)/g)) {
    const values: string[] = [];
    for (const cell of (row[1] ?? "").matchAll(/<c\b([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
      const attrs = cell[1];
      const body = cell[2] ?? "";
      const ref = /\br="([A-Z]+\d+)"/.exec(attrs)?.[1];
      const kind = /\bt="(\w+)"/.exec(attrs)?.[1];
      const raw = /<v>([\s\S]*?)<\/v>/.exec(body)?.[1];
      let value = "";
      if (kind === "s" && raw !== undefined) value = strings[Number(raw)] ?? "";
      else if (kind === "inlineStr") value = texts(body);
      else if (raw !== undefined) value = decode(raw);
      const index = ref ? columnIndex(ref) : values.length;
      while (values.length < index) values.push("");
      values[index] = value;
    }
    if (values.some((v) => v !== "")) rows.push(values);
  }
  return rows;
}

/** Data rows keyed by the header row. */
export function readRecords(xlsx: Buffer): { header: string[]; records: Record<string, string>[] } {
  const [header, ...data] = readFirstSheet(xlsx);
  const records = data.map((row) =>
    Object.fromEntries(header.map((name, i) => [name, row[i] ?? ""])),
  );
  return { header, records };
}
