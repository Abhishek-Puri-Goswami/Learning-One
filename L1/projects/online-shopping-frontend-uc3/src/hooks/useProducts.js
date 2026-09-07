import { useEffect, useRef, useState } from "react";
import { ProductApi } from "../api/apiClient";

/**
 * Fetches the product listing (or search results) and exposes
 * { data, loading, error }.
 *
 * UI risk mitigations implemented here (see ui-risk/ui-risk-report.md):
 * 1. Race condition: if `query`/`page` changes before the previous fetch
 *    resolves, an AbortController cancels the stale request AND a
 *    `requestId` ref guards against a late response overwriting newer state.
 * 2. No direct state mutation: state is always replaced via setState with a
 *    new object/array, never mutated in place.
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
        // Guard against out-of-order responses (race condition mitigation).
        if (latestRequestId.current !== requestId) return;
        setState({ data: result, loading: false, error: null });
      })
      .catch((err) => {
        if (err.name === "AbortError") return; // expected on cleanup/unmount
        if (latestRequestId.current !== requestId) return;
        setState({ data: null, loading: false, error: err });
      });

    return () => controller.abort();
  }, [query, page, size]);

  return state;
}
