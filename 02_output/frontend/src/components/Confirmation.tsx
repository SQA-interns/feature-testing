export interface Confirmed {
  firstName: string;
  lastName: string;
  optionNames: string[];
}

/** Shown only after the backend accepted the registration (BR-06, AC-004-01). */
export function Confirmation({ firstName, lastName, optionNames }: Confirmed) {
  return (
    <section className="confirmation" aria-labelledby="confirmation-title">
      <h2 id="confirmation-title">Registration received</h2>
      <p>
        Thank you, {firstName} {lastName}. Your registration was received and a confirmation email
        is on its way.
      </p>
      {optionNames.length > 0 ? (
        <>
          <p>Selected options:</p>
          <ul>
            {optionNames.map((name) => (
              <li key={name}>{name}</li>
            ))}
          </ul>
        </>
      ) : (
        <p>No optional activities selected.</p>
      )}
    </section>
  );
}
