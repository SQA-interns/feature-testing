import { RegistrationPage } from "./registration/RegistrationPage";

/** `/` is the registration page (the organizer page joins in US-009). */
export function App() {
  return (
    <main>
      <h1>Conference registration</h1>
      <RegistrationPage />
    </main>
  );
}
