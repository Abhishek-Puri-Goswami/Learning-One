# UI Risk Report — L1 UC3

Deliverable: "UI risk report." Documents the risks the AI-generated frontend was explicitly checked against, per the use case brief's "The AI Must" list: add ARIA labels, avoid state mutation bugs, warn about race conditions, avoid hallucinated APIs, suggest regression test points.

## 1. State Mutation Bugs

**Risk:** Directly mutating React state (e.g., `cart.items.push(newItem)` then calling `setCart(cart)`) causes stale renders, broken memoization, and subtle bugs where two renders reference the "same" object even though its contents changed.

**Mitigation implemented:**
- `useCart.js`: every mutation (`addItem`, `updateItemQuantity`, `removeItem`) calls the backend and then calls `setCart(updatedCart)` with the **new object returned by the server** — state is always *replaced*, never mutated in place.
- `useProducts.js`: `setState({ data: result, ... })` always constructs a new state object.
- `CheckoutForm.jsx`: `setForm((prev) => ({ ...prev, [field]: value }))` — spreads the previous state rather than mutating it.

**Verification:** code-reviewed line by line; no `.push`, `.splice`, direct property assignment (`obj.x = y`), or `Array.prototype` mutating methods appear anywhere on a value already held in `useState`/`useRef`.

## 2. Race Conditions

**Risk A — stale fetch response overwrites newer state.** If a user types a search query quickly (or navigates away and back), an earlier `fetch` may resolve *after* a later one, overwriting fresh data with stale data.

**Mitigation:** `useProducts.js` uses a `latestRequestId` ref incremented per request; any response is applied to state only if it's still the latest request. Combined with an `AbortController` cancelling the previous in-flight request on every dependency change (and on unmount).

**Risk B — double-submit / double-order.** A user double-clicking "Place order," or a network retry after a timeout, could create two orders.

**Mitigation:**
- Submit button is `disabled` while `submitting` is true (`aria-busy` reflects this to assistive tech too).
- A client-generated `idempotencyKey` (`crypto.randomUUID()`) is sent with every order-creation request; the same key is reused across retries of the *same* failed attempt (see `useMemo` dependency in `CheckoutForm.jsx`), matching the idempotency-key requirement already specified for `order-management-service` in L1/UC1 (TR-04) and L1/UC2.

**Risk C — cart mutation race.** Rapid double-click on "Add to cart" could fire two `addItem` calls before the first resolves.

**Mitigation:** `useCart`'s `busy` flag is surfaced to `ProductCard`'s "Add to cart" button (`disabled={busy}`), closing the click-to-click window.

## 3. Hallucinated APIs

**Risk:** AI-generated frontend code inventing endpoints, field names, or HTTP methods that don't exist on the backend, which only surfaces as a runtime 404 (or worse, a silent type mismatch) far from where the bug was introduced.

**Mitigation:**
- `api/apiClient.js` is the **single** place any endpoint URL is constructed. Every method maps 1:1 to an `operationId` in `openapi/product-service.yaml` / `openapi/cart-service.yaml` (L1/UC2) or the `api_boundaries` in L1/UC1's `architecture.json` (for `/api/v1/orders`).
- Every field referenced from an API response (`product.stockQuantity`, `cart.subtotal`, `item.lineTotal`, etc.) was cross-checked against the exact schema field names in the OpenAPI `components.schemas` blocks — not guessed.
- No component calls `fetch`/`axios` directly; they all go through `apiClient.js`, so a future contract change only needs to be updated in one file, and a mismatch is easy to grep for.

## 4. Stale Cart Data vs. Catalog (carried over from L1/UC1 architecture risk)

**Risk:** Cart shows a price/stock snapshot from add-to-cart time, which can drift from the live catalog by the time checkout happens.

**Mitigation:** `Cart.jsx` explicitly documents (code comment) that its subtotal is cart-computed, not authoritative, and `CheckoutForm.jsx` never sends client-held prices to the order API — only `userId`, shipping details, and payment method. Price/stock revalidation is the server's (order-management-service's) responsibility, consistent with ADR-001/ADR-003 from L1/UC1.

## 5. Accessibility Regressions (cross-reference)

Handled in depth in `accessibility/accessibility-audit.md`; flagged here because an accessibility regression is itself a UI risk category the brief calls out ("Add ARIA labels"). Automated axe-core checks are wired into the test suite specifically so a future change that removes a label or breaks a landmark fails CI, not just a manual review.

## 6. Risk Summary Table

| Risk | Severity if unmitigated | Status |
|---|---|---|
| State mutation bugs | High (silent stale UI) | Mitigated — code review confirmed |
| Race condition: stale fetch overwrite | Medium | Mitigated — requestId guard + AbortController |
| Race condition: double order submit | High (financial/duplicate order) | Mitigated — idempotency key + disabled submit |
| Race condition: double add-to-cart | Low-Medium | Mitigated — busy flag |
| Hallucinated API endpoints/fields | High (breaks at runtime, hard to trace) | Mitigated — single API client, schema cross-checked |
| Accessibility regression | Medium-High (compliance + usability) | Mitigated — automated axe tests in CI-ready test suite |
