import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import App from "../App";
import { stubApi } from "./support/api";
import {
  GENERAL_ERROR,
  PRIVACY_CONSENT,
  TEST_CAPTCHA,
  check,
  chooseType,
  fillValidExternal,
  submit,
  type,
} from "./support/form";

const accepted = {
  id: "3f2b8c1e-7a4d-4e6b-9f10-2c5d8e9a0b11",
  registeredAt: "2026-10-27T10:00:00Z",
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  options: [
    {
      id: "ws-testing",
      name: "Workshop: Testing in practice",
      category: "WORKSHOP",
    },
  ],
};

function expectValuesKept() {
  expect(screen.getByLabelText("First name")).toHaveValue("Ana");
  expect(screen.getByLabelText("Last name")).toHaveValue("Novak");
  expect(screen.getByLabelText("Email")).toHaveValue("ana.novak@example.org");
  expect(screen.getByLabelText("Organization / institution")).toHaveValue(
    "Example Institute",
  );
  expect(
    screen.getByRole("checkbox", { name: "Workshop: Testing in practice" }),
  ).toBeChecked();
}

describe("US-004 registration confirmation", () => {
  it("AC-004-01 shows the confirmation with name and selected options and hides the form after acceptance", async () => {
    const api = stubApi({ kind: "status", status: 201, body: accepted });
    render(<App />);
    await fillValidExternal();

    submit();

    expect(
      await screen.findByRole("heading", { name: "Registration received" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Thank you, Ana Novak.")).toBeInTheDocument();
    expect(
      screen.getByText("Workshop: Testing in practice"),
    ).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Register" })).toBeNull();
    expect(screen.queryByLabelText("First name")).toBeNull();
    expect(api.registrations).toHaveLength(1);
  });

  it("AC-004-02 shows backend field errors next to their fields, no confirmation, and keeps the values", async () => {
    stubApi({
      kind: "status",
      status: 400,
      body: {
        message: "The registration was rejected.",
        fieldErrors: [
          { field: "email", code: "INVALID_FORMAT", message: "invalid" },
          {
            field: "optionIds",
            code: "OPTION_NOT_AVAILABLE",
            message: "not available",
          },
        ],
      },
    });
    render(<App />);
    await fillValidExternal();

    submit();

    const email = screen.getByLabelText("Email");
    expect(
      await screen.findByText("Enter a valid email address."),
    ).toBeInTheDocument();
    expect(email).toHaveAttribute("aria-invalid", "true");
    expect(email).toHaveAccessibleDescription("Enter a valid email address.");
    expect(
      screen.getByText("One or more selected options are not available."),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Registration received" }),
    ).toBeNull();
    expectValuesKept();
  });

  it("AC-004-03 shows field errors for an empty required field and an invalid email without sending a request", async () => {
    const api = stubApi({ kind: "status", status: 201, body: accepted });
    render(<App />);
    await chooseType("External participant");
    type("Last name", "Novak");
    type("Email", "ana.novak@");
    type("Organization / institution", "Example Institute");
    check(PRIVACY_CONSENT);
    check(TEST_CAPTCHA);

    submit();

    const firstName = screen.getByLabelText("First name");
    const email = screen.getByLabelText("Email");
    expect(
      await screen.findByText("This field is required."),
    ).toBeInTheDocument();
    expect(firstName).toHaveAttribute("aria-invalid", "true");
    expect(firstName).toHaveAccessibleDescription("This field is required.");
    expect(email).toHaveAttribute("aria-invalid", "true");
    expect(email).toHaveAccessibleDescription("Enter a valid email address.");
    expect(api.registrations).toHaveLength(0);
    expect(
      api.fetch.mock.calls.some(([input]) =>
        String(input instanceof Request ? input.url : input).includes(
          "/api/registrations",
        ),
      ),
    ).toBe(false);
  });

  it.each([
    [
      "the backend fails (500)",
      {
        kind: "status",
        status: 500,
        body: { message: "The request could not be processed." },
      } as const,
    ],
    ["the backend cannot be reached", { kind: "network-error" } as const],
  ])(
    "AC-004-04 shows a general error without technical details when %s",
    async (_case, reply) => {
      stubApi(reply);
      render(<App />);
      await fillValidExternal();

      submit();

      const alert = await screen.findByRole("alert");
      expect(alert).toHaveTextContent(GENERAL_ERROR);
      expect(alert.textContent?.trim()).toBe(GENERAL_ERROR);
      expect(
        screen.queryByRole("heading", { name: "Registration received" }),
      ).toBeNull();
      expectValuesKept();
    },
  );
});
