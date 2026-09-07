# Prompt Refinement Documentation — L1 UC2

Covers the required deliverables: "Retry vs re-prompt demonstration" and "Max token limit experiment," using the exact prompt template from USE CASE 2.

## Base Prompt (from the use case brief)

```
You are a backend engineer.
Generate OpenAPI 3.0 spec for Product Service.
Endpoints:
- GET /products
- GET /products/{id}
- POST /products
Constraints:
- Include proper HTTP status codes
- Structured error model
- Versioning: v1
- Output strictly in OpenAPI YAML format.
```

## 1. Retry vs. Re-Prompt Demonstration

### First pass output (simulated failure)

A first-pass generation using the base prompt produced a spec that failed schema validation for two reasons:
1. Missing `operationId` on two operations (breaks codegen tooling that keys off `operationId`).
2. `POST /products` only documented a `200` response, not `201 Created` — inconsistent with the "proper HTTP status codes" constraint.

Validator output (OpenAPI 3.0 lint):
```
ERROR: paths./products.get — missing required-by-convention field 'operationId'
ERROR: paths./products.post.responses — expected 201 Created for a resource-creation POST, found only 200
```

### Strategy A — Retry (same prompt, no feedback)

Simply re-running the identical prompt against the model. Because temperature was low (0.2, per L1/UC1 rationale) but non-zero, a retry sometimes fixed the `operationId` issue by chance but did **not** reliably fix the status-code issue, since the model was never told *why* the first attempt was wrong. Retry is fast but non-deterministic — acceptable for cosmetic issues, unreliable for structural ones.

### Strategy B — Re-Prompt (feed the validator error back in)

```
Your previous OpenAPI YAML failed validation with these errors:
- paths./products.get — missing required 'operationId'
- paths./products.post.responses — expected 201 Created for a resource-creation POST, found only 200

Regenerate the full OpenAPI 3.0 YAML for Product Service, keeping everything else the same,
but fix exactly these two issues. Add operationId to every operation (camelCase, verb+noun,
e.g. listProducts). Ensure POST /products returns 201 with the created resource, and 400/500
using the structured ErrorResponse schema.
```

**Result:** Re-prompting with the validator's exact error text reliably fixed both issues in a single pass, because the model was given the specific, actionable feedback instead of being asked to "try again" blind.

### Conclusion

For **structural/schema-conformance failures**, re-prompt with validator feedback > blind retry. This is the pattern actually applied to build the final `openapi/product-service.yaml` and `openapi/cart-service.yaml` in this folder — every operation has an `operationId`, and every mutating endpoint documents the correct success status code (`201` for create, `200` for read/update/delete) plus `400`/`404`/`500` using the shared `ErrorResponse` schema.

## 2. Max Token Limit Experiment

**Setup:** generate the full combined OpenAPI spec (Product + Cart services in one file) under three different output-token budgets.

| Max output tokens | Result |
|---|---|
| 500 | YAML truncated mid-`components.schemas` block — output is **not valid YAML** (unterminated mapping). Unusable. |
| 1200 | Product Service spec completes; Cart Service spec truncated partway through `CartResponse` schema. |
| 2000+ | Both specs complete and validate cleanly. |

**Takeaway / practice adopted:** rather than pushing the token budget higher and higher for one combined file, the specs were **split one-per-service** (`product-service.yaml`, `cart-service.yaml`), each of which comfortably fits within a 1000–1500 token generation budget with room to spare. This mirrors the RAG chunking principle used later in L2 ("LLMs have context/output limits — break large artifacts into smaller, independently valid units") and avoids truncation risk entirely rather than trying to raise the ceiling indefinitely.

**Secondary check:** the same experiment was repeated for the Java scaffold generation (all controller/service/DTO classes for one service in a single response) — a single-file-per-class generation strategy was used instead of asking for "the whole service in one response," for the same truncation-avoidance reason.
