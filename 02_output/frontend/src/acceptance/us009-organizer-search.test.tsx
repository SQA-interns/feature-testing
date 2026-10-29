import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import App from "../App";
import { stubApi } from "./support/api";
import { chooseType } from "./support/form";
import {
  BASIC,
  EXTERNAL,
  PASSWORD,
  STUDENT,
  USERNAME,
  entries,
  logIn,
  param,
  registrationItems,
  result,
  stubSearchApi,
} from "./support/organizer";

const WRONG_CREDENTIALS = "Wrong username or password.";

function openOrganizerPage() {
  window.history.pushState({}, "", "/organizer");
  return render(<App />);
}

beforeEach(() => {
  window.localStorage.clear();
  window.sessionStorage.clear();
});

afterEach(() => {
  window.history.pushState({}, "", "/");
});

describe("US-009 organizer registration search", () => {
  it("AC-009-01 /organizer shows the login form and no registration data", async () => {
    const api = stubSearchApi(() => ({
      status: 200,
      body: result([EXTERNAL]),
    }));

    openOrganizerPage();

    expect(
      await screen.findByRole("heading", { name: "Organizer registrations" }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Username")).toBeInTheDocument();
    expect(screen.getByLabelText("Password")).toHaveAttribute(
      "type",
      "password",
    );
    expect(screen.getByRole("button", { name: "Log in" })).toBeInTheDocument();
    expect(screen.queryByRole("list", { name: "Registrations" })).toBeNull();
    expect(screen.queryByText(/Novak/)).toBeNull();
    expect(api.fetch).not.toHaveBeenCalled();
  });

  it.each(["External participant", "Student"] as const)(
    "AC-009-02 the registration page for %s has no link to /organizer",
    async (type) => {
      stubApi();
      window.history.pushState({}, "", "/");
      const { container } = render(<App />);

      await chooseType(type);

      expect(screen.getByLabelText("First name")).toBeInTheDocument();
      const links = [...container.querySelectorAll("a")].map(
        (a) => a.getAttribute("href") ?? "",
      );
      expect(links.filter((href) => href.includes("/organizer"))).toEqual([]);
      expect(container.textContent).not.toMatch(/organizer registrations/i);
    },
  );

  it("AC-009-04 wrong credentials show an error and no registration data", async () => {
    stubSearchApi((call) =>
      call.authorization === BASIC
        ? { status: 200, body: result([EXTERNAL]) }
        : { status: 401, body: { message: "Authentication required." } },
    );
    openOrganizerPage();

    logIn(USERNAME, "wrong-password-123456");

    expect(await screen.findByRole("alert")).toHaveTextContent(
      WRONG_CREDENTIALS,
    );
    expect(screen.getByRole("button", { name: "Log in" })).toBeInTheDocument();
    expect(screen.queryByRole("list", { name: "Registrations" })).toBeNull();
    expect(screen.queryByText(/Novak/)).toBeNull();
  });

  it("AC-009-06 valid credentials list every registration newest first with the number of matches", async () => {
    const api = stubSearchApi(() => ({
      status: 200,
      body: result([EXTERNAL, STUDENT]),
    }));
    openOrganizerPage();

    logIn();

    const items = await registrationItems();
    expect(items).toHaveLength(2);
    expect(entries(items[0])["Last name"]).toBe("Novak");
    expect(entries(items[1])["Last name"]).toBe("Kranjc");
    expect(screen.getByText("Matches: 2")).toBeInTheDocument();
    expect(api.calls).toHaveLength(1);
    expect(api.calls[0].authorization).toBe(BASIC);
    for (const name of [
      "search",
      "type",
      "workshop",
      "event",
      "meal",
      "other",
    ]) {
      expect(param(api.calls[0], name)).toBe("");
    }
  });

  it("AC-009-07 a listed registration shows its type, fields, options, consents and time, and nothing else", async () => {
    stubSearchApi(() => ({
      status: 200,
      body: result([EXTERNAL, STUDENT]),
    }));
    openOrganizerPage();

    logIn();

    const [external, student] = await registrationItems();
    expect(entries(external)).toEqual({
      Type: "External participant",
      "Registered at": "2026-10-27T10:05:00Z",
      "First name": "Ana",
      "Last name": "Novak",
      Email: "ana.novak@example.org",
      "Organization / institution": "Example Institute",
      Workshops: "Workshop: Testing in practice",
      Events: "Conference dinner",
      Meals: "Lunch (vegetarian)",
      Consents: "privacy (2026-10-27T10:05:00Z)",
    });
    expect(entries(student)).toEqual({
      Type: "Student",
      "Registered at": "2026-10-27T09:00:00Z",
      "First name": "Luka",
      "Last name": "Kranjc",
      Email: "luka.kranjc@student.example.org",
      "Study institution": "University of Ljubljana",
      "Study programme": "Computer Science",
      "Student ID": "63200001",
      Workshops: "Workshop: Testing in practice",
      Meals: "Lunch (vegetarian)",
      Consents: "privacy (2026-10-27T09:00:00Z)",
    });
    expect(document.body.textContent).not.toContain(EXTERNAL.id);
    expect(document.body.textContent).not.toContain(STUDENT.id);
  });

  it("AC-009-28 Slovenian characters are shown unchanged", async () => {
    const slovenian = {
      ...STUDENT,
      firstName: "Čedomir Žiga",
      lastName: "Šuštaršič",
      studyInstitution: "Inštitut za ŽČŠ raziskave, Črnomelj",
    };
    stubSearchApi(() => ({ status: 200, body: result([slovenian]) }));
    openOrganizerPage();

    logIn();

    const [item] = await registrationItems();
    expect(entries(item)["First name"]).toBe("Čedomir Žiga");
    expect(entries(item)["Last name"]).toBe("Šuštaršič");
    expect(entries(item)["Study institution"]).toBe(
      "Inštitut za ŽČŠ raziskave, Črnomelj",
    );
  });

  it("AC-009-29 markup in registration text appears only as literal text and no script runs", async () => {
    const hostile = {
      ...EXTERNAL,
      firstName: "<script>window.__xss = 1</script>",
      lastName: '<img src="x" onerror="window.__xss = 2">',
      organization: "<b>Bold Org</b>",
    };
    stubSearchApi(() => ({ status: 200, body: result([hostile]) }));
    const { container } = openOrganizerPage();

    logIn();

    const [item] = await registrationItems();
    expect(entries(item)["First name"]).toBe(hostile.firstName);
    expect(entries(item)["Last name"]).toBe(hostile.lastName);
    expect(entries(item)["Organization / institution"]).toBe(
      hostile.organization,
    );
    expect(container.querySelector("script")).toBeNull();
    expect(container.querySelector("img")).toBeNull();
    expect(container.querySelector("b")).toBeNull();
    expect((window as unknown as { __xss?: number }).__xss).toBeUndefined();
  });

  it("AC-009-30 after a reload the login form is shown again and no storage or cookie holds the credentials", async () => {
    stubSearchApi(() => ({ status: 200, body: result([EXTERNAL]) }));
    const page = openOrganizerPage();
    logIn();
    await registrationItems();

    const stored = [
      ...Object.keys(localStorage).map(
        (k) => k + "=" + localStorage.getItem(k),
      ),
      ...Object.keys(sessionStorage).map(
        (k) => k + "=" + sessionStorage.getItem(k),
      ),
      document.cookie,
    ].join("\n");
    expect(stored).not.toContain(PASSWORD);
    expect(stored).not.toContain(USERNAME);
    expect(stored).not.toContain(BASIC.slice("Basic ".length));

    page.unmount();
    openOrganizerPage();

    expect(screen.getByRole("button", { name: "Log in" })).toBeInTheDocument();
    expect(screen.getByLabelText("Password")).toHaveValue("");
    expect(screen.queryByRole("list", { name: "Registrations" })).toBeNull();
  });
});
