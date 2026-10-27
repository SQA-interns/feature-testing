import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import App from "./App";
import { Confirmation } from "./Confirmation";

describe("App", () => {
  it("shows loading, then the conference and the form", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            conferenceName: "Konferenca 2026",
            captcha: { mode: "TEST" },
            options: [],
            consents: [],
          }),
          { status: 200 },
        ),
      ),
    );
    render(<App />);

    expect(screen.getByText("Loading…")).toBeInTheDocument();
    expect(await screen.findByText("Konferenca 2026")).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: "Student" })).toBeInTheDocument();
    expect(screen.queryByText("Loading…")).toBeNull();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("shows an alert when the configuration cannot be loaded", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response("", { status: 500 })),
    );
    render(<App />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The registration form is not available right now. Please try again later.",
    );
    expect(screen.queryByRole("radio")).toBeNull();
  });

  it("ignores a late answer after unmounting", async () => {
    let answer: (r: Response) => void = () => {};
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise<Response>((resolve) => (answer = resolve))),
    );
    const error = vi.spyOn(console, "error");
    const { unmount } = render(<App />);
    unmount();
    answer(new Response("", { status: 500 }));
    await new Promise((r) => setTimeout(r, 0));

    expect(error).not.toHaveBeenCalled();
  });
});

describe("Confirmation", () => {
  it("says when no options were selected", () => {
    render(
      <Confirmation
        registration={{
          id: "1",
          registeredAt: "2026-10-27T10:00:00Z",
          type: "STUDENT",
          firstName: "Žiga",
          lastName: "Kos",
          options: [],
        }}
      />,
    );

    expect(
      screen.getByRole("heading", { name: "Registration received" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Thank you, Žiga Kos.")).toBeInTheDocument();
    expect(screen.getByText("No options selected.")).toBeInTheDocument();
    expect(screen.queryByRole("list")).toBeNull();
  });
});
