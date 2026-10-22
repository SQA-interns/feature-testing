import { type FormEvent, useEffect, useState } from "react";
import {
  type ConferenceOption,
  type Credentials,
  PAGE_SIZE,
  type SearchQuery,
  type SearchResult,
  fetchFormConfig,
  searchRegistrations,
} from "../api";

export const WRONG_CREDENTIALS = "Wrong username or password.";
export const LOAD_ERROR = "The registrations could not be loaded. Please try again later.";

const TYPE_LABELS: Record<string, string> = {
  EXTERNAL: "External participant",
  STUDENT: "Student",
};

const EMPTY_QUERY: SearchQuery = { text: "", type: "", option: "", page: 1 };

/** The organizer search page (US-009): sign in, then search, filter and page. */
export function OrganizerPage() {
  const [options, setOptions] = useState<ConferenceOption[]>([]);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [credentials, setCredentials] = useState<Credentials | null>(null);
  const [draft, setDraft] = useState<SearchQuery>(EMPTY_QUERY);
  const [query, setQuery] = useState<SearchQuery>(EMPTY_QUERY);
  const [result, setResult] = useState<SearchResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchFormConfig()
      .then((config) => setOptions(config.options))
      .catch(() => setOptions([]));
  }, []);

  async function load(using: Credentials, next: SearchQuery) {
    const outcome = await searchRegistrations(using, next);
    if (outcome.kind === "ok") {
      setCredentials(using);
      setQuery(next);
      setResult(outcome.result);
      setError(null);
    } else if (outcome.kind === "unauthorized") {
      setCredentials(null);
      setResult(null);
      setError(WRONG_CREDENTIALS);
    } else if (outcome.kind === "rejected") {
      setError(outcome.errors.map((e) => e.message).join(" ") || LOAD_ERROR);
    } else {
      setError(LOAD_ERROR);
    }
  }

  function signIn(event: FormEvent) {
    event.preventDefault();
    void load({ username, password }, EMPTY_QUERY);
  }

  function search(event: FormEvent) {
    event.preventDefault();
    if (credentials) void load(credentials, { ...draft, page: 1 });
  }

  function goTo(page: number) {
    if (credentials) void load(credentials, { ...query, page });
  }

  const optionName = (id: string) => options.find((o) => o.id === id)?.name ?? id;

  if (!credentials || !result) {
    return (
      <section aria-labelledby="organizer-heading">
        <h2 id="organizer-heading">Organizer sign-in</h2>
        <form onSubmit={signIn}>
          <div className="field">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button type="submit">Sign in</button>
        </form>
        {error && <p role="alert">{error}</p>}
      </section>
    );
  }

  const pages = Math.max(1, Math.ceil(result.total / PAGE_SIZE));
  return (
    <section aria-labelledby="organizer-heading">
      <h2 id="organizer-heading">Registrations</h2>
      <form role="search" onSubmit={search}>
        <label htmlFor="search-text">Search</label>
        <input
          id="search-text"
          type="search"
          maxLength={100}
          value={draft.text}
          onChange={(e) => setDraft({ ...draft, text: e.target.value })}
        />
        <label htmlFor="search-type">Type</label>
        <select
          id="search-type"
          value={draft.type}
          onChange={(e) => setDraft({ ...draft, type: e.target.value })}
        >
          <option value="">All types</option>
          <option value="EXTERNAL">{TYPE_LABELS.EXTERNAL}</option>
          <option value="STUDENT">{TYPE_LABELS.STUDENT}</option>
        </select>
        <label htmlFor="search-option">Option</label>
        <select
          id="search-option"
          value={draft.option}
          onChange={(e) => setDraft({ ...draft, option: e.target.value })}
        >
          <option value="">All options</option>
          {options.map((option) => (
            <option key={option.id} value={option.id}>
              {option.name}
            </option>
          ))}
        </select>
        <button type="submit">Search</button>
      </form>
      {error && <p role="alert">{error}</p>}

      <p>{result.total} registrations</p>
      {result.items.length === 0 ? (
        <p>No registrations found.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th scope="col">Registered</th>
              <th scope="col">Type</th>
              <th scope="col">First name</th>
              <th scope="col">Last name</th>
              <th scope="col">Email</th>
              <th scope="col">Organization / institution</th>
              <th scope="col">Options</th>
            </tr>
          </thead>
          <tbody>
            {result.items.map((item) => (
              <tr key={item.id}>
                <td>{new Date(item.registeredAt).toLocaleString()}</td>
                <td>{TYPE_LABELS[item.type] ?? item.type}</td>
                <td>{item.firstName}</td>
                <td>{item.lastName}</td>
                <td>{item.email}</td>
                <td>{item.organization ?? item.studyInstitution ?? ""}</td>
                <td>{item.options.map(optionName).join(", ")}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <nav aria-label="Pages">
        <button type="button" disabled={query.page <= 1} onClick={() => goTo(query.page - 1)}>
          Previous
        </button>
        <span>
          Page {query.page} of {pages}
        </span>
        <button type="button" disabled={query.page >= pages} onClick={() => goTo(query.page + 1)}>
          Next
        </button>
      </nav>
    </section>
  );
}
