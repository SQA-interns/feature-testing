import { useEffect, useState } from "react";
import { fetchFormConfig } from "./api";
import { Confirmation, type Confirmed } from "./components/Confirmation";
import { RegistrationForm } from "./components/RegistrationForm";
import type { FormConfig } from "./types";

export const LOAD_ERROR = "The registration form could not be loaded. Please try again later.";

export default function App() {
  const [config, setConfig] = useState<FormConfig | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [confirmed, setConfirmed] = useState<Confirmed | null>(null);

  useEffect(() => {
    let active = true;
    fetchFormConfig()
      .then((loaded) => active && setConfig(loaded))
      .catch(() => active && setLoadFailed(true));
    return () => {
      active = false;
    };
  }, []);

  return (
    <main>
      <h1>
        {config?.conferenceName
          ? `${config.conferenceName}: registration`
          : "Conference registration"}
      </h1>
      {loadFailed && (
        <p role="alert" className="error general">
          {LOAD_ERROR}
        </p>
      )}
      {!loadFailed && !config && <p>Loading…</p>}
      {config && !confirmed && <RegistrationForm config={config} onRegistered={setConfirmed} />}
      {confirmed && <Confirmation {...confirmed} />}
    </main>
  );
}
