import { useEffect, useState } from "react";
import { fetchFormConfig } from "./api/client";
import { RegistrationForm } from "./components/RegistrationForm";
import type { FormConfig } from "./domain/types";
import "./styles.css";

const LOAD_ERROR = "The registration form could not be loaded. Please try again later.";

/** Loads the form configuration, then shows the registration form. */
export function App() {
  const [config, setConfig] = useState<FormConfig | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);

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
      {config !== null && <RegistrationForm config={config} onRegistered={() => undefined} />}
    </main>
  );
}
