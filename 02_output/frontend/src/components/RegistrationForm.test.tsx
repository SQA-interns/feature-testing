import "@testing-library/jest-dom/vitest";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { FormConfig } from "../domain/types";
import { MESSAGES } from "../domain/validation";
import { RegistrationForm } from "./RegistrationForm";

const config: FormConfig = {
  options: [
    { id: "ws", name: "Workshop A", category: "WORKSHOP", availableTo: ["EXTERNAL", "STUDENT"] },
    { id: "ws2", name: "Workshop B", category: "WORKSHOP", availableTo: ["EXTERNAL", "STUDENT"] },
  ],
  consents: [{ id: "privacy", text: "Privacy", mandatory: true }],
  captchaSiteKey: "site",
  captchaTestMode: true,
};

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

function stubPost(answer: () => Response) {
  const posts: Record<string, unknown>[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (_url: string, init?: RequestInit) => {
      posts.push(JSON.parse(String(init?.body)) as Record<string, unknown>);
      return answer();
    }),
  );
  return posts;
}

function fill(labels: Record<string, string>) {
  for (const [label, value] of Object.entries(labels)) {
    fireEvent.change(screen.getByRole("textbox", { name: label }), { target: { value } });
  }
}

function fillExternal() {
  fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
  fill({
    "First name": "  Ana  ",
    "Last name": " Novak ",
    Email: "ana@example.org",
    "Organization / institution": "Org",
  });
  fireEvent.click(screen.getByRole("checkbox", { name: "Privacy" }));
  fireEvent.click(screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }));
}

afterEach(() => {
  vi.unstubAllGlobals();
  delete window.grecaptcha;
  delete window.onRecaptchaLoad;
  document.getElementById("recaptcha-script")?.remove();
});

describe("RegistrationForm", () => {
  it("starts with nothing chosen, no errors and nothing submitted", () => {
    const posts = stubPost(() => json(201, { id: "x" }));
    render(<RegistrationForm config={config} onRegistered={() => {}} />);
    const form = document.querySelector("form")!;

    expect(screen.getByRole("radio", { name: "External participant" })).not.toBeChecked();
    expect(fireEvent.submit(form)).toBe(false);

    expect(document.getElementById("type-error")).toBeNull();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(posts).toHaveLength(0);
  });

  it("marks the chosen type and starts with empty fields of the right input types", () => {
    stubPost(() => json(201, { id: "x" }));
    render(<RegistrationForm config={config} onRegistered={() => {}} />);

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));

    expect(screen.getByRole("radio", { name: "Student" })).toBeChecked();
    expect(screen.getByRole("radio", { name: "External participant" })).not.toBeChecked();
    for (const box of screen.getAllByRole("textbox")) expect(box).toHaveValue("");
    expect(screen.getByRole("textbox", { name: "Email" })).toHaveAttribute("type", "email");
    expect(
      screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }),
    ).not.toBeChecked();
  });

  it("reports untouched fields, the consent and the captcha with their messages and no alert", async () => {
    const posts = stubPost(() => json(201, { id: "x" }));
    render(<RegistrationForm config={config} onRegistered={() => {}} />);
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
    fill({ "Last name": "Ž".repeat(201) });

    expect(fireEvent.submit(document.querySelector("form")!)).toBe(false);

    await waitFor(() => expect(document.getElementById("firstName-error")).not.toBeNull());
    expect(document.getElementById("lastName-error")).toHaveTextContent(MESSAGES.tooLong);
    expect(document.getElementById("consents-error")).toHaveTextContent(MESSAGES.consent);
    expect(document.getElementById("captchaToken-error")).toHaveTextContent(MESSAGES.captcha);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(posts).toHaveLength(0);
    expect(MESSAGES).toEqual({
      required: "This field is required.",
      email: "Enter a valid email address.",
      tooLong: "This value is too long.",
      consent: "This consent is required.",
      captcha: "Please confirm that you are not a robot.",
    });
  });

  it("removes only the unchecked option and confirms with trimmed names", async () => {
    const posts = stubPost(() => json(201, { id: "x" }));
    const onRegistered = vi.fn();
    render(<RegistrationForm config={config} onRegistered={onRegistered} />);
    fillExternal();
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop A" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop B" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop A" }));

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() =>
      expect(onRegistered).toHaveBeenCalledWith({
        firstName: "Ana",
        lastName: "Novak",
        optionNames: ["Workshop B"],
      }),
    );
    expect(posts[0].options).toEqual(["ws2"]);
  });

  it.each([
    ["type", "type-error"],
    ["firstName", "firstName-error"],
    ["lastName", "lastName-error"],
    ["email", "email-error"],
    ["organization", "organization-error"],
    ["options", "options-error"],
    ["consents", "consents-error"],
    ["captchaToken", "captchaToken-error"],
  ])("shows a backend error for %s at its field without a general error", async (field, id) => {
    stubPost(() => json(400, { errors: [{ field, message: `bad ${field}` }] }));
    render(<RegistrationForm config={config} onRegistered={() => {}} />);
    fillExternal();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() => expect(document.getElementById(id)).toHaveTextContent(`bad ${field}`));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it.each([
    ["studyInstitution", "studyInstitution-error"],
    ["studyProgramme", "studyProgramme-error"],
    ["studentId", "studentId-error"],
  ])("shows a backend error for the student field %s", async (field, id) => {
    stubPost(() => json(400, { errors: [{ field, message: `bad ${field}` }] }));
    render(<RegistrationForm config={config} onRegistered={() => {}} />);
    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    fill({
      "First name": "Luka",
      "Last name": "Kranjc",
      Email: "luka@uni.si",
      "Study institution": "UL",
      "Study programme": "CS",
      "Student ID": "1",
    });
    fireEvent.click(screen.getByRole("checkbox", { name: "Privacy" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }));

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() => expect(document.getElementById(id)).toHaveTextContent(`bad ${field}`));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("resets the Google widget after every failed submission", async () => {
    const reset = vi.fn();
    const callbacks: ((token: string) => void)[] = [];
    window.grecaptcha = {
      render: vi.fn((_e: HTMLElement, o: { callback: (t: string) => void }) => {
        callbacks.push(o.callback);
        return 3;
      }),
      reset,
    };
    stubPost(() => json(500, { message: "x" }));
    render(
      <RegistrationForm config={{ ...config, captchaTestMode: false }} onRegistered={() => {}} />,
    );
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
    fill({
      "First name": "Ana",
      "Last name": "Novak",
      Email: "ana@example.org",
      "Organization / institution": "Org",
    });
    fireEvent.click(screen.getByRole("checkbox", { name: "Privacy" }));
    act(() => callbacks[0]("google-token"));

    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    await screen.findByRole("alert");
    expect(reset).toHaveBeenCalledTimes(1);
    expect(reset).toHaveBeenCalledWith(3);

    act(() => callbacks[0]("second-token"));
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    await waitFor(() => expect(reset).toHaveBeenCalledTimes(2));
  });
});
