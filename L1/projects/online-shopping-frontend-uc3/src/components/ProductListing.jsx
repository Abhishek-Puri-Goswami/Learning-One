import { useState } from "react";
import { useProducts } from "../hooks/useProducts";
import { useCart } from "../hooks/useCart";
import ProductCard from "./ProductCard";
import LoadingSpinner from "./LoadingSpinner";
import ErrorMessage from "./ErrorMessage";

/**
 * Product listing page/component.
 *
 * Fulfils the exact requirements from the USE CASE 3 example prompt:
 *  - Accessible (WCAG 2.1 AA)              -> see accessibility/accessibility-audit.md
 *  - Handles loading and error states      -> useProducts() below
 *  - Avoid direct state mutation           -> all state updates go through
 *                                              setState with new objects (see useProducts/useCart)
 *  - Use API endpoint: /api/v1/products    -> via ProductApi.list() in api/apiClient.js
 *  - Explanation of potential UI risks     -> ui-risk/ui-risk-report.md (full doc);
 *                                              key risks are called out inline below.
 */
export default function ProductListing({ userId = "guest" }) {
  const [searchInput, setSearchInput] = useState("");
  const [committedQuery, setCommittedQuery] = useState("");
  const [page, setPage] = useState(0);

  const { data, loading, error } = useProducts({ query: committedQuery, page, size: 12 });
  const { addItem, busy: cartBusy } = useCart(userId);

  // RISK: submitting the search form triggers a new fetch (via useProducts'
  // effect dependency on `committedQuery`); if the user types quickly and
  // submits multiple times, useProducts' requestId guard (see hooks/useProducts.js)
  // ensures only the latest response is ever applied to state.
  function handleSearchSubmit(event) {
    event.preventDefault();
    setPage(0);
    setCommittedQuery(searchInput.trim());
  }

  async function handleAddToCart(productId) {
    try {
      await addItem(productId, 1);
    } catch {
      // Error surfaced via useCart's `error` state in the Cart component;
      // this page-level catch just prevents an unhandled promise rejection
      // from a stale click after navigating away.
    }
  }

  return (
    <section aria-labelledby="product-listing-heading">
      <h2 id="product-listing-heading">Shop Products</h2>

      <form role="search" onSubmit={handleSearchSubmit}>
        <label htmlFor="product-search">Search products</label>
        <input
          id="product-search"
          name="q"
          type="search"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          autoComplete="off"
        />
        <button type="submit">Search</button>
      </form>

      {loading && <LoadingSpinner label="Loading products…" />}

      {!loading && error && (
        <ErrorMessage error={error} onRetry={() => setCommittedQuery((q) => q)} />
      )}

      {!loading && !error && data && (
        <>
          <ul className="product-grid" aria-label="Product results">
            {data.content.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                busy={cartBusy}
                onAddToCart={handleAddToCart}
              />
            ))}
          </ul>

          {data.content.length === 0 && <p>No products found.</p>}

          <nav aria-label="Product pagination">
            <button
              type="button"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
            >
              Previous
            </button>
            <span aria-live="polite">
              Page {data.page + 1} of {Math.max(1, data.totalPages)}
            </span>
            <button
              type="button"
              onClick={() => setPage((p) => p + 1)}
              disabled={page + 1 >= data.totalPages}
            >
              Next
            </button>
          </nav>
        </>
      )}
    </section>
  );
}
