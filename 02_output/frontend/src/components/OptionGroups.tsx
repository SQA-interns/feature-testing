import type { FormOption, OptionCategory } from "../types";

export const GROUP_TITLES: [OptionCategory, string][] = [
  ["WORKSHOP", "Workshops"],
  ["EVENT", "Events"],
  ["MEAL", "Meals"],
  ["OTHER", "Other activities"],
];

interface OptionGroupsProps {
  options: FormOption[];
  selected: ReadonlySet<string>;
  error?: string;
  onToggle: (id: string) => void;
}

/** The options of the chosen type as checkboxes, grouped by category (BR-04); none preselected. */
export function OptionGroups({ options, selected, error, onToggle }: OptionGroupsProps) {
  return (
    <div className="options">
      {GROUP_TITLES.map(([category, title]) => {
        const inGroup = options.filter((option) => option.category === category);
        if (inGroup.length === 0) {
          return null;
        }
        return (
          <fieldset key={category}>
            <legend>{title}</legend>
            {inGroup.map((option) => (
              <div className="choice" key={option.id}>
                <input
                  type="checkbox"
                  id={`option-${option.id}`}
                  checked={selected.has(option.id)}
                  onChange={() => onToggle(option.id)}
                />
                <label htmlFor={`option-${option.id}`}>{option.name}</label>
              </div>
            ))}
          </fieldset>
        );
      })}
      {error && (
        <p id="options-error" role="alert" className="error">
          {error}
        </p>
      )}
    </div>
  );
}
