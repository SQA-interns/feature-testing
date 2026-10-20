// US-004 registration confirmation: AC-004-01 … AC-004-04 (NFR-03).
import "@testing-library/jest-dom/vitest";
import { screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  LABELS,
  expectFormStillShown,
  field,
  fillValidExternal,
  mockBackend,
  postCalls,
  queryField,
  submit,
  type,
} from "./harness";

describe("US-004 registration confirmation", () => {
  it("AC-004-01 an accepted registration shows the name and options and hides the form", async () => {
    const calls = mockBackend({
      status: 201,
      body: { id: "3f1c9a52-0000-4000-8000-000000000001" },
    });
    await fillValidExternal();

    submit();

    await waitFor(() => expect(queryField(LABELS.firstName)).toBeNull());
    const text = document.body.textContent ?? "";
    expect(text).toContain("Ana");
    expect(text).toContain("Novak");
    expect(text).toContain("AI workshop");
    expect(text).toContain("Lunch");
    expect(text).not.toContain("Gala dinner");
    expect(screen.queryByRole("button", { name: "Register" })).toBeNull();

    const posts = postCalls(calls);
    expect(posts).toHaveLength(1);
    expect(posts[0].body).toMatchObject({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana.novak@example.org",
      organization: "Example d.o.o.",
      consents: ["privacy"],
      captchaToken: "test-pass",
    });
    expect([...(posts[0].body as { options: string[] }).options].sort()).toEqual([
      "ml-lunch",
      "ws-ai",
    ]);
  });

  it("AC-004-02 backend field errors appear next to their fields and the values remain", async () => {
    mockBackend({
      status: 400,
      body: {
        errors: [
          { field: "email", message: "This email address is not accepted." },
          { field: "organization", message: "This organization is not accepted." },
        ],
      },
    });
    await fillValidExternal();

    submit();

    await waitFor(() =>
      expect(field(LABELS.email)).toHaveAccessibleDescription(
        /This email address is not accepted\./,
      ),
    );
    expect(field(LABELS.organization)).toHaveAccessibleDescription(
      /This organization is not accepted\./,
    );
    expect(field(LABELS.firstName)).toHaveValue("Ana");
    expect(field(LABELS.lastName)).toHaveValue("Novak");
    expect(field(LABELS.email)).toHaveValue("ana.novak@example.org");
    expect(field(LABELS.organization)).toHaveValue("Example d.o.o.");
    expect(screen.getByRole("checkbox", { name: /^AI workshop/ })).toBeChecked();
    expectFormStillShown();
  });

  it("AC-004-02 a backend options error is shown and no confirmation appears", async () => {
    mockBackend({
      status: 400,
      body: { errors: [{ field: "options", message: "An option is no longer available." }] },
    });
    await fillValidExternal();

    submit();

    expect(await screen.findByText(/An option is no longer available\./)).toBeVisible();
    expect(field(LABELS.firstName)).toHaveValue("Ana");
    expectFormStillShown();
  });

  it("AC-004-03 an empty required field and an invalid email are shown and nothing is sent", async () => {
    const calls = mockBackend({ status: 201, body: { id: "unused" } });
    await fillValidExternal();
    type(LABELS.firstName, "   ");
    type(LABELS.email, "not-an-email");

    submit();

    await waitFor(() => expect(field(LABELS.firstName)).toHaveAccessibleDescription(/\S/));
    expect(field(LABELS.email)).toHaveAccessibleDescription(/\S/);
    expect(field(LABELS.lastName)).not.toHaveAccessibleDescription(/\S/);
    expect(screen.getAllByRole("alert").length).toBeGreaterThanOrEqual(2);
    expect(postCalls(calls)).toHaveLength(0);
    expectFormStillShown();
  });

  it("AC-004-04 a backend failure shows a general error without technical details", async () => {
    mockBackend({
      status: 500,
      body: { message: "java.lang.NullPointerException at si.konferenca.Internal" },
    });
    await fillValidExternal();

    submit();

    expect(await screen.findByText(/could not be completed/i)).toBeVisible();
    const text = document.body.textContent ?? "";
    expect(text).not.toMatch(/NullPointerException|si\.konferenca|500|stack/i);
    expect(field(LABELS.firstName)).toHaveValue("Ana");
    expectFormStillShown();
  });

  it("AC-004-04 an unreachable backend shows a general error without technical details", async () => {
    mockBackend({ networkError: true });
    await fillValidExternal();

    submit();

    expect(await screen.findByText(/could not be completed/i)).toBeVisible();
    const text = document.body.textContent ?? "";
    expect(text).not.toMatch(/TypeError|Failed to fetch/i);
    expect(field(LABELS.email)).toHaveValue("ana.novak@example.org");
    expectFormStillShown();
  });
});
