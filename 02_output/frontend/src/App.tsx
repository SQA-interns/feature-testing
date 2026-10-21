import { useEffect, useState } from "react";
import { fetchFormConfig } from "./api/client";
import { Confirmation } from "./components/Confirmation";
import { RegistrationForm, type Registered } from "./components/RegistrationForm";
import type { FormConfig } from "./domain/types";
import "./styles.css";

const LOAD_ERROR = "The registration form could not be loaded. Please try again later.";

/** Loads the form configuration, shows the form and, once accepted, the confirmation. */
export function App() {
  const [config, setConfig] = useState<FormConfig | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [registered, setRegistered] = useState<Registered | null>(null);

  useEffect(() => {
    let active = true;
    fetchFormConfig()
      .then((c) => active && setConfig(c))
      .catch(() => active && setLoadFailed(true));
    return () => {
      active = false;
    };
  }, []);

  return (
    <main>
      <h1>Conference registration</h1>
      {loadFailed && (
        <p role="alert" id="general-error" className="general-error">
          {LOAD_ERROR}
        </p>
      )}
      {!loadFailed && config === null && <p>Loading…</p>}
      {registered !== null && <Confirmation {...registered} />}
      {config !== null && registered === null && (
        <RegistrationForm config={config} onRegistered={setRegistered} />
      )}
    </main>
  );
}
