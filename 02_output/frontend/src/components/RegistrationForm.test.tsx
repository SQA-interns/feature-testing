import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App, { LOAD_ERROR } from "../App";
import type { FormConfig } from "../types";
import { Confirmation } from "./Confirmation";
import { OptionGroups } from "./OptionGroups";
import { GENERAL_ERROR, RegistrationForm } from "./RegistrationForm";
import { TextInput } from "./TextInput";

const config: FormConfig = {
  captchaSiteKey: "",
  captchaTestMode: true,
  options: [
    {
      id: "ws-both",
      name: "Both workshop",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    { id: "ws-ext", name: "External workshop", category: "WORKSHOP", availableTo: ["EXTERNAL"] },
    { id: "ml", name: "Lunch", category: "MEAL", availableTo: ["STUDENT"] },
  ],
  consents: [{ id: "privacy", text: "I agree", mandatory: true }],
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

function backend(status: number, body: unknown) {
  const posts: unknown[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, init?: RequestInit) => {
      if (url === "/api/form-config") {
        return new Response(JSON.stringify({ ...config, conferenceName: "Konferenca" }), {
          status: 200,
        });
      }
      posts.push(JSON.parse(String(init?.body)));
      return new Response(JSON.stringify(body), { status });
    }),
  );
  return posts;
}

function fill(label: RegExp, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

function fillStudent() {
  fireEvent.click(screen.getByRole("radio", { name: "Student" }));
  fill(/^First name/, " Luka ");
  fill(/^Last name/, "Horvat");
  fill(/^Email/, "l@x.org");
  fill(/^Study institution/, "Uni");
  fill(/^Study programme/, "CS");
  fill(/^Student ID/, "1");
  fireEvent.click(screen.getByRole("checkbox", { name: "I agree" }));
  fireEvent.click(screen.getByRole("checkbox", { name: /test mode/ }));
}

describe("RegistrationForm", () => {
  it("shows no fields before a type is chosen", () => {
    render(<RegistrationForm config={config} onRegistered={vi.fn()} />);
    expect(screen.queryAllByRole("textbox")).toHaveLength(0);
    expect(screen.queryByRole("button", { name: "Register" })).toBeNull();
  });

  it("drops options the new type cannot select and clears errors", () => {
    render(<RegistrationForm config={config} onRegistered={vi.fn()} />);
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Both workshop" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "External workshop" }));
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(screen.getAllByRole("alert").length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));

    expect(screen.queryAllByRole("alert")).toHaveLength(0);
    expect(screen.getByRole("checkbox", { name: "Both workshop" })).toBeChecked();
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
    expect(screen.getByRole("checkbox", { name: "External workshop" })).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Both workshop" })).toBeChecked();
  });

  it("sends a student request with trimmed names in the confirmation", async () => {
    const posts = backend(201, { id: "x" });
    const onRegistered = vi.fn();
    render(<RegistrationForm config={config} onRegistered={onRegistered} />);
    fillStudent();
    fireEvent.click(screen.getByRole("checkbox", { name: "Lunch" }));

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() => expect(onRegistered).toHaveBeenCalled());
    expect(posts).toEqual([
      {
        type: "STUDENT",
        firstName: " Luka ",
        lastName: "Horvat",
        email: "l@x.org",
        organization: null,
        studyInstitution: "Uni",
        studyProgramme: "CS",
        studentId: "1",
        options: ["ml"],
        consents: ["privacy"],
        captchaToken: "test-pass",
      },
    ]);
    expect(onRegistered).toHaveBeenCalledWith({
      firstName: "Luka",
      lastName: "Horvat",
      optionNames: ["Lunch"],
    });
  });

  it("shows field errors it cannot place as a general message", async () => {
    backend(400, {
      errors: [
        { field: "type", message: "Choose a type." },
        { field: "body", message: "Bad body." },
        { field: "consents", message: "Consent needed." },
        { field: "captchaToken", message: "Robot?" },
      ],
    });
    render(<RegistrationForm config={config} onRegistered={vi.fn()} />);
    fillStudent();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByText("Choose a type. Bad body.")).toBeVisible();
    expect(screen.getByText("Consent needed.")).toBeVisible();
    expect(screen.getByRole("checkbox", { name: /test mode/ })).toHaveAccessibleDescription(
      "Robot?",
    );
    expect(screen.getByRole("checkbox", { name: /test mode/ })).toBeChecked();
  });

  it("resets a Google captcha after a failed submission", async () => {
    backend(500, {});
    const reset = vi.fn();
    window.grecaptcha = {
      ready: (callback) => callback(),
      render: (_c, p) => {
        p.callback("google-token");
        return 3;
      },
      reset,
    };
    render(
      <RegistrationForm
        config={{ ...config, captchaTestMode: false, captchaSiteKey: "site" }}
        onRegistered={vi.fn()}
      />,
    );
    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    fill(/^First name/, "L");
    fill(/^Last name/, "H");
    fill(/^Email/, "l@x.org");
    fill(/^Study institution/, "U");
    fill(/^Study programme/, "P");
    fill(/^Student ID/, "1");
    fireEvent.click(screen.getByRole("checkbox", { name: "I agree" }));

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByText(GENERAL_ERROR)).toBeVisible();
    expect(reset).toHaveBeenCalledWith(3);
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(await screen.findByText("Please confirm that you are not a robot.")).toBeVisible();
    delete window.grecaptcha;
  });

  it("ignores a second submit while the first is running", async () => {
    let release: (r: Response) => void = () => undefined;
    const fetchMock = vi.fn(
      () =>
        new Promise<Response>((resolve) => {
          release = resolve;
        }),
    );
    vi.stubGlobal("fetch", fetchMock);
    render(<RegistrationForm config={config} onRegistered={vi.fn()} />);
    fillStudent();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(screen.getByRole("button", { name: "Register" })).toBeDisabled();
    fireEvent.submit(
      screen.getByRole("button", { name: "Register" }).closest("form") as HTMLFormElement,
    );
    release(new Response(JSON.stringify({ id: "x" }), { status: 201 }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
  });
});

describe("App", () => {
  it("names the conference and shows the confirmation after registering", async () => {
    backend(201, { id: "x" });
    render(<App />);
    expect(screen.getByText("Loading…")).toBeVisible();
    expect(await screen.findByRole("heading", { name: "Konferenca: registration" })).toBeVisible();
    fillStudent();
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(await screen.findByRole("heading", { name: "Registration received" })).toBeVisible();
    expect(screen.queryByText("Loading…")).toBeNull();
  });

  it("explains when the form cannot be loaded", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("", { status: 503 })),
    );
    render(<App />);
    expect(await screen.findByRole("alert")).toHaveTextContent(LOAD_ERROR);
    expect(screen.getByRole("heading", { name: "Conference registration" })).toBeVisible();
    expect(screen.queryByText("Loading…")).toBeNull();
  });

  it("does not update after unmounting", async () => {
    let release: (r: Response) => void = () => undefined;
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise<Response>((resolve) => (release = resolve))),
    );
    const { unmount } = render(<App />);
    unmount();
    release(new Response("", { status: 500 }));
    await new Promise((r) => setTimeout(r, 0));
    expect(document.body.textContent).toBe("");
  });
});

describe("small components", () => {
  it("TextInput links its error and marks the input invalid", () => {
    const onChange = vi.fn();
    const { rerender } = render(
      <TextInput id="email" label="Email" value="" onChange={onChange} />,
    );
    const input = screen.getByLabelText("Email");
    expect(input).toHaveAttribute("type", "text");
    expect(input).not.toHaveAttribute("aria-invalid");
    fireEvent.change(input, { target: { value: "a" } });
    expect(onChange).toHaveBeenCalledWith("a");

    rerender(
      <TextInput id="email" label="Email" type="email" value="a" error="Bad" onChange={onChange} />,
    );
    expect(input).toHaveAttribute("type", "email");
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveAccessibleDescription("Bad");
  });

  it("OptionGroups hides empty groups and reports toggles", () => {
    const onToggle = vi.fn();
    render(
      <OptionGroups
        options={config.options.slice(0, 1)}
        selected={new Set()}
        error="Nope"
        onToggle={onToggle}
      />,
    );
    expect(screen.getAllByRole("group")).toHaveLength(1);
    expect(screen.getByRole("group", { name: "Workshops" })).toBeVisible();
    fireEvent.click(screen.getByRole("checkbox", { name: "Both workshop" }));
    expect(onToggle).toHaveBeenCalledWith("ws-both");
    expect(screen.getByRole("alert")).toHaveTextContent("Nope");
  });

  it("Confirmation lists options or says there are none", () => {
    const { rerender } = render(
      <Confirmation firstName="A" lastName="B" optionNames={["Lunch"]} />,
    );
    expect(screen.getByRole("listitem")).toHaveTextContent("Lunch");
    expect(screen.getByText(/Thank you, A B\./)).toBeVisible();
    rerender(<Confirmation firstName="A" lastName="B" optionNames={[]} />);
    expect(screen.queryByRole("list")).toBeNull();
    expect(screen.getByText("No optional activities selected.")).toBeVisible();
  });
});
