import { describe, expect, it } from "vitest";
import { MESSAGES } from "./messages";
import type { FormConsent } from "./types";
import { FIELDS, FIELDS_BY_TYPE, isValidEmail, validate } from "./validation";
import type { FormState } from "./validation";

const consents: FormConsent[] = [
  { id: "privacy", text: "Privacy", mandatory: true },
  { id: "photos", text: "Photos", mandatory: false },
];

const external = (
  values: FormState["values"] = {},
  overrides: Partial<FormState> = {},
): FormState => ({
  type: "EXTERNAL",
  values: {
    firstName: "Ana",
    lastName: "Novak",
    email: "ana@example.org",
    organization: "Institute",
    ...values,
  },
  consents: ["privacy"],
  captchaToken: "test-pass",
  ...overrides,
});

describe("isValidEmail", () => {
  it.each([
    "a@b.si",
    "ana.novak@example.org",
    "ana+tag@sub.example.co.uk",
    "o'neil@example.org",
    `${"a".repeat(64)}@example.org`,
  ])("accepts %s", (value) => {
    expect(isValidEmail(value)).toBe(true);
  });

  it.each([
    "",
    "ana",
    "@example.org",
    "ana@",
    "ana@@example.org",
    "ana@x@example.org",
    ".ana@example.org",
    "an..a@example.org",
    "ana novak@example.org",
    "ana@example",
    "ana@example.o",
    "ana@-host.example.org",
    "ana@example.org.",
    "xana@example.org,eve@example.org",
    `${"a".repeat(65)}@example.org`,
  ])("rejects %s", (value) => {
    expect(isValidEmail(value)).toBe(false);
  });
});

describe("validate", () => {
  it("accepts a complete external form", () => {
    expect(validate(external(), consents)).toEqual({});
  });

  it("checks only the fields of the chosen type", () => {
    expect(FIELDS_BY_TYPE.EXTERNAL).toEqual([
      "firstName",
      "lastName",
      "email",
      "organization",
    ]);
    const student: FormState = {
      type: "STUDENT",
      values: { firstName: "Ž", lastName: "K", email: "z@x.si" },
      consents: ["privacy"],
      captchaToken: "t",
    };
    expect(validate(student, consents)).toEqual({
      studyInstitution: MESSAGES.required,
      studyProgramme: MESSAGES.required,
      studentId: MESSAGES.required,
    });
  });

  it("reports missing and blank fields as required", () => {
    expect(
      validate(
        external({ firstName: "  ", organization: undefined }),
        consents,
      ),
    ).toEqual({
      firstName: MESSAGES.required,
      organization: MESSAGES.required,
    });
  });

  it("counts characters after trimming, by code point", () => {
    expect(FIELDS.firstName.max).toBe(200);
    expect(FIELDS.studentId.max).toBe(50);
    expect(FIELDS.email.max).toBe(254);
    expect(
      validate(external({ firstName: ` ${"😀".repeat(200)} ` }), consents),
    ).toEqual({});
    expect(validate(external({ lastName: "č".repeat(201) }), consents)).toEqual(
      {
        lastName: MESSAGES.tooLong,
      },
    );
    const student: FormState = {
      type: "STUDENT",
      values: {
        firstName: "a",
        lastName: "b",
        email: "a@b.si",
        studyInstitution: "x".repeat(200),
        studyProgramme: "y",
        studentId: "1".repeat(51),
      },
      consents: ["privacy"],
      captchaToken: "t",
    };
    expect(validate(student, consents)).toEqual({
      studentId: MESSAGES.tooLong,
    });
  });

  it("checks the email format after the length, on the trimmed value", () => {
    expect(
      validate(external({ email: " ana@example.org " }), consents),
    ).toEqual({});
    expect(validate(external({ email: "ana@example" }), consents)).toEqual({
      email: MESSAGES.email,
    });
    expect(
      validate(external({ email: `${"a".repeat(255)}@x.si` }), consents),
    ).toEqual({ email: MESSAGES.tooLong });
  });

  it("requires mandatory consents only", () => {
    expect(validate(external({}, { consents: [] }), consents)).toEqual({
      "consents.privacy": MESSAGES.consentRequired,
    });
    expect(
      validate(external({}, { consents: ["privacy", "photos"] }), consents),
    ).toEqual({});
  });

  it("requires a captcha token", () => {
    expect(validate(external({}, { captchaToken: "" }), consents)).toEqual({
      recaptchaToken: MESSAGES.captcha,
    });
  });
});
