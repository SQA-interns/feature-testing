import { Fragment } from "react";
import { ORGANIZER_MESSAGES } from "./messages";
import type { OptionCategory, SearchRegistration, SearchResult } from "./types";

const CATEGORY_TITLES: Record<OptionCategory, string> = {
  WORKSHOP: "Workshops",
  EVENT: "Events",
  MEAL: "Meals",
  OTHER: "Other activities",
};

const TYPE_LABELS = {
  EXTERNAL: "External participant",
  STUDENT: "Student",
};

/** Label and value of each piece of a registration's export data (BR-10); no id (D-24). */
function rows(r: SearchRegistration): [string, string][] {
  const fields: [string, string | undefined][] = [
    ["Type", TYPE_LABELS[r.type]],
    ["Registered at", r.registeredAt],
    ["First name", r.firstName],
    ["Last name", r.lastName],
    ["Email", r.email],
    ["Organization / institution", r.organization],
    ["Study institution", r.studyInstitution],
    ["Study programme", r.studyProgramme],
    ["Student ID", r.studentId],
  ];
  for (const [category, title] of Object.entries(CATEGORY_TITLES)) {
    const names = r.options
      .filter((o) => o.category === category)
      .map((o) => o.name);
    fields.push([title, names.length > 0 ? names.join("; ") : undefined]);
  }
  fields.push([
    "Consents",
    r.consents.map((c) => `${c.id} (${c.givenAt})`).join("; "),
  ]);
  return fields.filter((f): f is [string, string] => f[1] !== undefined);
}

/** The count, the list or "No registrations match", and the option counts (BR-13). */
export function OrganizerResults({ result }: { result: SearchResult }) {
  return (
    <>
      <p className="matches">Matches: {result.count}</p>
      {result.registrations.length === 0 ? (
        <p>{ORGANIZER_MESSAGES.noMatch}</p>
      ) : (
        <ol aria-label="Registrations" className="registrations">
          {result.registrations.map((r) => (
            <li key={r.id}>
              <dl>
                {rows(r).map(([label, value]) => (
                  <Fragment key={label}>
                    <dt>{label}</dt>
                    <dd>{value}</dd>
                  </Fragment>
                ))}
              </dl>
            </li>
          ))}
        </ol>
      )}
      <section aria-label="Option counts" className="counts">
        {result.optionCounts.map((c) => (
          <div key={c.category}>
            <h3>{CATEGORY_TITLES[c.category]}</h3>
            <ul>
              {c.options.map((o) => (
                <li key={o.id}>
                  {o.name}: {o.count}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </section>
    </>
  );
}
