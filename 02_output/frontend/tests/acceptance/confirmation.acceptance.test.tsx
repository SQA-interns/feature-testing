import "@testing-library/jest-dom/vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "../../src/App";
import {
  CAPTCHA,
  GENERAL_ERROR,
  PRIVACY,
  check,
  chooseType,
  fieldError,
  fillValidExternal,
  stubFetch,
  submit,
  textbox,
  type,
} from "./support";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("US-004 registration confirmation", () => {
  it("AC-004-01 shows the confirmation with name and selected options and removes the form", async () => {
    const stub = stubFetch({ status: 201, body: { id: "0b6f4a8e-1c2d-4e5f-8a9b-0c1d2e3f4a5b" } });
    render(<App />);
    await fillValidExternal();
    type("First name", "  Ana  ");

    submit();

    const status = await screen.findByRole("status");
    expect(
      within(status).getByRole("heading", { name: "Registration received" }),
    ).toBeInTheDocument();
    expect(status).toHaveTextContent("Ana Novak");
    const list = within(status).getByRole("list", { name: "Selected options" });
    const items = within(list)
      .getAllByRole("listitem")
      .map((li) => li.textContent?.trim());
    expect(items.sort()).toEqual(["AI workshop", "Gala evening"]);
    expect(screen.queryByRole("button", { name: "Register" })).not.toBeInTheDocument();
    expect(screen.queryAllByRole("textbox")).toHaveLength(0);

    expect(stub.posts).toHaveLength(1);
    const sent = stub.posts[0];
    expect(sent).toMatchObject({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana@example.org",
      organization: "Example d.o.o.",
      consents: ["privacy"],
      captchaToken: "test-pass",
    });
    expect([...(sent.options as string[])].sort()).toEqual(["ev-gala", "ws-ai"]);
  });

  it("AC-004-01 confirms a student registration without options", async () => {
    stubFetch({ status: 201, body: { id: "1b6f4a8e-1c2d-4e5f-8a9b-0c1d2e3f4a5b" } });
    render(<App />);
    await chooseType("Student");
    type("First name", "Luka");
    type("Last name", "Kranjc");
    type("Email", "luka@student.example.org");
    type("Study institution", "Univerza v Ljubljani");
    type("Study programme", "Računalništvo");
    type("Student ID", "63210001");
    check(PRIVACY);
    check(CAPTCHA);

    submit();

    const status = await screen.findByRole("status");
    expect(status).toHaveTextContent("Luka Kranjc");
    expect(status).toHaveTextContent("No options selected.");
    expect(screen.queryByRole("button", { name: "Register" })).not.toBeInTheDocument();
  });

  it("AC-004-02 shows backend field errors next to their fields and keeps the values", async () => {
    stubFetch({
      status: 400,
      body: {
        errors: [
          { field: "email", message: "Enter a valid email address." },
          { field: "options", message: "Option 'ev-gala' is not available." },
          { field: "organization", message: "This value is too long." },
        ],
      },
    });
    render(<App />);
    await fillValidExternal();

    submit();

    await waitFor(() => expect(fieldError("email")).toBe("Enter a valid email address."));
    expect(fieldError("options")).toBe("Option 'ev-gala' is not available.");
    expect(fieldError("organization")).toBe("This value is too long.");
    expect(textbox("Email")).toHaveAttribute("aria-invalid", "true");
    expect(textbox("Email").getAttribute("aria-describedby")).toContain("email-error");
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.queryByText("Registration received")).not.toBeInTheDocument();
    expect(textbox("First name").value).toBe("Ana");
    expect(textbox("Email").value).toBe("ana@example.org");
    expect(textbox("Organization / institution").value).toBe("Example d.o.o.");
    expect(screen.getByRole("checkbox", { name: "AI workshop" })).toBeChecked();
    expect(screen.getByRole("button", { name: "Register" })).toBeInTheDocument();
  });

  it("AC-004-03 shows field errors for an empty required field and an invalid email without a request", async () => {
    const stub = stubFetch({ status: 201, body: { id: "x" } });
    render(<App />);
    await fillValidExternal();
    type("First name", "");
    type("Last name", "   ");
    type("Email", "ana@");

    submit();

    await waitFor(() => expect(fieldError("firstName")).toBe("This field is required."));
    expect(fieldError("lastName")).toBe("This field is required.");
    expect(fieldError("email")).toBe("Enter a valid email address.");
    expect(textbox("First name")).toHaveAttribute("aria-invalid", "true");
    expect(stub.posts).toHaveLength(0);
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("AC-004-04 shows a general error without technical details when the backend fails", async () => {
    stubFetch({
      status: 500,
      body: { message: "java.lang.IllegalStateException at si.konferenca.Storage" },
    });
    render(<App />);
    await fillValidExternal();

    submit();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(GENERAL_ERROR);
    expect(document.body).not.toHaveTextContent("IllegalStateException");
    expect(document.body).not.toHaveTextContent("500");
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Register" })).toBeInTheDocument();
  });

  it("AC-004-04 shows a general error when the backend cannot be reached", async () => {
    stubFetch("network-error");
    render(<App />);
    await fillValidExternal();

    submit();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(GENERAL_ERROR);
    expect(document.body).not.toHaveTextContent("Failed to fetch");
    expect(document.body).not.toHaveTextContent("TypeError");
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });
});
