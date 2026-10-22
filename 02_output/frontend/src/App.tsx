import { OrganizerPage } from "./OrganizerPage";
import { RegistrationPage } from "./RegistrationPage";

/** Routes by path: /organizer is the organizer page, everything else the registration form. */
export function App() {
  return window.location.pathname.startsWith("/organizer") ? (
    <OrganizerPage />
  ) : (
    <RegistrationPage />
  );
}
