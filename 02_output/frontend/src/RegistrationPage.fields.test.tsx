import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  formConfig,
  mockApi,
  PRIVACY_TEXT,
  registrationPosts,
  type ApiAnswer,
} from "../tests/acceptance/support/mockApi";
import { GENERAL_FAILURE, RegistrationPage } from "./RegistrationPage";

const FIELDS = ["First name", "Last name", "Email", "Organization / institution"];

function answerPost(answer: ApiAnswer) {
  return mockApi((call) => (call.method === "POST" ? answer : undefined));
}

function textbox(name: string) {
  return screen.getByRole("textbox", { name });
}

function fill(name: string, value: string) {
  fireEvent.change(textbox(name), { target: { value } });
}

async function chooseExternal() {
  fireEvent.click(await screen.findByRole("radio", { name: "External participant" }));
}

function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

describe("RegistrationPage fields", () => {
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("starts with empty fields, no errors and the chosen radio checked", async () => {
    mockApi();
    render(<RegistrationPage />);
    const external = await screen.findByRole("radio", { name: "External participant" });
    const student = screen.getByRole("radio", { name: "Student" });
    expect(external).not.toBeChecked();
    expect(student).not.toBeChecked();

    fireEvent.click(external);

    expect(external).toBeChecked();
    expect(student).not.toBeChecked();
    for (const name of FIELDS) {
      expect(textbox(name)).toHaveValue("");
      expect(textbox(name)).not.toHaveAttribute("aria-invalid");
      expect(textbox(name)).not.toHaveAttribute("aria-describedby");
    }
    expect(document.querySelector(".field-error")).toBeNull();
    expect(screen.queryByRole("alert")).toBeNull();
    fireEvent.click(student);
    expect(student).toBeChecked();
    expect(external).not.toBeChecked();
    for (const name of ["Study institution", "Study programme", "Student ID"]) {
      expect(textbox(name)).toHaveValue("");
    }
  });

  it("uses an email input for the email and text inputs otherwise", async () => {
    mockApi();
    render(<RegistrationPage />);
    await chooseExternal();

    expect(textbox("Email")).toHaveAttribute("type", "email");
    for (const name of ["First name", "Last name", "Organization / institution"]) {
      expect(textbox(name)).toHaveAttribute("type", "text");
    }
  });

  it("links an error to its field and removes the link once fixed", async () => {
    answerPost({ status: 201, body: { id: "x" } });
    render(<RegistrationPage />);
    await chooseExternal();

    submit();

    expect(textbox("First name")).toHaveAttribute("aria-describedby", "firstName-error");
    expect(textbox("First name")).toHaveAttribute("aria-invalid", "true");
    expect(document.getElementById("firstName-error")).toHaveClass("field-error");
    fill("First name", "Ana");
    fill("Last name", "Novak");
    fill("Email", "ana@example.org");
    fill("Organization / institution", "Org");
    fireEvent.click(screen.getByRole("checkbox", { name: PRIVACY_TEXT }));
    submit();
    expect(
      await screen.findByRole("heading", { name: "Registration received" }),
    ).toBeInTheDocument();
  });

  it("sends and confirms trimmed values", async () => {
    const calls = answerPost({ status: 201, body: { id: "x" } });
    render(<RegistrationPage />);
    await chooseExternal();
    fill("First name", "  Ana ");
    fill("Last name", " Novak  ");
    fill("Email", " ana@example.org ");
    fill("Organization / institution", "  Org  ");
    fireEvent.click(screen.getByRole("checkbox", { name: PRIVACY_TEXT }));

    submit();

    expect(await screen.findByText("Thank you, Ana Novak.")).toBeInTheDocument();
    expect(registrationPosts(calls)[0].body).toMatchObject({
      firstName: "Ana",
      lastName: "Novak",
      email: "ana@example.org",
      organization: "Org",
      studyInstitution: null,
    });
  });

  it("clears client errors when the type changes", async () => {
    mockApi();
    render(<RegistrationPage />);
    await chooseExternal();
    submit();
    expect(screen.getAllByText("This field is required.").length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));

    expect(screen.queryByText("This field is required.")).toBeNull();
  });

  it("enables the button again after a failure and clears the failure on the next submit", async () => {
    answerPost({ status: 500 });
    render(<RegistrationPage />);
    await chooseExternal();
    fill("First name", "Ana");
    fill("Last name", "Novak");
    fill("Email", "ana@example.org");
    fill("Organization / institution", "Org");
    fireEvent.click(screen.getByRole("checkbox", { name: PRIVACY_TEXT }));
    submit();
    expect(await screen.findByText(GENERAL_FAILURE)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Register" })).toBeEnabled();

    fill("Email", "bad");
    submit();

    expect(screen.queryByText(GENERAL_FAILURE)).toBeNull();
    expect(screen.getByText("Enter a valid email address.")).toBeInTheDocument();
  });

  it("shows no form error when the backend reports field errors only", async () => {
    answerPost({ status: 400, body: { errors: [{ field: "lastName", message: "Too long." }] } });
    render(<RegistrationPage />);
    await chooseExternal();
    fill("First name", "Ana");
    fill("Last name", "Novak");
    fill("Email", "ana@example.org");
    fill("Organization / institution", "Org");
    fireEvent.click(screen.getByRole("checkbox", { name: PRIVACY_TEXT }));

    submit();

    await waitFor(() => expect(textbox("Last name")).toHaveAccessibleDescription("Too long."));
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("hides a category without options for the type", async () => {
    mockApi((call) =>
      call.url.pathname === "/api/form-config"
        ? {
            status: 200,
            body: {
              ...formConfig,
              options: formConfig.options.filter((o) => o.category !== "MEAL"),
            },
          }
        : undefined,
    );
    render(<RegistrationPage />);
    await chooseExternal();

    expect(screen.queryByRole("group", { name: "Meals" })).toBeNull();
    expect(screen.getByRole("group", { name: "Events" })).toBeInTheDocument();
  });

  it("sends an empty token while the widget has given none", async () => {
    window.grecaptcha = { render: () => 1, reset: vi.fn() };
    const calls = mockApi((call) => {
      if (call.url.pathname === "/api/form-config")
        return {
          status: 200,
          body: { ...formConfig, captchaTestMode: false, captchaSiteKey: "k" },
        };
      return call.method === "POST" ? { status: 400, body: { errors: [] } } : undefined;
    });
    render(<RegistrationPage />);
    await chooseExternal();
    fill("First name", "Ana");
    fill("Last name", "Novak");
    fill("Email", "ana@example.org");
    fill("Organization / institution", "Org");
    fireEvent.click(screen.getByRole("checkbox", { name: PRIVACY_TEXT }));

    submit();

    await waitFor(() => expect(registrationPosts(calls)).toHaveLength(1));
    expect((registrationPosts(calls)[0].body as { captchaToken: string }).captchaToken).toBe("");
    delete window.grecaptcha;
  });
});
