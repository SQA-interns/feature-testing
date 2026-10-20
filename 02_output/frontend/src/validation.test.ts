import { describe, expect, it } from "vitest";
import { MESSAGES, fieldsOf, validate, type FormInput } from "./validation";
import type { FormConsent } from "./types";

const consents: FormConsent[] = [
  { id: "privacy", text: "p", mandatory: true },
  { id: "photos", text: "f", mandatory: false },
];

function external(values: FormInput["values"], extra: Partial<FormInput> = {}): FormInput {
  return {
    type: "EXTERNAL",
    values: {
      firstName: "Ana",
      lastName: "Novak",
      email: "a@b.org",
      organization: "Org",
      ...values,
    },
    consents: new Set(["privacy"]),
    captchaToken: "t",
    ...extra,
  };
}

describe("fieldsOf", () => {
  it("lists the fixed fields of each type", () => {
    expect(fieldsOf("EXTERNAL")).toEqual(["firstName", "lastName", "email", "organization"]);
    expect(fieldsOf("STUDENT")).toEqual([
      "firstName",
      "lastName",
      "email",
      "studyInstitution",
      "studyProgramme",
      "studentId",
    ]);
  });
});

describe("validate", () => {
  it("accepts a complete form", () => {
    expect(validate(external({}), consents)).toEqual({});
  });

  it("requires every field of the type after trimming", () => {
    expect(validate(external({ firstName: "  ", organization: undefined }), consents)).toEqual({
      firstName: MESSAGES.required,
      organization: MESSAGES.required,
    });
  });

  it("checks only the fields of the chosen type", () => {
    const student: FormInput = {
      type: "STUDENT",
      values: { firstName: "L", lastName: "H", email: "l@x.org", studyInstitution: "U" },
      consents: new Set(["privacy"]),
      captchaToken: "t",
    };
    expect(validate(student, consents)).toEqual({
      studyProgramme: MESSAGES.required,
      studentId: MESSAGES.required,
    });
  });

  it("limits text to 200 and email to 254 characters", () => {
    const domain = "@" + "d".repeat(60) + ".org";
    const email254 = "a".repeat(254 - domain.length) + domain;
    expect(validate(external({ lastName: "x".repeat(200), email: email254 }), consents)).toEqual(
      {},
    );
    expect(
      validate(external({ lastName: "x".repeat(201), email: "b" + email254 }), consents),
    ).toEqual({
      lastName: MESSAGES.tooLong(200),
      email: MESSAGES.tooLong(254),
    });
    expect(MESSAGES.tooLong(7)).toBe("This field must be at most 7 characters long.");
  });

  it.each([
    "not-an-email",
    "ana@",
    "@example.org",
    "ana@example",
    "ana@@example.org",
    "ana novak@example.org",
    ".ana@example.org",
    "ana@-example.org",
    "ana@example-.org",
  ])("rejects the email %s", (email) => {
    expect(validate(external({ email }), consents)).toEqual({ email: MESSAGES.email });
  });

  it.each(["a@b.co", "first.last+tag@sub.example.org", " x_y@a-b.si "])(
    "accepts the email %s",
    (email) => {
      expect(validate(external({ email }), consents)).toEqual({});
    },
  );

  it("requires the mandatory consents only", () => {
    expect(validate(external({}, { consents: new Set(["photos"]) }), consents)).toEqual({
      consents: MESSAGES.consent,
    });
    expect(validate(external({}, { consents: new Set(["privacy"]) }), [consents[1]])).toEqual({});
  });

  it("requires a captcha token", () => {
    expect(validate(external({}, { captchaToken: null }), consents)).toEqual({
      captchaToken: MESSAGES.captcha,
    });
    expect(validate(external({}, { captchaToken: "" }), consents)).toEqual({
      captchaToken: MESSAGES.captcha,
    });
  });
});
