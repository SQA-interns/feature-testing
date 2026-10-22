import { describe, expect, it } from "vitest";
import { FIELDS, MESSAGES, validate } from "./fields";

const consents = [
  { id: "privacy", text: "I agree.", mandatory: true },
  { id: "photos", text: "Photos.", mandatory: false },
];

describe("form field rules", () => {
  it("lists the fixed fields of each type", () => {
    expect(FIELDS.EXTERNAL.map((f) => f.name)).toEqual([
      "firstName",
      "lastName",
      "email",
      "organization",
    ]);
    expect(FIELDS.STUDENT.map((f) => f.name)).toEqual([
      "firstName",
      "lastName",
      "email",
      "studyInstitution",
      "studyProgramme",
      "studentId",
    ]);
    expect(FIELDS.STUDENT.map((f) => f.label)).toEqual([
      "First name",
      "Last name",
      "Email",
      "Study institution",
      "Study programme",
      "Student ID",
    ]);
    expect(FIELDS.EXTERNAL.find((f) => f.name === "email")?.inputType).toBe("email");
    expect(FIELDS.STUDENT.find((f) => f.name === "studentId")?.inputType).toBe("text");
  });

  it("accepts a complete form", () => {
    const values = {
      firstName: "Luka",
      lastName: "Horvat",
      email: " luka@uni.si ",
      studyInstitution: "Uni",
      studyProgramme: "CS",
      studentId: "63",
    };

    expect(validate("STUDENT", values, ["privacy"], consents)).toEqual({});
  });

  it("requires every field of the type, blank counts as empty", () => {
    expect(validate("STUDENT", { firstName: "  " }, ["privacy"], consents)).toEqual({
      firstName: MESSAGES.required,
      lastName: MESSAGES.required,
      email: MESSAGES.required,
      studyInstitution: MESSAGES.required,
      studyProgramme: MESSAGES.required,
      studentId: MESSAGES.required,
    });
  });

  it("checks the email format", () => {
    const base = { firstName: "A", lastName: "B", organization: "O" };
    for (const email of ["a@b", "a b@c.si", "a@b@c.si", "@b.si", "a@b.", "a@b.si x", "x a@b.si"]) {
      expect(validate("EXTERNAL", { ...base, email }, ["privacy"], consents)).toEqual({
        email: MESSAGES.email,
      });
    }
    expect(validate("EXTERNAL", { ...base, email: "a.b@c.si" }, ["privacy"], consents)).toEqual({});
  });

  it("requires only the mandatory consents", () => {
    const values = { firstName: "A", lastName: "B", email: "a@b.si", organization: "O" };

    expect(validate("EXTERNAL", values, ["photos"], consents)).toEqual({
      consents: MESSAGES.consent,
    });
    expect(validate("EXTERNAL", values, ["privacy"], consents)).toEqual({});
    expect(validate("EXTERNAL", values, [], [consents[1]])).toEqual({});
  });
});
