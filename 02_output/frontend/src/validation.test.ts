import { describe, expect, it } from "vitest";
import type { Consent } from "./types";
import { isValidEmail, MESSAGES, validate, type FormValues } from "./validation";

const consents: Consent[] = [
  { id: "privacy", text: "P", mandatory: true },
  { id: "photo", text: "F", mandatory: false },
];

const values: FormValues = {
  firstName: "Ana",
  lastName: "Novak",
  email: "ana@example.org",
  organization: "Org",
  studyInstitution: "",
  studyProgramme: "",
  studentId: "",
};

describe("isValidEmail", () => {
  it.each(["ana@example.org", "a@b.co", "č@primer.si", "a.b+c@sub.example.org"])(
    "accepts %s",
    (email) => expect(isValidEmail(email)).toBe(true),
  );

  it.each([
    "",
    "ana",
    "@example.org",
    "ana@",
    "ana@@example.org",
    "ana@ex@ample.org",
    "ana@example",
    "ana@.example.org",
    "ana@example.org.",
    "ana@example..org",
    "ana novak@example.org",
    "a,b@example.org",
    "a;b@example.org",
    "<a@example.org>",
    "a(b)@example.org",
    "a[b]@example.org",
    'a"b@example.org',
    "a\\b@example.org",
    "a:b@example.org",
  ])("rejects %s", (email) => expect(isValidEmail(email)).toBe(false));
});

describe("validate", () => {
  it("accepts a complete external form", () => {
    expect(validate("EXTERNAL", values, consents, new Set(["privacy"]))).toEqual({});
  });

  it("requires the fields of the chosen type only", () => {
    expect(
      validate("EXTERNAL", { ...values, organization: "  " }, consents, new Set(["privacy"])),
    ).toEqual({
      organization: MESSAGES.required,
    });
    expect(validate("STUDENT", values, consents, new Set(["privacy"]))).toEqual({
      studyInstitution: MESSAGES.required,
      studyProgramme: MESSAGES.required,
      studentId: MESSAGES.required,
    });
  });

  it("reports an empty email as required, not as invalid", () => {
    expect(validate("EXTERNAL", { ...values, email: " " }, consents, new Set(["privacy"]))).toEqual(
      {
        email: MESSAGES.required,
      },
    );
  });

  it("checks the trimmed email format", () => {
    expect(
      validate(
        "EXTERNAL",
        { ...values, email: " ana@example.org " },
        consents,
        new Set(["privacy"]),
      ),
    ).toEqual({});
    expect(
      validate("EXTERNAL", { ...values, email: "ana@" }, consents, new Set(["privacy"])),
    ).toEqual({
      email: MESSAGES.email,
    });
  });

  it("requires only the mandatory consents", () => {
    expect(validate("EXTERNAL", values, consents, new Set(["photo"]))).toEqual({
      consents: MESSAGES.consent,
    });
    expect(validate("EXTERNAL", values, [consents[1]], new Set())).toEqual({});
  });

  it("uses the messages of the UI contract", () => {
    expect(MESSAGES).toEqual({
      required: "This field is required.",
      email: "Enter a valid email address.",
      consent: "This consent is required.",
    });
  });
});
