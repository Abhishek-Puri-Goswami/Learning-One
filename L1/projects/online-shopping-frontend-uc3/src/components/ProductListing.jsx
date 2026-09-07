import { useState } from "react";
import { useProducts } from "../hooks/useProducts";
import { useCart } from "../hooks/useCart";
import ProductCard from "./ProductCard";
import LoadingSpinner from "./LoadingSpinner";
import ErrorMessage from "./ErrorMessage";

/**
 * The main "shop" page: a search box plus a paginated grid of products,
 * each with an "Add to cart" button.
 * <p>
 * It shows a spinner while products are loading, an error message (with
 * a retry option) if the request fails, and otherwise the actual list of
 * products with Previous/Next buttons underneath for paging through
 * results.
 */
export default function ProductListing({ userId = "guest" }) {
  const [searchInput, setSearchInput] = useState("");
  const [committedQuery, setCommittedQuery] = useState("");
  const [page, setPage] = useState(0);

  const { data, loading, error } = useProducts({ query: committedQuery, page, size: 12 });
  const { addItem, busy: cartBusy } = useCart(userId);

  // Submitting the search box triggers a new fetch inside useProducts, because
  // that hook watches `committedQuery` for changes. If the user searches
  // multiple times quickly, useProducts already protects against an older,
  // slower search response overwriting a newer one (see hooks/useProducts.js).
  function handleSearchSubmit(event) {
    event.preventDefault();
    setPage(0);
    setCommittedQuery(searchInput.trim());
  }

  async function handleAddToCart(productId) {
    try {
      await addItem(productId, 1);
    } catch {
      // If this fails, the error already shows up through useCart's `error`
      // state over on the Cart page — this empty catch here just stops an
      // unhandled-promise warning if the user has already navigated away.
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
