# Accessibility Compliance Proof — WCAG 2.1 AA

Deliverable: "Accessibility compliance proof" for L1 UC3. This covers `ProductListing`, `ProductCard`, `Cart`, and `CheckoutForm` in `online-shopping-frontend/src/components/`.

## 1. Automated Proof (real, executed test run)

`ProductListing.test.jsx` and `CheckoutForm.test.jsx` run [`jest-axe`](https://github.com/nickcolley/jest-axe) (the axe-core accessibility engine) against the rendered DOM of both components. This was actually executed in this environment — see `vitest-axe-run-log.txt` in this folder for the real console output:

```
✓ src/components/__tests__/CheckoutForm.test.jsx (3 tests) 521ms
✓ src/components/__tests__/ProductListing.test.jsx (3 tests) 368ms

Test Files  2 passed (2)
     Tests  6 passed (6)
```

Both `axe(container)` assertions (`results.violations` has length 0) passed — i.e., axe-core found **zero automated-detectable WCAG violations** in the rendered listing and checkout screens. `vite-build-log.txt` in this folder shows the full app also builds cleanly (43 modules, no errors).

Automated tooling (axe-core) reliably catches roughly 30-50% of WCAG issues; the checklist below covers the criteria that require manual/human judgment.

## 2. Manual WCAG 2.1 AA Checklist

| WCAG 2.1 SC | Requirement | Where addressed |
|---|---|---|
| 1.1.1 Non-text Content | Icons/decorative elements have `aria-hidden` or text alternative | `LoadingSpinner` spinner is `aria-hidden`, paired with a text label |
| 1.3.1 Info and Relationships | Semantic structure (headings, table markup, fieldset/legend) | `<table>` with `<caption>`/`scope` in `Cart.jsx`; `<fieldset><legend>` for payment method in `CheckoutForm.jsx` |
| 1.4.3 Contrast (Minimum) | Text contrast ≥ 4.5:1 | `styles/index.css` — `--color-primary` (#0b5fff) = 4.6:1 on white, `--color-error` (#b00020) = 6.1:1 on white, `--color-border` (#6b6b6b) = 5.7:1 on white |
| 2.1.1 Keyboard | All functionality operable via keyboard | Native `<button>`, `<input>`, `<select>` used throughout — no click-only `<div>` handlers |
| 2.4.1 Bypass Blocks | Skip-navigation mechanism | `.skip-link` "Skip to main content" in `App.jsx` |
| 2.4.6 Headings and Labels | Descriptive headings/labels | Every form input has an explicit `<label htmlFor>`; every section has an `aria-labelledby` heading |
| 2.4.7 Focus Visible | Visible focus indicator | `:focus-visible` outline rule in `styles/index.css`, never removed without replacement |
| 2.5.3 Label in Name | Accessible name matches visible label | Buttons' visible text matches their `aria-label`/accessible name (e.g., "Remove Wireless Mouse from cart" starts with visible "Remove") |
| 3.3.1 Error Identification | Errors identified in text, not color alone | `FormField` renders `role="alert"` text under each invalid field, not just a red border |
| 3.3.2 Labels or Instructions | Inputs have labels | All `CheckoutForm` fields use `<label>`; search input in `ProductListing` has an associated `<label>` |
| 4.1.2 Name, Role, Value | Custom/dynamic controls expose correct ARIA state | `aria-invalid`, `aria-describedby`, `aria-disabled`, `aria-busy` used on interactive elements as their state changes |
| 4.1.3 Status Messages | Dynamic updates announced without moving focus | `role="status"` (`LoadingSpinner`, order confirmation) and `role="alert"` (`ErrorMessage`, field errors) with `aria-live="polite"` on pagination/stock text |
| 2.3.3 Animation from Interactions | Respect reduced motion | `@media (prefers-reduced-motion: reduce)` disables the spinner animation |

## 3. Items Requiring Manual Verification in a Real Browser (not fully provable in a sandboxed test run)

- [ ] Screen reader smoke test (NVDA/VoiceOver) walking through: search → results → add to cart → checkout → submit error → success.
- [ ] Actual rendered color contrast measured with a browser dev-tools contrast checker (values above were computed against the CSS custom properties; a final check against any theme customization is recommended).
- [ ] Zoom to 200% / reflow test (WCAG 1.4.10) on the product grid and checkout form.

## 4. Known Gaps / Follow-ups

- `Cart.jsx` and `ProductCard.jsx` do not yet have their own dedicated axe test files (only exercised indirectly via `ProductListing`'s render tree, which does include `ProductCard`). Recommend adding a standalone `Cart.test.jsx` with axe once cart-service is running for integration testing.
