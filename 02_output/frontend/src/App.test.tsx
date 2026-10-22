import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";

afterEach(() => {
  vi.unstubAllGlobals();
  window.history.pushState({}, "", "/");
});

describe("App routing", () => {
  it.each(["/organizer", "/organizer/", "/organizer//"])(
    "shows the organizer page at %s",
    (path) => {
      vi.stubGlobal(
        "fetch",
        vi.fn(() => new Promise(() => {})),
      );
      window.history.pushState({}, "", path);

      render(<App />);

      expect(screen.getByRole("heading", { name: "Organizer sign-in" })).toBeVisible();
    },
  );

  it.each(["/", "/organizers", "/x/organizer"])("shows the registration page at %s", (path) => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise(() => {})),
    );
    window.history.pushState({}, "", path);

    render(<App />);

    expect(screen.getByText("Loading…")).toBeVisible();
  });
});
