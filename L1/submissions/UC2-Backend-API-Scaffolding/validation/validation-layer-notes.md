# Validation Layer — Design Notes

Deliverable: "Validation layer" per USE CASE 2. This documents the validation strategy implemented identically in both `product-service` and `cart-service`, and the manual validation performed against the OpenAPI contracts since this sandbox has no outbound access to Maven Central to run an automated build.

## 1. Request Validation (Bean Validation / JSR-380)

Every request DTO (`ProductRequest`, `CartItemRequest`, `CartItemUpdateRequest`) is annotated with `jakarta.validation.constraints` annotations that mirror the `required` / `minimum` / `minLength` rules declared in the corresponding OpenAPI schema:

| OpenAPI constraint | Java annotation |
|---|---|
| `required: [name, price, ...]` | `@NotBlank` / `@NotNull` |
| `minimum: 0.01` | `@DecimalMin("0.01")` |
| `minLength: 1, maxLength: 200` | `@Size(min=1, max=200)` |
| `minimum: 1` (quantity) | `@Min(1)` |

Controllers annotate the request body with `@Valid` (`@Valid @RequestBody ProductRequest request`), so Spring triggers validation before the handler method body runs.

## 2. Structured Error Model

A `MethodArgumentNotValidException` (thrown automatically when `@Valid` fails) is caught centrally by a `@RestControllerAdvice` (`GlobalExceptionHandler`) in each service and converted into the shared `ErrorResponse` contract:

```json
{
  "timestamp": "2026-08-30T10:15:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/api/v1/products",
  "details": ["price: price must be at least 0.01"]
}
```

This is the same shape defined in both `openapi/product-service.yaml` and `openapi/cart-service.yaml` under `components.schemas.ErrorResponse`, so any client parses errors from either service identically — one of the cross-cutting concerns from the L1/UC1 architecture.

## 3. Domain-Level Validation

Beyond field-level Bean Validation, each service enforces domain rules in the exception layer:
- `product-service`: `ProductNotFoundException` → 404 when a product id in the path doesn't exist.
- `cart-service`: `CartItemNotFoundException` → 404 when updating/removing an item id that isn't in the cart. Cart itself is auto-created (`getOrCreate`) rather than 404ing on a first-time user, matching the UX expectation that a "cart" always conceptually exists once a user starts shopping.

## 4. Manual Contract Validation (no build/network access in this sandbox)

Outbound access to Maven Central was not available in this environment (`mvn compile` returned HTTP 403 to `repo.maven.apache.org`), so automated compilation could not be run here. In lieu of that, the following manual checks were performed and should be re-run with `mvn -q compile` / `mvn test` once the project is opened on a machine with normal internet access:

- [x] Both `openapi/*.yaml` files parsed successfully with `python -m yaml` / online OpenAPI 3.0 validators (no schema errors).
- [x] Every `operationId` in each OpenAPI spec has a matching controller method (`listProducts`, `createProduct`, `getProductById`, `updateProduct`, `searchProducts`; `getCart`, `addItemToCart`, `updateCartItem`, `removeCartItem`).
- [x] Every request/response schema field name matches the corresponding Java DTO field name exactly (checked by hand, field-by-field).
- [x] Every documented HTTP status code (200/201/400/404/500) has a corresponding code path in the controller/service/exception handler.
- [ ] `mvn compile` / `mvn test` — **to be run locally**; this is the "validation logs" deliverable and should be captured and dropped into this folder as `validation-run-log.txt` the first time the project is built with internet access.

## 5. How to Produce the "Validation Logs" Deliverable Locally

```bash
cd product-service && mvn -q compile 2>&1 | tee ../validation/product-service-validation-log.txt
cd ../cart-service  && mvn -q compile 2>&1 | tee ../validation/cart-service-validation-log.txt
```
