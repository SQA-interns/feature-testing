import type { RegistrationAccepted } from "./types";

/** Shown only after the backend accepted the registration (BR-06, AC-004-01). */
export function Confirmation({
  registration,
}: {
  registration: RegistrationAccepted;
}) {
  return (
    <section className="confirmation">
      <h2>Registration received</h2>
      <p>
        Thank you, {registration.firstName} {registration.lastName}.
      </p>
      <h3>Selected options</h3>
      {registration.options.length === 0 ? (
        <p>No options selected.</p>
      ) : (
        <ul>
          {registration.options.map((option) => (
            <li key={option.id}>{option.name}</li>
          ))}
        </ul>
      )}
    </section>
  );
}
