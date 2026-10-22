import { OrganizerPage } from "./organizer/OrganizerPage";
import { RegistrationPage } from "./registration/RegistrationPage";

/** `/` is the registration page, `/organizer` the organizer search page. */
export function App() {
  const organizer = window.location.pathname.replace(/\/+$/, "") === "/organizer";
  return (
    <main>
      <h1>Conference registration</h1>
      {organizer ? <OrganizerPage /> : <RegistrationPage />}
    </main>
  );
}
