# Prompt Library Entry — L1 UC3 (Frontend Generation)

Deliverable: "Prompt library entry." Reusable prompt pattern for generating accessible, risk-aware React components, distilled from building `ProductListing`, `Cart`, and `CheckoutForm`.

---

## Entry: `react-accessible-data-component`

**When to use:** generating a React component that fetches/mutates data from a REST API and is shown directly to end users (i.e., anything more than an internal dev tool).

**Template:**
```
You are a senior frontend engineer.
Generate a React component: <COMPONENT_PURPOSE>.

Requirements:
- Accessible (WCAG 2.1 AA): proper labels, ARIA roles/states, visible focus,
  keyboard operability, and role="alert"/role="status" for dynamic error/loading text.
- Handle loading, error, and empty states explicitly (don't assume the happy path).
- Avoid direct state mutation — every setState call must construct a new
  object/array; never mutate a value already held in state or a ref.
- Use ONLY these API endpoint(s), matching this exact contract: <PASTE OPENAPI OPERATION(S)>.
  Do not invent additional endpoints, fields, or HTTP methods.
- Call out any race conditions this component could hit (e.g., rapid re-fetch,
  double-submit) and how you mitigated each one.
- Provide a short explanation of potential UI risks after the code.
```

**Notes on why each line is there (traceable back to real defects this avoided):**
- "Do not invent additional endpoints" — directly targets the "hallucinated APIs" risk in `ui-risk/ui-risk-report.md`. Effective because it forces the prompt author to paste the actual contract rather than describing it from memory.
- "Never mutate a value already held in state" — prevents the classic React bug where `array.push()` + `setState(array)` doesn't trigger a re-render or causes downstream memoization bugs.
- "Call out race conditions" — this single line is what produced the `AbortController` + `requestId` guard pattern in `useProducts.js` and the idempotency-key pattern in `CheckoutForm.jsx`; asking generically for "good code" did not surface these on its own in earlier attempts.

## Entry: `react-form-validation-accessible`

**When to use:** any form the AI generates that collects user input and can fail validation.

**Template:**
```
Generate a React form component with client-side validation.
- Every invalid field must have aria-invalid="true" and aria-describedby
  pointing to a role="alert" element containing the specific error message.
- Do not rely on color alone to indicate an error.
- Disable the submit button while submission is in flight, and prevent a
  second submission from firing (double-submit guard).
- If failure is possible mid-submission (network error), generate and send an
  idempotency key with the mutating request, and explain how it is reused on
  retry vs. rotated on success.
```

## Retrospective: Bad Prompt vs. Good Prompt (this use case)

**Bad (intent only):**
```
Make a React product page and a checkout form.
```
Would produce a plausible-looking but likely inaccessible, hallucinated-endpoint, mutation-bug-prone component with no error handling — exactly the failure modes this use case's brief asks to guard against.

**Good (the templates above):** produced `ProductListing.jsx`, `Cart.jsx`, and `CheckoutForm.jsx` in this folder, each of which passed automated `jest-axe` accessibility checks with zero violations (see `accessibility/vitest-axe-run-log.txt`) on the first fully-specified generation pass.
