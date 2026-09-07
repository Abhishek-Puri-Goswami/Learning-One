# Regression Checklist — L1 UC3 Frontend

Deliverable: "Suggest regression test points" / "regression checklist." To be re-run manually (or automated in L1/UC5) whenever `ProductListing`, `Cart`, `CheckoutForm`, `apiClient.js`, or `useCart`/`useProducts` change.

## Automated (already implemented — run with `npm test`)

- [x] `ProductListing` renders loading state, then product data, from a mocked `/api/v1/products` response.
- [x] `ProductListing` renders an accessible error (`role="alert"`) on a 5xx response.
- [x] `ProductListing` passes axe-core with zero violations.
- [x] `CheckoutForm` shows field-scoped, screen-reader-linked validation errors on empty submit.
- [x] `CheckoutForm` disables its submit button while a request is in flight (double-submit guard).
- [x] `CheckoutForm` passes axe-core with zero violations.

## Manual / To Automate Next (recommended additions, tracked for L1/UC5)

- [ ] **Search race condition:** type a query, then immediately clear it before the first request resolves — assert the final rendered list matches the *last* query, not a stale intermediate one. (Regression target: `useProducts.js` requestId guard.)
- [ ] **Add-to-cart double-click:** simulate two rapid clicks on "Add to cart" — assert only one `POST /api/v1/cart/{userId}/items` call fires while `busy` is true, or that the second is safely queued/ignored.
- [ ] **Checkout idempotency key reuse on retry:** simulate a failed order submission (network error), then a successful resubmit — assert the **same** `idempotencyKey` is sent both times.
- [ ] **Checkout idempotency key rotation after success:** after a successful order, start a new checkout — assert a **new** `idempotencyKey` is generated (not the one from the completed order).
- [ ] **Pagination boundary:** on the last page, assert "Next" is disabled; on the first page, assert "Previous" is disabled.
- [ ] **Out-of-stock product:** assert "Add to cart" is disabled and reads "Unavailable" when `stockQuantity <= 0`.
- [ ] **Cart empty state:** assert `Cart.jsx` renders "Your cart is empty." with no console errors when `items` is `[]`.
- [ ] **Keyboard-only walkthrough:** Tab from the skip link through search → product card → cart → checkout form → submit, without a mouse, confirming focus order matches visual order and nothing is skipped.
- [ ] **Component unmount during fetch:** navigate away from `ProductListing` while a request is in flight — assert no "setState on unmounted component" warning (covered structurally by the `AbortController` cleanup in `useEffect`, but should get an explicit test).
- [ ] **Contract drift detection:** add a lightweight test (or a build-time script) that fails if a field referenced in a component (e.g., `product.stockQuantity`) no longer appears in the corresponding OpenAPI schema in L1/UC2 — turns "hallucinated API" risk into a caught regression instead of a runtime surprise.

## Suggested CI Gate (for later use cases, e.g. L2/UC6's CI/CD pattern)

```
npm ci
npm run lint
npm test           # includes the axe accessibility assertions above
npm run build       # fails the pipeline on any TypeScript/JSX/bundling error
```
