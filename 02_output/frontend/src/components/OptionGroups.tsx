import { CATEGORIES, type FormOption } from "../domain/types";

interface Props {
  options: FormOption[];
  selected: string[];
  error?: string;
  onToggle: (id: string) => void;
}

/** One fieldset per category with options, in the order of `CATEGORIES` (BR-04). */
export function OptionGroups({ options, selected, error, onToggle }: Props) {
  return (
    <div className="options">
      {CATEGORIES.map(({ category, legend }) => {
        const inCategory = options.filter((o) => o.category === category);
        if (inCategory.length === 0) return null;
        return (
          <fieldset key={category} aria-describedby={error ? "options-error" : undefined}>
            <legend>{legend}</legend>
            {inCategory.map((option) => (
              <label key={option.id} className="choice">
                <input
                  type="checkbox"
                  name="options"
                  value={option.id}
                  checked={selected.includes(option.id)}
                  onChange={() => onToggle(option.id)}
                />
                {option.name}
              </label>
            ))}
          </fieldset>
        );
      })}
      {error && (
        <span id="options-error" className="error">
          {error}
        </span>
      )}
    </div>
  );
}
