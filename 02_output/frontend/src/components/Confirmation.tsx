import type { Registered } from "./RegistrationForm";

/** Shown only after the backend accepted the registration (BR-06, AC-004-01). */
export function Confirmation({ firstName, lastName, optionNames }: Registered) {
  return (
    <section role="status" className="confirmation">
      <h2>Registration received</h2>
      <p>
        Thank you, {firstName} {lastName}. Your registration has been received.
      </p>
      {optionNames.length > 0 ? (
        <ul aria-label="Selected options">
          {optionNames.map((name) => (
            <li key={name}>{name}</li>
          ))}
        </ul>
      ) : (
        <p>No options selected.</p>
      )}
    </section>
  );
}
