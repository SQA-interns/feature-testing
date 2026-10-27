import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RegistrationForm } from "./RegistrationForm";
import type { FormConfig } from "./types";

const config: FormConfig = {
  conferenceName: "C",
  captcha: { mode: "TEST" },
  options: [
    {
      id: "ev-b",
      name: "Dinner",
      category: "EVENT",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "ws-ext",
      name: "Industry",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL"],
    },
    {
      id: "ws-all",
      name: "Testing",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
  ],
  consents: [
    { id: "photos", text: "Photos", mandatory: false },
    { id: "privacy", text: "Privacy", mandatory: true },
  ],
};

const GENERAL = "Your registration could not be sent. Please try again later.";

function stubPost(status: number, body: unknown) {
  const fetch = vi
    .fn()
    .mockResolvedValue(new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fetch);
  return () => JSON.parse(fetch.mock.calls.at(-1)?.[1].body as string);
}

const type = (label: string, value: string) =>
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
const check = (name: string) =>
  fireEvent.click(screen.getByRole("checkbox", { name }));
const submit = () =>
  act(async () => {
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
  });

function fillStudent() {
  fireEvent.click(screen.getByRole("radio", { name: "Student" }));
  type("First name", " Žiga ");
  type("Last name", "Kos");
  type("Email", "z@x.si");
  type("Study institution", "UL");
  type("Study programme", "FRI");
  type("Student ID", "0123");
  check("Privacy (required)");
  check("I am not a robot (test mode)");
}

afterEach(() => {
  delete window.grecaptcha;
});

describe("RegistrationForm", () => {
  it("shows nothing but the type choice before a type is chosen", () => {
    const fetch = vi.fn();
    vi.stubGlobal("fetch", fetch);
    const { container } = render(<RegistrationForm config={config} />);

    expect(screen.queryByRole("button")).toBeNull();
    expect(screen.queryByLabelText("First name")).toBeNull();
    fireEvent.submit(container.querySelector("form")!);
    expect(fetch).not.toHaveBeenCalled();
  });

  it("offers options by type and drops unavailable ones when the type changes", async () => {
    const sent = stubPost(201, {
      id: "1",
      registeredAt: "x",
      type: "STUDENT",
      firstName: "Žiga",
      lastName: "Kos",
      options: [{ id: "ws-all", name: "Testing", category: "WORKSHOP" }],
    });
    render(<RegistrationForm config={config} />);
    fireEvent.click(
      screen.getByRole("radio", { name: "External participant" }),
    );
    expect(screen.getByRole("group", { name: "Workshops" })).toHaveTextContent(
      "Industry",
    );
    expect(screen.queryByRole("group", { name: "Meals" })).toBeNull();
    check("Industry");
    check("Testing");
    check("Dinner");
    check("Dinner");

    fillStudent();
    expect(screen.queryByRole("checkbox", { name: "Industry" })).toBeNull();
    expect(screen.queryByLabelText("Organization / institution")).toBeNull();
    check("Photos");
    await submit();

    expect(sent()).toEqual({
      type: "STUDENT",
      firstName: " Žiga ",
      lastName: "Kos",
      email: "z@x.si",
      studyInstitution: "UL",
      studyProgramme: "FRI",
      studentId: "0123",
      optionIds: ["ws-all"],
      consents: ["photos", "privacy"],
      recaptchaToken: "test-pass",
    });
    expect(await screen.findByText("Testing")).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Registration received" }),
    ).toBeInTheDocument();
  });

  it("checks the form before sending and marks the fields", async () => {
    const fetch = vi.fn();
    vi.stubGlobal("fetch", fetch);
    render(<RegistrationForm config={config} />);
    fireEvent.click(
      screen.getByRole("radio", { name: "External participant" }),
    );
    await submit();

    expect(fetch).not.toHaveBeenCalled();
    expect(screen.getByLabelText("First name")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(screen.getByLabelText("First name")).toHaveAccessibleDescription(
      "This field is required.",
    );
    expect(
      screen.getByRole("checkbox", { name: "Privacy (required)" }),
    ).toHaveAccessibleDescription("This consent is required.");
    expect(
      screen.getByRole("checkbox", { name: "Photos" }),
    ).not.toHaveAttribute("aria-invalid");

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    expect(screen.getByLabelText("First name")).not.toHaveAttribute(
      "aria-invalid",
    );
  });

  it("shows the backend's field errors and a general alert for unknown fields", async () => {
    stubPost(400, {
      message: "rejected",
      fieldErrors: [
        { field: "email", code: "INVALID_FORMAT", message: "x" },
        { field: "optionIds", code: "INACTIVE_OPTION", message: "x" },
        { field: "consents.privacy", code: "CONSENT_REQUIRED", message: "x" },
        { field: "recaptchaToken", code: "CAPTCHA_FAILED", message: "x" },
      ],
    });
    render(<RegistrationForm config={config} />);
    fillStudent();
    await submit();

    expect(screen.getByLabelText("Email")).toHaveAccessibleDescription(
      "Enter a valid email address.",
    );
    expect(
      screen.getByText("One or more selected options are not available."),
    ).toHaveAttribute("id", "optionIds-error");
    expect(
      screen.getByText("Please confirm that you are not a robot."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("alert")).toBeNull();
    expect(
      screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }),
    ).toBeChecked();

    stubPost(400, {
      fieldErrors: [
        { field: "organization", code: "NOT_ALLOWED", message: "x" },
      ],
    });
    await submit();
    expect(screen.getByRole("alert")).toHaveTextContent(GENERAL);
    expect(screen.queryByText("Enter a valid email address.")).toBeNull();
  });

  it("shows the general alert when sending fails, and disables the button meanwhile", async () => {
    let answer: (r: Response) => void = () => {};
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise<Response>((resolve) => (answer = resolve))),
    );
    render(<RegistrationForm config={config} />);
    fillStudent();
    await submit();
    expect(screen.getByRole("button", { name: "Register" })).toBeDisabled();

    await act(async () => answer(new Response("{}", { status: 500 })));

    expect(screen.getByRole("button", { name: "Register" })).toBeEnabled();
    expect(screen.getByRole("alert")).toHaveTextContent(GENERAL);
    fireEvent.click(
      screen.getByRole("radio", { name: "External participant" }),
    );
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("asks reCAPTCHA for a new token after a failed send", async () => {
    const callbacks: Record<string, unknown>[] = [];
    window.grecaptcha = {
      ready: (cb) => cb(),
      render: (_el, p) => {
        callbacks.push(p);
        return 3;
      },
      reset: vi.fn(),
    };
    const sent = stubPost(500, {});
    render(
      <RegistrationForm
        config={{ ...config, captcha: { mode: "RECAPTCHA", siteKey: "k" } }}
      />,
    );
    fireEvent.click(
      screen.getByRole("radio", { name: "External participant" }),
    );
    type("First name", "Ana");
    type("Last name", "Novak");
    type("Email", "a@x.si");
    type("Organization / institution", "I");
    check("Privacy (required)");
    act(() => (callbacks[0].callback as (t: string) => void)("g-token"));
    await submit();

    expect(sent().recaptchaToken).toBe("g-token");
    expect(window.grecaptcha.reset).toHaveBeenCalledWith(3);
    await submit();
    expect(
      screen.getByText("Please confirm that you are not a robot."),
    ).toBeInTheDocument();
  });
});
