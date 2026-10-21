import { describe, expect, it } from "vitest";
import { buildPayload } from "./payload";
import { EMPTY_VALUES, optionsFor, type FormConfig, type FormValues } from "./types";
import { EMAIL_PATTERN, MAX_EMAIL_LENGTH, MAX_TEXT_LENGTH, MESSAGES, validate } from "./validation";

const config: FormConfig = {
  options: [
    { id: "b", name: "B", category: "EVENT", availableTo: ["EXTERNAL", "STUDENT"] },
    { id: "a", name: "A", category: "WORKSHOP", availableTo: ["STUDENT"] },
  ],
  consents: [
    { id: "privacy", text: "P", mandatory: true },
    { id: "photos", text: "F", mandatory: false },
  ],
  captchaSiteKey: "",
  captchaTestMode: true,
};

function external(text: Partial<FormValues["text"]> = {}): FormValues {
  return {
    ...EMPTY_VALUES,
    type: "EXTERNAL",
    text: {
      ...EMPTY_VALUES.text,
      firstName: "Ana",
      lastName: "Novak",
      email: "ana@example.org",
      organization: "Org",
      ...text,
    },
    consents: ["privacy"],
    captchaToken: "test-pass",
  };
}

describe("validate", () => {
  it("accepts a complete external form", () => {
    expect(validate(external(), config)).toEqual({});
  });

  it("asks for a type before anything else", () => {
    expect(validate({ ...EMPTY_VALUES }, config)).toEqual({ type: MESSAGES.required });
  });

  it("requires every field of the type after trimming", () => {
    expect(validate(external({ firstName: "  ", organization: "" }), config)).toEqual({
      firstName: MESSAGES.required,
      organization: MESSAGES.required,
    });
  });

  it("checks only the fields of the chosen type", () => {
    const student: FormValues = {
      ...external(),
      type: "STUDENT",
      text: {
        ...external().text,
        organization: "",
        studyInstitution: "UL",
        studyProgramme: "CS",
        studentId: "",
      },
    };
    expect(validate(student, config)).toEqual({ studentId: MESSAGES.required });
  });

  it("limits text by characters, not UTF-16 units", () => {
    expect(validate(external({ lastName: "😀".repeat(MAX_TEXT_LENGTH) }), config)).toEqual({});
    expect(validate(external({ lastName: "Ž".repeat(MAX_TEXT_LENGTH + 1) }), config)).toEqual({
      lastName: MESSAGES.tooLong,
    });
  });

  it("limits the email to 254 characters", () => {
    const domain = "@" + "d".repeat(60) + ".si";
    const local = "a".repeat(MAX_EMAIL_LENGTH - domain.length);
    expect(validate(external({ email: local + domain }), config)).toEqual({});
    expect(validate(external({ email: "a" + local + domain }), config)).toEqual({
      email: MESSAGES.tooLong,
    });
  });

  it.each(["ana", "ana@", "@x.si", "ana@x", "a b@x.si", "ana@x..si", "ana@-x.si"])(
    "rejects the email %s",
    (email) => {
      expect(validate(external({ email }), config)).toEqual({ email: MESSAGES.email });
    },
  );

  it("accepts a trimmed valid email", () => {
    expect(validate(external({ email: "  a.b+c@sub.example.org " }), config)).toEqual({});
    expect(EMAIL_PATTERN.test("a.b+c@sub.example.org")).toBe(true);
  });

  it("requires mandatory consents only", () => {
    expect(validate({ ...external(), consents: ["photos"] }, config)).toEqual({
      consents: MESSAGES.consent,
    });
  });

  it("requires a captcha token", () => {
    expect(validate({ ...external(), captchaToken: "" }, config)).toEqual({
      captchaToken: MESSAGES.captcha,
    });
  });
});

describe("buildPayload and optionsFor", () => {
  it("sends trimmed fields of the type with options and consents in configuration order", () => {
    const values = {
      ...external({ firstName: "  Ana ", organization: " Org " }),
      type: "EXTERNAL" as const,
      options: ["a", "b", "gone"],
      consents: ["photos", "privacy"],
    };

    expect(buildPayload(values, config)).toEqual({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana@example.org",
      organization: "Org",
      options: ["b", "a"],
      consents: ["privacy", "photos"],
      captchaToken: "test-pass",
    });
  });

  it("offers each type only its options", () => {
    expect(optionsFor(config, "EXTERNAL").map((o) => o.id)).toEqual(["b"]);
    expect(optionsFor(config, "STUDENT").map((o) => o.id)).toEqual(["b", "a"]);
  });
});

describe("email pattern boundaries", () => {
  it.each(["x@a.si", "a@b.c", "ana@example.org"])("accepts %s", (email) => {
    expect(EMAIL_PATTERN.test(email)).toBe(true);
  });

  it.each(["ana@example.org,", "ana@example.org x", "ana@example.org-"])("rejects %s", (email) => {
    expect(EMAIL_PATTERN.test(email)).toBe(false);
  });
});
