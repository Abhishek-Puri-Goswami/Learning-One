import { useEffect, useRef, useState } from "react";
import { ProductApi } from "../api/apiClient";

/**
 * A custom React hook that fetches the product listing (or search
 * results, if a query is given) and hands back {@code { data, loading,
 * error }} so a component can show a spinner, an error message, or the
 * actual products.
 * <p>
 * One subtle bug this hook protects against: what if the user types a
 * new search before the OLD search has even finished loading? Without
 * protection, the old (slower) response could arrive AFTER the new one
 * and overwrite it with stale results. This hook avoids that two ways:
 * an {@code AbortController} cancels the previous request outright, and
 * a {@code requestId} counter double-checks that only the MOST RECENT
 * request is allowed to update what's shown on screen.
 */
export function useProducts({ query = "", page = 0, size = 20 } = {}) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const latestRequestId = useRef(0);

  useEffect(() => {
    const requestId = ++latestRequestId.current;
    const controller = new AbortController();

    setState((prev) => ({ ...prev, loading: true, error: null }));

    const fetchPromise = query
      ? ProductApi.search(query, { page, size }, controller.signal)
      : ProductApi.list({ page, size }, controller.signal);

    fetchPromise
      .then((result) => {
        // If a newer request has started since this one began, ignore this
        // (now stale) response instead of letting it overwrite fresher data.
        if (latestRequestId.current !== requestId) return;
        setState({ data: result, loading: false, error: null });
      })
      .catch((err) => {
        if (err.name === "AbortError") return; // this is expected when we cancel the request ourselves, not a real error
        if (latestRequestId.current !== requestId) return;
        setState({ data: null, loading: false, error: err });
      });

    return () => controller.abort();
  }, [query, page, size]);

  return state;
}
