import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  PRIVACY_CONSENT,
  check,
  chooseType,
  fakeBackend,
  fillExternal,
  json,
  posts,
  renderApp,
  submit,
  type,
} from "./support";

afterEach(() => {
  vi.unstubAllGlobals();
});

const GENERAL_ERROR = "Your registration could not be sent. Please try again later.";

async function fillValidExternalForm() {
  renderApp("/");
  await chooseType("External participant");
  fillExternal();
  check("AI workshop");
  check("Lunch");
  check(PRIVACY_CONSENT);
}

describe("US-004 registration confirmation", () => {
  it("AC-004-01 shows the confirmation with the name and the selected options once the backend accepts", async () => {
    const calls = fakeBackend({
      "POST /api/registrations": () => json(201, { id: "4b0f8a4e-7d3c-4d2b-9a55-0c2f6f1c9e11" }),
    });
    await fillValidExternalForm();

    submit();

    expect(await screen.findByRole("heading", { name: "Registration received" })).toBeVisible();
    expect(screen.getByText(/Ana Novak/)).toBeVisible();
    expect(screen.getByText("AI workshop")).toBeVisible();
    expect(screen.getByText("Lunch")).toBeVisible();
    expect(screen.queryByText("Conference dinner")).toBeNull();
    expect(screen.queryByRole("button", { name: "Register" })).toBeNull();
    expect(screen.queryByLabelText("First name")).toBeNull();

    const [post] = posts(calls);
    expect(post.path).toBe("/api/registrations");
    expect(post.body).toMatchObject({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana.novak@example.org",
      organization: "Example d.o.o.",
      options: ["ws-ai", "meal-lunch"],
      consents: ["privacy"],
      captchaToken: "test-pass",
    });
  });

  it("AC-004-02 shows the backend's field errors next to the fields and keeps the entered values", async () => {
    fakeBackend({
      "POST /api/registrations": () =>
        json(400, {
          errors: [
            { field: "email", message: "This email address is not accepted." },
            { field: "options", message: "An option is not available." },
          ],
        }),
    });
    await fillValidExternalForm();

    submit();

    expect(await screen.findByText("An option is not available.")).toBeVisible();
    expect(screen.getByLabelText("Email")).toHaveAccessibleDescription(
      "This email address is not accepted.",
    );
    expect(screen.queryByRole("heading", { name: "Registration received" })).toBeNull();
    expect(screen.getByLabelText("First name")).toHaveValue("Ana");
    expect(screen.getByLabelText("Email")).toHaveValue("ana.novak@example.org");
    expect(screen.getByLabelText("Organization / institution")).toHaveValue("Example d.o.o.");
    expect(screen.getByRole("checkbox", { name: "AI workshop" })).toBeChecked();
  });

  it("AC-004-03 shows field errors for an empty required field and an invalid email and sends nothing", async () => {
    const calls = fakeBackend();
    await fillValidExternalForm();
    type("First name", "   ");
    type("Email", "ana.novak-at-example.org");

    submit();

    expect(await screen.findByText("This field is required.")).toBeVisible();
    expect(screen.getByLabelText("First name")).toHaveAccessibleDescription(
      "This field is required.",
    );
    expect(screen.getByLabelText("Email")).toHaveAccessibleDescription(
      "Enter a valid email address.",
    );
    expect(posts(calls)).toEqual([]);
    expect(screen.queryByRole("heading", { name: "Registration received" })).toBeNull();
  });

  it("AC-004-04 shows a general error without technical details when the backend fails", async () => {
    fakeBackend({
      "POST /api/registrations": () =>
        json(500, { errors: [{ field: "server", message: "Internal error" }] }),
    });
    await fillValidExternalForm();

    submit();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(GENERAL_ERROR);
    expect(alert.textContent).not.toMatch(/500|Internal|server/i);
    expect(screen.queryByRole("heading", { name: "Registration received" })).toBeNull();
    expect(screen.getByLabelText("First name")).toHaveValue("Ana");
  });

  it("AC-004-04 shows a general error without technical details when the backend cannot be reached", async () => {
    fakeBackend({
      "POST /api/registrations": () => {
        throw new TypeError("Failed to fetch");
      },
    });
    await fillValidExternalForm();

    submit();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(GENERAL_ERROR);
    expect(alert.textContent).not.toMatch(/fetch|TypeError/i);
    expect(screen.queryByRole("heading", { name: "Registration received" })).toBeNull();
  });
});
