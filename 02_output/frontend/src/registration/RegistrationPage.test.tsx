import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { FormConfig } from "../api";
import { GENERAL_ERROR, RegistrationPage } from "./RegistrationPage";

const config: FormConfig = {
  options: [
    { id: "ws", name: "Workshop", category: "WORKSHOP", availableTo: ["EXTERNAL", "STUDENT"] },
    { id: "ex", name: "Dinner", category: "EVENT", availableTo: ["EXTERNAL"] },
  ],
  consents: [{ id: "privacy", text: "I agree.", mandatory: true }],
  captchaSiteKey: "site-key",
  captchaTestMode: false,
};

let rendered: { callback: (token: string) => void } | null = null;

function backend(post: (body: Record<string, unknown>) => Response, formConfig = config) {
  const posts: Record<string, unknown>[] = [];
  window.grecaptcha = {
    render: vi.fn((_c: HTMLElement, parameters: { callback: (t: string) => void }) => {
      rendered = parameters;
      return 1;
    }),
    reset: vi.fn(),
  };
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, init?: RequestInit) => {
      if (url === "/api/form-config") return Response.json(formConfig);
      const body = JSON.parse(String(init?.body)) as Record<string, unknown>;
      posts.push(body);
      return post(body);
    }),
  );
  return posts;
}

function fill() {
  fireEvent.change(screen.getByLabelText("First name"), { target: { value: " Ana " } });
  fireEvent.change(screen.getByLabelText("Last name"), { target: { value: "Novak" } });
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: "ana@example.org" } });
  fireEvent.change(screen.getByLabelText("Organization / institution"), {
    target: { value: "Org" },
  });
  fireEvent.click(screen.getByRole("checkbox", { name: /I agree/ }));
}

afterEach(() => {
  vi.unstubAllGlobals();
  delete window.grecaptcha;
  rendered = null;
});

describe("RegistrationPage", () => {
  it("says so when the form configuration cannot be loaded", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("offline")));
    render(<RegistrationPage />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The registration form could not be loaded",
    );
    expect(screen.queryByText("Loading…")).toBeNull();
  });

  it("shows a loading text first", () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise(() => {})),
    );
    render(<RegistrationPage />);

    expect(screen.getByText("Loading…")).toBeInTheDocument();
  });

  it("sends the widget token, trimmed values and nulls for the other type", async () => {
    const posts = backend(() => Response.json({ id: "1" }, { status: 201 }));
    render(<RegistrationPage />);
    fireEvent.click(await screen.findByRole("radio", { name: "External participant" }));
    fill();
    await vi.waitFor(() => expect(rendered).not.toBeNull());
    act(() => rendered?.callback("widget-token"));

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByText("No options selected")).toBeVisible();
    expect(screen.getByText("Thank you, Ana Novak.")).toBeVisible();
    expect(posts[0]).toEqual({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana@example.org",
      organization: "Org",
      studyInstitution: null,
      studyProgramme: null,
      studentId: null,
      options: [],
      consents: ["privacy"],
      captchaToken: "widget-token",
    });
  });

  it("resets the widget and shows unplaced errors as a general message", async () => {
    backend(() =>
      Response.json(
        {
          errors: [
            { field: "captchaToken", message: "Robot check failed." },
            { field: "captchaToken", message: "second message" },
            { field: "type", message: "Choose a type." },
          ],
        },
        { status: 400 },
      ),
    );
    render(<RegistrationPage />);
    fireEvent.click(await screen.findByRole("radio", { name: "External participant" }));
    fill();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByText("Robot check failed.")).toBeVisible();
    expect(screen.queryByText("second message")).toBeNull();
    expect(screen.getByRole("alert")).toHaveTextContent("Choose a type.");
    expect(window.grecaptcha?.reset).toHaveBeenCalledWith(1);
  });

  it("clears the general error when the backend rejects only known fields", async () => {
    let answer = Response.json({}, { status: 500 });
    backend(() => answer);
    render(<RegistrationPage />);
    fireEvent.click(await screen.findByRole("radio", { name: "External participant" }));
    fill();
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(GENERAL_ERROR);

    answer = Response.json({ errors: [{ field: "email", message: "Taken." }] }, { status: 400 });
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByText("Taken.")).toBeVisible();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("drops options the new type cannot select and lets options be unchecked", async () => {
    const posts = backend(() => Response.json({ id: "1" }, { status: 201 }), {
      ...config,
      captchaTestMode: true,
    });
    render(<RegistrationPage />);
    fireEvent.click(await screen.findByRole("radio", { name: "External participant" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Dinner" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop" }));
    expect(screen.getByRole("checkbox", { name: "Workshop" })).not.toBeChecked();
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop" }));
    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    expect(screen.queryByRole("checkbox", { name: "Dinner" })).toBeNull();
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
    expect(screen.getByRole("checkbox", { name: "Dinner" })).not.toBeChecked();
    fill();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByRole("heading", { name: "Registration received" })).toBeVisible();
    expect(posts[0].options).toEqual(["ws"]);
    expect(posts[0].captchaToken).toBe("test-pass");
    expect(screen.getByText("Workshop")).toBeVisible();
  });

  it("clears a field's error when the field changes", async () => {
    backend(() => Response.json({ id: "1" }, { status: 201 }));
    render(<RegistrationPage />);
    fireEvent.click(await screen.findByRole("radio", { name: "External participant" }));
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(screen.getByLabelText("Last name")).toHaveAccessibleDescription(
      "This field is required.",
    );
    expect(screen.getByLabelText("Last name")).toHaveAttribute("aria-invalid", "true");

    fireEvent.change(screen.getByLabelText("Last name"), { target: { value: "N" } });

    expect(screen.getByLabelText("Last name")).not.toHaveAttribute("aria-describedby");
    expect(screen.getByLabelText("Last name")).not.toHaveAttribute("aria-invalid");
    expect(screen.getByLabelText("First name")).toHaveAccessibleDescription(
      "This field is required.",
    );
    expect(screen.getByText("This consent is required.")).toBeVisible();
  });
});
