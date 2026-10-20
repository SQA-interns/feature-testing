interface TextInputProps {
  id: string;
  label: string;
  value: string;
  error?: string;
  type?: "text" | "email";
  autoComplete?: string;
  onChange: (value: string) => void;
}

/** A labelled input; its error is shown right after it and linked by aria-describedby. */
export function TextInput({
  id,
  label,
  value,
  error,
  type = "text",
  autoComplete,
  onChange,
}: TextInputProps) {
  const errorId = `${id}-error`;
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        name={id}
        type={type}
        value={value}
        autoComplete={autoComplete}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      {error && (
        <p id={errorId} role="alert" className="error">
          {error}
        </p>
      )}
    </div>
  );
}
