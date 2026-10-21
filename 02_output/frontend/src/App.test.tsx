import "@testing-library/jest-dom/vitest";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";
import type { FormConfig } from "./domain/types";

const config: FormConfig = {
  options: [
    { id: "ws", name: "Workshop A", category: "WORKSHOP", availableTo: ["EXTERNAL", "STUDENT"] },
    { id: "tour", name: "Tour", category: "EVENT", availableTo: ["EXTERNAL"] },
  ],
  consents: [{ id: "privacy", text: "Privacy", mandatory: true }],
  captchaSiteKey: "",
  captchaTestMode: true,
};

function stub(post: (body: Record<string, unknown>) => Response | Promise<Response>) {
  const posts: Record<string, unknown>[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, init?: RequestInit) => {
      if (url === "/api/form-config") return new Response(JSON.stringify(config), { status: 200 });
      const body = JSON.parse(String(init?.body)) as Record<string, unknown>;
      posts.push(body);
      return post(body);
    }),
  );
  return posts;
}

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

async function fillExternal() {
  fireEvent.click(await screen.findByRole("radio", { name: "External participant" }));
  for (const [label, value] of [
    ["First name", "Ana"],
    ["Last name", "Novak"],
    ["Email", "ana@example.org"],
    ["Organization / institution", "Org"],
  ]) {
    fireEvent.change(screen.getByRole("textbox", { name: label }), { target: { value } });
  }
  fireEvent.click(screen.getByRole("checkbox", { name: "Privacy" }));
  fireEvent.click(screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }));
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("App", () => {
  it("shows loading until the configuration is there, then only the type chooser", async () => {
    stub(() => json(201, { id: "x" }));
    render(<App />);

    expect(screen.getByText("Loading…")).toBeInTheDocument();
    expect(
      await screen.findByRole("radiogroup", { name: "Registration type" }),
    ).toBeInTheDocument();
    expect(screen.queryByText("Loading…")).not.toBeInTheDocument();
    expect(screen.queryAllByRole("textbox")).toHaveLength(0);
    expect(screen.queryByRole("button", { name: "Register" })).not.toBeInTheDocument();
  });

  it("shows a general error when the configuration cannot be loaded", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("", { status: 500 })),
    );
    render(<App />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The registration form could not be loaded. Please try again later.",
    );
    expect(screen.queryByText("Loading…")).not.toBeInTheDocument();
  });

  it("keeps entered text, drops options the new type cannot have, and has no field errors yet", async () => {
    const posts = stub(() => json(201, { id: "x" }));
    render(<App />);
    await fillExternal();
    fireEvent.click(screen.getByRole("checkbox", { name: "Tour" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop A" }));

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));

    expect(screen.getByRole("textbox", { name: "First name" })).toHaveValue("Ana");
    expect(screen.getByRole("checkbox", { name: "Tour" })).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Workshop A" })).toBeChecked();
    expect(screen.getByRole("textbox", { name: "Email" })).not.toHaveAttribute("aria-invalid");
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop A" }));
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    await screen.findByRole("status");
    expect(posts[0].options).toEqual([]);
  });

  it("shows backend errors of unknown fields as the general error and keeps known ones", async () => {
    stub(() =>
      json(400, {
        errors: [
          { field: "body", message: "Request body is not valid JSON." },
          { field: "type", message: "Registration type must be EXTERNAL or STUDENT." },
          { field: "email", message: "First" },
          { field: "email", message: "Second" },
          { field: "consents", message: "Consent 'privacy' is required." },
          { field: "captchaToken", message: "The reCAPTCHA check failed." },
        ],
      }),
    );
    render(<App />);
    await fillExternal();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The registration could not be completed. Please try again later.",
    );
    expect(document.getElementById("type-error")).toHaveTextContent(
      "Registration type must be EXTERNAL or STUDENT.",
    );
    expect(document.getElementById("email-error")).toHaveTextContent("First");
    expect(document.getElementById("consents-error")).toHaveTextContent("required");
    expect(document.getElementById("captchaToken-error")).toHaveTextContent("failed");
    expect(screen.getByRole("group", { name: "Consents" })).toHaveAttribute(
      "aria-describedby",
      "consents-error",
    );
  });

  it("clears the general error on the next submission and shows no general error for known fields", async () => {
    let answer: Response = json(500, { message: "x" });
    stub(() => answer);
    render(<App />);
    await fillExternal();
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    await screen.findByRole("alert");

    answer = json(400, {
      errors: [{ field: "options", message: "Option 'ws' is not available." }],
    });
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() => expect(document.getElementById("options-error")).not.toBeNull());
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByRole("group", { name: "Workshops" })).toHaveAttribute(
      "aria-describedby",
      "options-error",
    );
  });

  it("disables the button while the submission is in flight and sends it once", async () => {
    let release: (r: Response) => void = () => {};
    const posts = stub(() => new Promise<Response>((resolve) => (release = resolve)));
    render(<App />);
    await fillExternal();
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop A" }));

    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    await waitFor(() => expect(screen.getByRole("button", { name: "Register" })).toBeDisabled());
    fireEvent.submit(screen.getByRole("button", { name: "Register" }).closest("form")!);
    release(json(201, { id: "x" }));

    const status = await screen.findByRole("status");
    expect(within(status).getByRole("list", { name: "Selected options" })).toHaveTextContent(
      "Workshop A",
    );
    expect(posts).toHaveLength(1);
  });
});
