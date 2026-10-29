import { useEffect, useState } from "react";
import { fetchFormConfig } from "./api";
import { MESSAGES } from "./messages";
import { OrganizerPage } from "./OrganizerPage";
import { RegistrationForm } from "./RegistrationForm";
import { isOrganizerPath } from "./routes";
import type { FormConfig } from "./types";

type State =
  | { status: "loading" }
  | { status: "error" }
  | { status: "ready"; config: FormConfig };

/** The organizer page at /organizer (BR-09), the registration page anywhere else. */
export default function App() {
  return isOrganizerPath(window.location.pathname) ? (
    <OrganizerPage />
  ) : (
    <RegistrationPage />
  );
}

function RegistrationPage() {
  const [state, setState] = useState<State>({ status: "loading" });

  useEffect(() => {
    let active = true;
    fetchFormConfig()
      .then((config) => active && setState({ status: "ready", config }))
      .catch(() => active && setState({ status: "error" }));
    return () => {
      active = false;
    };
  }, []);

  return (
    <main>
      <h1>Conference registration</h1>
      {state.status === "loading" && <p>Loading…</p>}
      {state.status === "error" && (
        <p role="alert" className="alert">
          {MESSAGES.configUnavailable}
        </p>
      )}
      {state.status === "ready" && (
        <>
          <p className="conference">{state.config.conferenceName}</p>
          <RegistrationForm config={state.config} />
        </>
      )}
    </main>
  );
}
