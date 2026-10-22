// The organizer search page (US-009). Credentials stay in memory only; every value from the
// backend is rendered as text, never as HTML (SR-09).
import { useEffect, useState, type FormEvent } from "react";
import { getFormConfig, searchRegistrations } from "./api";
import type { ConferenceOption, Credentials, SearchFilters, SearchResult } from "./types";

export const PAGE_SIZE = 20;
const INVALID_LOGIN = "Invalid username or password.";
const LOAD_FAILURE = "Registrations could not be loaded.";
const FIRST_PAGE: SearchFilters = { text: "", type: "", option: "", page: 1 };

function formatTime(iso: string): string {
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleString();
}

export function OrganizerPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [credentials, setCredentials] = useState<Credentials | null>(null);
  const [loginError, setLoginError] = useState<string | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [options, setOptions] = useState<ConferenceOption[]>([]);
  const [text, setText] = useState("");
  const [filters, setFilters] = useState<SearchFilters>(FIRST_PAGE);
  const [result, setResult] = useState<SearchResult | null>(null);

  useEffect(() => {
    getFormConfig()
      .then((config) => setOptions(config.options))
      .catch(() => setOptions([]));
  }, []);

  async function load(account: Credentials, next: SearchFilters) {
    const outcome = await searchRegistrations(account, next);
    if (outcome.kind === "unauthorized") {
      setCredentials(null);
      setResult(null);
      setLoginError(INVALID_LOGIN);
      return;
    }
    if (outcome.kind !== "ok") {
      setLoadError(LOAD_FAILURE);
      return;
    }
    setLoginError(null);
    setLoadError(null);
    setCredentials(account);
    setFilters(next);
    setResult(outcome.result);
  }

  function onSignIn(event: FormEvent) {
    event.preventDefault();
    void load({ username, password }, FIRST_PAGE);
  }

  function apply(changes: Partial<SearchFilters>) {
    if (credentials) void load(credentials, { ...filters, page: 1, ...changes });
  }

  function onSearch(event: FormEvent) {
    event.preventDefault();
    apply({ text });
  }

  function signOut() {
    setCredentials(null);
    setResult(null);
    setPassword("");
    setText("");
    setFilters(FIRST_PAGE);
  }

  const pages = result ? Math.max(1, Math.ceil(result.total / PAGE_SIZE)) : 1;

  return (
    <main>
      <h1>Organizer registrations</h1>
      {!credentials && (
        <form onSubmit={onSignIn}>
          <div className="field">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
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
          {loginError && <p role="alert">{loginError}</p>}
          <button type="submit">Sign in</button>
        </form>
      )}
      {loadError && <p role="alert">{loadError}</p>}
      {credentials && result && (
        <>
          <form className="filters" onSubmit={onSearch}>
            <label htmlFor="search">Search</label>
            <input
              id="search"
              type="search"
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
            <button type="submit">Search</button>
            <label htmlFor="type-filter">Type</label>
            <select
              id="type-filter"
              value={filters.type}
              onChange={(e) => apply({ text, type: e.target.value as SearchFilters["type"] })}
            >
              <option value="">All types</option>
              <option value="EXTERNAL">External</option>
              <option value="STUDENT">Student</option>
            </select>
            <label htmlFor="option-filter">Option</label>
            <select
              id="option-filter"
              value={filters.option}
              onChange={(e) => apply({ text, option: e.target.value })}
            >
              <option value="">All options</option>
              {options.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.name}
                </option>
              ))}
            </select>
            <button type="button" onClick={signOut}>
              Sign out
            </button>
          </form>
          <p>{result.total} registrations found</p>
          <table aria-label="Registrations">
            <thead>
              <tr>
                <th>Registered at</th>
                <th>Type</th>
                <th>First name</th>
                <th>Last name</th>
                <th>Email</th>
                <th>Organization / study institution</th>
                <th>Options</th>
              </tr>
            </thead>
            <tbody>
              {result.items.map((item) => (
                <tr key={item.id}>
                  <td>{formatTime(item.registeredAt)}</td>
                  <td>{item.type === "EXTERNAL" ? "External" : "Student"}</td>
                  <td>{item.firstName}</td>
                  <td>{item.lastName}</td>
                  <td>{item.email}</td>
                  <td>{item.organization ?? item.studyInstitution ?? ""}</td>
                  <td>{item.options.join(", ")}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <nav className="paging">
            <button
              type="button"
              disabled={filters.page <= 1}
              onClick={() => void load(credentials, { ...filters, page: filters.page - 1 })}
            >
              Previous
            </button>
            <span>
              Page {filters.page} of {pages}
            </span>
            <button
              type="button"
              disabled={filters.page >= pages}
              onClick={() => void load(credentials, { ...filters, page: filters.page + 1 })}
            >
              Next
            </button>
          </nav>
        </>
      )}
    </main>
  );
}
