import { useEffect, useState, type FormEvent } from "react";
import { fetchFormConfig, searchRegistrations, type SearchOutcome } from "../api";
import type { Credentials, FormOption, SearchItem, SearchQuery, SearchResult } from "../types";

// The organizer search page (US-009, interfaces.md "Organizer search page"). Every value from the
// API is rendered as React text, so markup in stored data is shown literally (SR-09).

export const WRONG_CREDENTIALS = "Wrong username or password.";
export const HTTPS_REQUIRED = "Organizer access requires HTTPS.";
export const LOAD_FAILED = "Registrations could not be loaded. Please try again later.";
export const PAGE_SIZE = 20;

const EMPTY_QUERY: SearchQuery = { text: "", type: "", option: "" };
const TYPE_LABELS = { EXTERNAL: "External", STUDENT: "Student" } as const;

function failureMessage(outcome: Exclude<SearchOutcome, { kind: "found" }>): string[] {
  switch (outcome.kind) {
    case "unauthorized":
      return [WRONG_CREDENTIALS];
    case "forbidden":
      return [HTTPS_REQUIRED];
    case "rejected":
      return outcome.errors.map((e) => e.message);
    default:
      return [LOAD_FAILED];
  }
}

export function OrganizerPage() {
  const [options, setOptions] = useState<FormOption[]>([]);
  const [credentials, setCredentials] = useState<Credentials | null>(null);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [draft, setDraft] = useState<SearchQuery>(EMPTY_QUERY);
  const [query, setQuery] = useState<SearchQuery>(EMPTY_QUERY);
  const [result, setResult] = useState<SearchResult | null>(null);
  const [errors, setErrors] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    fetchFormConfig()
      .then((config) => active && setOptions(config.options))
      .catch(() => undefined); // option names are a convenience; ids are shown instead
    return () => {
      active = false;
    };
  }, []);

  async function load(creds: Credentials, next: SearchQuery, page: number): Promise<void> {
    setBusy(true);
    const outcome = await searchRegistrations(creds, next, page);
    setBusy(false);
    if (outcome.kind === "found") {
      setCredentials(creds);
      setQuery(next);
      setResult(outcome.result);
      setErrors([]);
      return;
    }
    if (outcome.kind === "unauthorized") {
      setCredentials(null);
      setResult(null);
    }
    setErrors(failureMessage(outcome));
  }

  function signIn(event: FormEvent): void {
    event.preventDefault();
    void load({ username, password }, EMPTY_QUERY, 1);
  }

  function search(event: FormEvent): void {
    event.preventDefault();
    if (credentials) void load(credentials, draft, 1);
  }

  function goTo(page: number): void {
    if (credentials) void load(credentials, query, page);
  }

  function signOut(): void {
    setCredentials(null);
    setResult(null);
    setPassword("");
    setDraft(EMPTY_QUERY);
    setErrors([]);
  }

  const alerts = errors.length > 0 && (
    <div role="alert" className="error general">
      {errors.map((message) => (
        <p key={message}>{message}</p>
      ))}
    </div>
  );

  if (!credentials || !result) {
    return (
      <main>
        <h1>Organizer sign-in</h1>
        {alerts}
        <form onSubmit={signIn}>
          <div className="field">
            <label htmlFor="organizer-username">Username</label>
            <input
              id="organizer-username"
              type="text"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="organizer-password">Password</label>
            <input
              id="organizer-password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button type="submit" disabled={busy}>
            Sign in
          </button>
        </form>
      </main>
    );
  }

  const pages = Math.max(1, Math.ceil(result.total / PAGE_SIZE));
  const optionName = (id: string) => options.find((o) => o.id === id)?.name ?? id;

  return (
    <main className="wide">
      <h1>Registrations</h1>
      <form className="search" onSubmit={search}>
        <div className="field">
          <label htmlFor="search-text">Search</label>
          <input
            id="search-text"
            type="text"
            maxLength={100}
            value={draft.text}
            onChange={(e) => setDraft({ ...draft, text: e.target.value })}
          />
        </div>
        <div className="field">
          <label htmlFor="search-type">Type</label>
          <select
            id="search-type"
            value={draft.type}
            onChange={(e) => setDraft({ ...draft, type: e.target.value as SearchQuery["type"] })}
          >
            <option value="">All types</option>
            <option value="EXTERNAL">{TYPE_LABELS.EXTERNAL}</option>
            <option value="STUDENT">{TYPE_LABELS.STUDENT}</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor="search-option">Option</label>
          <select
            id="search-option"
            value={draft.option}
            onChange={(e) => setDraft({ ...draft, option: e.target.value })}
          >
            <option value="">All options</option>
            {options.map((o) => (
              <option key={o.id} value={o.id}>
                {o.name}
              </option>
            ))}
          </select>
        </div>
        <button type="submit" disabled={busy}>
          Search
        </button>
      </form>
      {alerts}
      <p>
        {result.total === 0
          ? "No registrations found."
          : `${result.total} ${result.total === 1 ? "registration" : "registrations"} found`}
      </p>
      {result.items.length > 0 && <ResultTable items={result.items} optionName={optionName} />}
      <nav aria-label="Pages" className="pages">
        <button
          type="button"
          disabled={busy || result.page <= 1}
          onClick={() => goTo(result.page - 1)}
        >
          Previous
        </button>
        <span>
          Page {result.page} of {pages}
        </span>
        <button
          type="button"
          disabled={busy || result.page >= pages}
          onClick={() => goTo(result.page + 1)}
        >
          Next
        </button>
      </nav>
      <button type="button" className="secondary" onClick={signOut}>
        Sign out
      </button>
    </main>
  );
}

function ResultTable({
  items,
  optionName,
}: {
  items: SearchItem[];
  optionName: (id: string) => string;
}) {
  return (
    <table>
      <thead>
        <tr>
          <th scope="col">Name</th>
          <th scope="col">Email</th>
          <th scope="col">Type</th>
          <th scope="col">Organization / study institution</th>
          <th scope="col">Options</th>
          <th scope="col">Registered</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <tr key={item.id}>
            <td>
              {item.firstName} {item.lastName}
            </td>
            <td>{item.email}</td>
            <td>{TYPE_LABELS[item.type] ?? item.type}</td>
            <td>{item.organization ?? item.studyInstitution ?? ""}</td>
            <td>{item.options.map(optionName).join(", ")}</td>
            <td>{item.registeredAt}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
