import { useRef, useState, type FormEvent } from "react";
import { searchRegistrations, type SearchOutcome } from "./api";
import { ORGANIZER_MESSAGES } from "./messages";
import { OrganizerResults } from "./OrganizerResults";
import type {
  CategoryCounts,
  Credentials,
  OptionCategory,
  SearchQuery,
  SearchResult,
} from "./types";

type Filter = Exclude<keyof SearchQuery, "search">;

const NO_FILTERS: Record<Filter, string> = {
  type: "",
  workshop: "",
  event: "",
  meal: "",
  other: "",
};

const CATEGORY_FILTERS: [OptionCategory, Exclude<Filter, "type">, string][] = [
  ["WORKSHOP", "workshop", "Workshop"],
  ["EVENT", "event", "Event"],
  ["MEAL", "meal", "Meal"],
  ["OTHER", "other", "Other activity"],
];

const MAX_SEARCH_LENGTH = 100;

function tooLong(text: string): boolean {
  return [...text.trim()].length > MAX_SEARCH_LENGTH;
}

/**
 * The organizer page of organizer-search-ui.md (US-009). The credentials live only in this
 * component's state, never in browser storage or cookies (SR-09).
 */
export function OrganizerPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [credentials, setCredentials] = useState<Credentials | null>(null);
  const [loggingIn, setLoggingIn] = useState(false);
  const [wrongCredentials, setWrongCredentials] = useState(false);
  const [failed, setFailed] = useState(false);
  const [text, setText] = useState("");
  const [filters, setFilters] = useState(NO_FILTERS);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [result, setResult] = useState<SearchResult | null>(null);
  const [categories, setCategories] = useState<CategoryCounts[]>([]);
  const latest = useRef(0);

  const apply = (outcome: SearchOutcome) => {
    setFailed(false);
    switch (outcome.kind) {
      case "ok":
        setResult(outcome.result);
        setCategories(outcome.result.optionCounts);
        return;
      case "unauthorized":
        setCredentials(null);
        setResult(null);
        setWrongCredentials(true);
        return;
      case "rejected":
        setResult(null);
        if (outcome.fieldErrors.some((e) => e.field === "search")) {
          setSearchError(ORGANIZER_MESSAGES.searchTooLong);
        } else {
          setFailed(true);
        }
        return;
      default:
        setFailed(true);
    }
  };

  const run = async (
    login: Credentials,
    search: string,
    next: Record<Filter, string>,
  ) => {
    const request = ++latest.current;
    if (tooLong(search)) {
      setSearchError(ORGANIZER_MESSAGES.searchTooLong);
      setResult(null);
      return;
    }
    setSearchError(null);
    const outcome = await searchRegistrations(login, { search, ...next });
    if (request === latest.current) {
      apply(outcome);
    }
  };

  const onLogIn = async (event: FormEvent) => {
    event.preventDefault();
    const login = { username, password };
    setLoggingIn(true);
    setWrongCredentials(false);
    setFailed(false);
    const request = ++latest.current;
    const outcome = await searchRegistrations(login, {
      search: "",
      ...NO_FILTERS,
    });
    setLoggingIn(false);
    if (request !== latest.current) {
      return;
    }
    if (outcome.kind === "ok") {
      setCredentials(login);
      setText("");
      setFilters(NO_FILTERS);
      setSearchError(null);
    }
    apply(outcome);
  };

  const onSearch = (event: FormEvent) => {
    event.preventDefault();
    if (credentials) {
      void run(credentials, text, filters);
    }
  };

  const onFilter = (filter: Filter, value: string) => {
    const next = { ...filters, [filter]: value };
    setFilters(next);
    if (credentials) {
      void run(credentials, text, next);
    }
  };

  const optionsOf = (category: OptionCategory) =>
    categories.find((c) => c.category === category)?.options ?? [];

  return (
    <main className="organizer">
      <h1>{ORGANIZER_MESSAGES.title}</h1>
      {wrongCredentials && (
        <p role="alert" className="alert">
          {ORGANIZER_MESSAGES.wrongCredentials}
        </p>
      )}
      {failed && (
        <p role="alert" className="alert">
          {ORGANIZER_MESSAGES.loadFailed}
        </p>
      )}
      {credentials === null ? (
        <form aria-label="Organizer login" onSubmit={onLogIn}>
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
          <button type="submit" disabled={loggingIn}>
            Log in
          </button>
        </form>
      ) : (
        <>
          <form
            role="search"
            title="Search registrations"
            onSubmit={onSearch}
            noValidate
          >
            <div className="field">
              <label htmlFor="organizer-search">Search</label>
              <input
                id="organizer-search"
                type="search"
                value={text}
                aria-invalid={searchError ? "true" : undefined}
                aria-describedby={
                  searchError ? "organizer-search-error" : undefined
                }
                onChange={(e) => setText(e.target.value)}
              />
              {searchError && (
                <p id="organizer-search-error" className="error">
                  {searchError}
                </p>
              )}
            </div>
            <button type="submit">Search</button>
          </form>
          <div className="filters">
            <div className="field">
              <label htmlFor="filter-type">Registration type</label>
              <select
                id="filter-type"
                value={filters.type}
                onChange={(e) => onFilter("type", e.target.value)}
              >
                <option value="">All</option>
                <option value="EXTERNAL">External participant</option>
                <option value="STUDENT">Student</option>
              </select>
            </div>
            {CATEGORY_FILTERS.map(([category, filter, label]) => (
              <div className="field" key={filter}>
                <label htmlFor={`filter-${filter}`}>{label}</label>
                <select
                  id={`filter-${filter}`}
                  value={filters[filter]}
                  onChange={(e) => onFilter(filter, e.target.value)}
                >
                  <option value="">Any</option>
                  {optionsOf(category).map((o) => (
                    <option key={o.id} value={o.id}>
                      {o.name}
                    </option>
                  ))}
                </select>
              </div>
            ))}
          </div>
          {result && !searchError && <OrganizerResults result={result} />}
        </>
      )}
    </main>
  );
}
