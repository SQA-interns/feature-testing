import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../../src/App";
import { mockApi, PRIVACY_TEXT, registrationPosts, type ApiHandler } from "./support/mockApi";
import { check, chooseType, fill, fillValidExternal, submit } from "./support/page";

const GENERAL_FAILURE = "Your registration could not be completed. Please try again later.";

function postAnswer(answer: ReturnType<ApiHandler>): ApiHandler {
  return (call) =>
    call.method === "POST" && call.url.pathname === "/api/registrations" ? answer : undefined;
}

describe("US-004 registration confirmation", () => {
  beforeEach(() => {
    window.history.pushState({}, "", "/");
  });
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("AC-004-01: accepted registration shows a confirmation with name and options instead of the form", async () => {
    const calls = mockApi(
      postAnswer({ status: 201, body: { id: "0b6f8e1c-1111-4c2a-9d7e-2f1a3b4c5d6e" } }),
    );
    render(<App />);
    await chooseType("External participant");
    fillValidExternal(PRIVACY_TEXT);
    check("AI workshop");
    check("City tour");

    submit();

    expect(
      await screen.findByRole("heading", { name: "Registration received" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Thank you, Ana Novak.")).toBeInTheDocument();
    expect(screen.getByText("AI workshop")).toBeInTheDocument();
    expect(screen.getByText("City tour")).toBeInTheDocument();
    expect(screen.queryByText("Lunch")).toBeNull();
    expect(screen.queryByRole("button", { name: "Register" })).toBeNull();
    expect(screen.queryByRole("textbox", { name: "First name" })).toBeNull();
    const posts = registrationPosts(calls);
    expect(posts).toHaveLength(1);
    expect(posts[0].body).toMatchObject({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana@example.org",
      organization: "Example d.o.o.",
      consents: ["privacy"],
    });
    expect((posts[0].body as { options: string[] }).options.sort()).toEqual([
      "other-tour",
      "ws-ai",
    ]);
  });

  it("AC-004-01: no confirmation is shown before the backend accepted", async () => {
    let release: (() => void) | undefined;
    const gate = new Promise<void>((resolve) => (release = resolve));
    mockApi(postAnswer({ status: 201, body: { id: "x" } }));
    const fetchMock = globalThis.fetch;
    vi.stubGlobal("fetch", async (input: RequestInfo | URL, init?: RequestInit) => {
      const method = init?.method ?? (input instanceof Request ? input.method : "GET");
      if (method.toUpperCase() === "POST") await gate;
      return fetchMock(input, init);
    });
    render(<App />);
    await chooseType("External participant");
    fillValidExternal(PRIVACY_TEXT);

    submit();
    await new Promise((r) => setTimeout(r, 50));

    expect(screen.queryByRole("heading", { name: "Registration received" })).toBeNull();
    release?.();
    expect(
      await screen.findByRole("heading", { name: "Registration received" }),
    ).toBeInTheDocument();
  });

  it("AC-004-02: backend field errors are shown next to their fields and the values remain", async () => {
    mockApi(
      postAnswer({
        status: 400,
        body: {
          errors: [
            { field: "email", message: "Email address is not accepted." },
            { field: "options", message: "Option is not available." },
            { field: "organization", message: "Organization is required." },
          ],
        },
      }),
    );
    render(<App />);
    await chooseType("External participant");
    fillValidExternal(PRIVACY_TEXT);
    check("AI workshop");

    submit();

    const email = screen.getByRole("textbox", { name: "Email" });
    await waitFor(() =>
      expect(email).toHaveAccessibleDescription("Email address is not accepted."),
    );
    expect(email).toHaveAttribute("aria-invalid", "true");
    expect(
      screen.getByRole("textbox", { name: "Organization / institution" }),
    ).toHaveAccessibleDescription("Organization is required.");
    expect(document.getElementById("options-error")).toHaveTextContent("Option is not available.");
    expect(screen.queryByRole("heading", { name: "Registration received" })).toBeNull();
    expect(screen.getByRole("textbox", { name: "First name" })).toHaveValue("Ana");
    expect(screen.getByRole("textbox", { name: "Last name" })).toHaveValue("Novak");
    expect(email).toHaveValue("ana@example.org");
    expect(screen.getByRole("checkbox", { name: "AI workshop" })).toBeChecked();
    expect(screen.getByRole("checkbox", { name: PRIVACY_TEXT })).toBeChecked();
  });

  it("AC-004-03: an empty required field is reported and no request is sent", async () => {
    const calls = mockApi(postAnswer({ status: 201, body: { id: "x" } }));
    render(<App />);
    await chooseType("External participant");
    fillValidExternal(PRIVACY_TEXT);
    fill("First name", "   ");

    submit();

    expect(await screen.findByText("This field is required.")).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "First name" })).toHaveAccessibleDescription(
      "This field is required.",
    );
    await new Promise((r) => setTimeout(r, 50));
    expect(registrationPosts(calls)).toHaveLength(0);
  });

  it("AC-004-03: an invalid email is reported and no request is sent", async () => {
    const calls = mockApi(postAnswer({ status: 201, body: { id: "x" } }));
    render(<App />);
    await chooseType("Student");
    fill("First name", "Luka");
    fill("Last name", "Kranjc");
    fill("Email", "luka@");
    fill("Study institution", "University of Ljubljana");
    fill("Study programme", "Computer Science");
    fill("Student ID", "63200001");
    check(PRIVACY_TEXT);

    submit();

    expect(await screen.findByText("Enter a valid email address.")).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Email" })).toHaveAccessibleDescription(
      "Enter a valid email address.",
    );
    await new Promise((r) => setTimeout(r, 50));
    expect(registrationPosts(calls)).toHaveLength(0);
  });

  it("AC-004-04: a backend failure shows a general message without technical details", async () => {
    mockApi(
      postAnswer({
        status: 500,
        body: { message: "java.lang.IllegalStateException: SQLState 23505 at RegistrationService" },
      }),
    );
    render(<App />);
    await chooseType("External participant");
    fillValidExternal(PRIVACY_TEXT);

    submit();

    expect(await screen.findByText(GENERAL_FAILURE)).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Registration received" })).toBeNull();
    expect(document.body.textContent).not.toMatch(/IllegalStateException|SQLState|500/);
  });

  it("AC-004-04: an unreachable backend shows a general message and no confirmation", async () => {
    mockApi(postAnswer("network-error"));
    render(<App />);
    await chooseType("External participant");
    fillValidExternal(PRIVACY_TEXT);

    submit();

    expect(await screen.findByText(GENERAL_FAILURE)).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Registration received" })).toBeNull();
    expect(document.body.textContent).not.toMatch(/Failed to fetch|TypeError/);
  });
});
