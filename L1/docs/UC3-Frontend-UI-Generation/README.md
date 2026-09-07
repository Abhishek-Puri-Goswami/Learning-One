# L1 — USE CASE 3: AI for Frontend Generation & UI Risk Control

React (Vite) frontend for the Online Shopping System: Product Listing, Cart, and Checkout — wired to the exact API contracts from L1/UC2.

## Deliverables Checklist (per the use case brief)

- [x] **React component(s)** → [`online-shopping-frontend/src/components/`](online-shopping-frontend/src/components/) — `ProductListing.jsx`, `ProductCard.jsx`, `Cart.jsx`, `CheckoutForm.jsx`
- [x] **Accessibility compliance proof** → [`accessibility/accessibility-audit.md`](accessibility/accessibility-audit.md) + real executed test logs (`vitest-axe-run-log.txt`, `vite-build-log.txt`)
- [x] **State management improvement notes** → covered in `ui-risk/ui-risk-report.md` §1 and inline code comments in `useCart.js` / `useProducts.js`
- [x] **UI risk report** → [`ui-risk/ui-risk-report.md`](ui-risk/ui-risk-report.md)
- [x] **Regression checklist** → [`regression/regression-checklist.md`](regression/regression-checklist.md)
- [x] **Prompt library entry** → [`prompt-engineering/prompt-library-entry.md`](prompt-engineering/prompt-library-entry.md)

## What's Different From UC2 — This Was Actually Run

Unlike the Java backend in L1/UC2 (blocked by no Maven Central access in this sandbox), **this frontend was installed, built, and tested for real** in this environment:

```
✓ src/components/__tests__/CheckoutForm.test.jsx (3 tests) 521ms
✓ src/components/__tests__/ProductListing.test.jsx (3 tests) 368ms
Test Files  2 passed (2)
     Tests  6 passed (6)

✓ vite build — 43 modules transformed, built in 1.46s, no errors
```

Full logs are saved in `accessibility/vitest-axe-run-log.txt` and `accessibility/vite-build-log.txt`. `node_modules/` was removed after the run to keep the delivered folder small — run `npm install` before `npm run dev` / `npm test`.

## Folder Structure

```
UC3-Frontend-UI-Generation/
├── README.md
├── online-shopping-frontend/               React 18 + Vite app
│   ├── package.json / vite.config.js / index.html
│   └── src/
│       ├── api/apiClient.js                Single source of truth for endpoint calls (no hallucinated APIs)
│       ├── hooks/useProducts.js, useCart.js Race-condition-safe data hooks
│       ├── components/
│       │   ├── ProductListing.jsx           <- the exact "example prompt" component from the brief
│       │   ├── ProductCard.jsx
│       │   ├── Cart.jsx
│       │   ├── CheckoutForm.jsx
│       │   ├── LoadingSpinner.jsx, ErrorMessage.jsx
│       │   └── __tests__/                   Vitest + Testing Library + jest-axe
│       ├── styles/index.css                 WCAG-checked color contrast, focus rings, reduced-motion
│       ├── App.jsx, main.jsx, setupTests.js
├── accessibility/
│   ├── accessibility-audit.md
│   ├── vitest-axe-run-log.txt              Real test output captured in this sandbox
│   └── vite-build-log.txt
├── ui-risk/
│   └── ui-risk-report.md
├── regression/
│   └── regression-checklist.md
└── prompt-engineering/
    └── prompt-library-entry.md
```

## Run It Yourself

```bash
cd online-shopping-frontend
npm install
npm run dev      # http://localhost:5173, proxies /api to product-service (:8081) and cart-service (:8082) from L1/UC2
npm test         # vitest + jest-axe accessibility assertions
npm run build    # production build
```
