import { describe, expect, it } from "vitest";
import { isOrganizerPath } from "./routes";

describe("isOrganizerPath", () => {
  it.each(["/organizer", "/organizer/"])("accepts %s", (path) => {
    expect(isOrganizerPath(path)).toBe(true);
  });

  it.each(["/", "", "/organizers", "/organizer/x", "/Organizer"])(
    "rejects %s",
    (path) => {
      expect(isOrganizerPath(path)).toBe(false);
    },
  );
});
