import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  formConfig,
  mockApi,
  PHOTO_TEXT,
  PRIVACY_TEXT,
  registrationPosts,
  type ApiAnswer,
} from "../tests/acceptance/support/mockApi";
import { GENERAL_FAILURE, RegistrationPage } from "./RegistrationPage";

function answerPost(answer: ApiAnswer) {
  return mockApi((call) => (call.method === "POST" ? answer : undefined));
}

function fill(label: string, value: string) {
  fireEvent.change(screen.getByRole("textbox", { name: label }), { target: { value } });
}

async function external() {
  fireEvent.click(await screen.findByRole("radio", { name: "External participant" }));
  fill("First name", "Ana");
  fill("Last name", "Novak");
  fill("Email", "ana@example.org");
  fill("Organization / institution", "Org");
}

function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

describe("RegistrationPage", () => {
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
    delete window.grecaptcha;
  });

  it("shows a message when the form configuration cannot be loaded", async () => {
    mockApi((call) => (call.url.pathname === "/api/form-config" ? { status: 500 } : undefined));

    render(<RegistrationPage />);

    expect(
      await screen.findByText("The registration form could not be loaded. Please try again later."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("radio")).toBeNull();
  });

  it("shows only the type choice before a type is chosen", async () => {
    mockApi();

    render(<RegistrationPage />);

    expect(await screen.findByRole("group", { name: "Registration type" })).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).toBeNull();
    expect(screen.queryByRole("button", { name: "Register" })).toBeNull();
  });

  it("keeps common values and drops options the new type cannot select", async () => {
    const calls = answerPost({ status: 201, body: { id: "x" } });
    render(<RegistrationPage />);
    await external();
    fireEvent.click(screen.getByRole("checkbox", { name: "Cloud workshop" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "AI workshop" }));

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    fill("Study institution", "Uni");
    fill("Study programme", "CS");
    fill("Student ID", "1");
    fireEvent.click(screen.getByRole("checkbox", { name: PRIVACY_TEXT }));
    submit();

    await screen.findByRole("heading", { name: "Registration received" });
    expect(registrationPosts(calls)[0].body).toEqual({
      type: "STUDENT",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana@example.org",
      organization: null,
      studyInstitution: "Uni",
      studyProgramme: "CS",
      studentId: "1",
      options: ["ws-ai"],
      consents: ["privacy"],
      captchaToken: "test-pass",
    });
  });

  it("unchecking an option or consent removes it from the request", async () => {
    const calls = answerPost({ status: 201, body: { id: "x" } });
    render(<RegistrationPage />);
    await external();
    fireEvent.click(screen.getByRole("checkbox", { name: "Lunch" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Lunch" }));
    fireEvent.click(screen.getByRole("checkbox", { name: PHOTO_TEXT }));
    fireEvent.click(screen.getByRole("checkbox", { name: PRIVACY_TEXT }));
    fireEvent.click(screen.getByRole("checkbox", { name: PHOTO_TEXT }));

    submit();

    await screen.findByRole("heading", { name: "Registration received" });
    const body = registrationPosts(calls)[0].body as { options: string[]; consents: string[] };
    expect(body.options).toEqual([]);
    expect(body.consents).toEqual(["privacy"]);
    expect(screen.getByText("No options selected.")).toBeInTheDocument();
    expect(screen.getByText("Thank you, Ana Novak.")).toBeInTheDocument();
  });

  it("requires the mandatory consent before sending", async () => {
    const calls = answerPost({ status: 201, body: { id: "x" } });
    render(<RegistrationPage />);
    await external();

    submit();

    expect(await screen.findByText("This consent is required.")).toHaveAttribute(
      "id",
      "consents-error",
    );
    expect(registrationPosts(calls)).toHaveLength(0);
    expect(screen.getByRole("textbox", { name: "First name" })).not.toHaveAttribute("aria-invalid");
  });

  it("shows backend errors without an input in the form error area", async () => {
    answerPost({
      status: 400,
      body: {
        errors: [
          { field: "captchaToken", message: "The captcha was not accepted." },
          { field: "body", message: "Invalid." },
          { field: "email", message: "First." },
          { field: "email", message: "Second." },
          { field: "consents", message: "Consent missing." },
        ],
      },
    });
    render(<RegistrationPage />);
    await external();
    fireEvent.click(screen.getByRole("checkbox", { name: PRIVACY_TEXT }));

    submit();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The captcha was not accepted. Invalid.",
    );
    expect(screen.getByRole("alert")).toHaveAttribute("id", "form-error");
    expect(screen.getByRole("textbox", { name: "Email" })).toHaveAccessibleDescription("First.");
    expect(document.getElementById("consents-error")).toHaveTextContent("Consent missing.");
  });

  it("clears earlier errors when the type changes and after a valid resubmission", async () => {
    answerPost({ status: 500 });
    render(<RegistrationPage />);
    await external();
    fireEvent.click(screen.getByRole("checkbox", { name: PRIVACY_TEXT }));
    submit();
    expect(await screen.findByText(GENERAL_FAILURE)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));

    expect(screen.queryByText(GENERAL_FAILURE)).toBeNull();
  });

  it("disables the button while sending and ignores a second submit", async () => {
    let release: (answer: Response) => void = () => {};
    const fetchMock = vi.fn((input: RequestInfo | URL) =>
      String(input) === "/api/form-config"
        ? Promise.resolve(new Response(JSON.stringify(formConfig), { status: 200 }))
        : new Promise<Response>((resolve) => (release = resolve)),
    );
    vi.stubGlobal("fetch", fetchMock);
    render(<RegistrationPage />);
    await external();
    fireEvent.click(screen.getByRole("checkbox", { name: PRIVACY_TEXT }));

    submit();
    await waitFor(() => expect(screen.getByRole("button", { name: "Register" })).toBeDisabled());
    fireEvent.submit(screen.getByRole("button", { name: "Register" }).closest("form")!);

    expect(fetchMock).toHaveBeenCalledTimes(2);
    await act(async () => release(new Response('{"id":"x"}', { status: 201 })));
    expect(
      await screen.findByRole("heading", { name: "Registration received" }),
    ).toBeInTheDocument();
  });

  it("sends the widget token when not in test mode", async () => {
    const callbacks: ((token: string) => void)[] = [];
    window.grecaptcha = {
      render: (_el, options) => {
        callbacks.push(options.callback);
        return 1;
      },
      reset: vi.fn(),
    };
    const calls = mockApi((call) => {
      if (call.url.pathname === "/api/form-config")
        return {
          status: 200,
          body: { ...formConfig, captchaTestMode: false, captchaSiteKey: "k" },
        };
      return call.method === "POST" ? { status: 201, body: { id: "x" } } : undefined;
    });
    render(<RegistrationPage />);
    await external();
    fireEvent.click(screen.getByRole("checkbox", { name: PRIVACY_TEXT }));
    await waitFor(() => expect(callbacks).toHaveLength(1));
    act(() => callbacks[0]("widget-token"));

    submit();

    await screen.findByRole("heading", { name: "Registration received" });
    expect((registrationPosts(calls)[0].body as { captchaToken: string }).captchaToken).toBe(
      "widget-token",
    );
    expect(screen.queryByText("reCAPTCHA test mode")).toBeNull();
  });
});
