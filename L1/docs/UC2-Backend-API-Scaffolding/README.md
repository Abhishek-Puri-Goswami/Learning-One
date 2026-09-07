# L1 — USE CASE 2: AI for Backend API Scaffolding (Contract-First)

Product Service and Cart Service, designed OpenAPI-first, then scaffolded as Spring Boot 3 (Java 17) applications on top of `../ecommerce-core` -- a shared, plain-JDK domain module this rework introduced (see "What changed in this rework" below).

## Deliverables Checklist (per the use case brief)

- [x] **OpenAPI file** → [`product-service/openapi/product-service.yaml`](product-service/openapi/product-service.yaml), [`cart-service/openapi/cart-service.yaml`](cart-service/openapi/cart-service.yaml)
- [x] **Backend scaffold** → full Spring Boot Maven projects in [`product-service/`](product-service/) and [`cart-service/`](cart-service/), wrapping the real, compiled-and-tested [`../ecommerce-core`](../ecommerce-core)
- [x] **Validation layer** → Bean Validation on all request DTOs + shared structured `ErrorResponse` via `@RestControllerAdvice`; see [`validation/validation-layer-notes.md`](validation/validation-layer-notes.md)
- [x] **Retry vs. re-prompt demonstration** → [`prompt-engineering/prompt-refinement.md`](prompt-engineering/prompt-refinement.md)
- [x] **Max token limit experiment** → same file, section 2
- [x] **Prompt refinement documentation** → same file

## What's real vs. documented-but-unverified

Same disclosure standard used throughout this submission (L2/L3): say plainly what was actually compiled and run versus what's written but blocked by this sandbox's lack of Maven Central access (confirmed with a `403 Forbidden, X-Proxy-Error: blocked-by-allowlist` against `repo.maven.apache.org`, in both the build sandbox and on the machine this repo is delivered to).

| Layer | Status |
|---|---|
| `../ecommerce-core` (Product/Cart/CartItem domain model, stock reservation, cart merge/update/remove logic) | **Real** -- compiles and runs with plain `javac`/`java`, 12/12 self-tests passing; see `../ecommerce-core/reports/` |
| `product-service`, `cart-service` (Spring Boot controllers, DTOs, `@RestControllerAdvice`, `RestTemplate` client) | Documented, not compile-verified -- needs Maven Central for `spring-boot-starter-web`/`-validation`/etc. |
| `ProductRepository`, `CartRepository` (this rework) | Documented, not compile-verified (still Spring `@Repository` beans) -- but now thin adapters over the real `ecommerce-core` catalog/model, not a second, never-run reimplementation of the same rules |
| OpenAPI specs, validation notes, prompt-engineering log | Documents, as originally delivered -- unaffected by this rework |

## What changed in this rework

The original L1/UC2 submission had real Java source for both services, but nothing in it was ever machine-compiled or run -- `ProductRepository` and `CartRepository` were each a `ConcurrentHashMap` with their own hand-written merge/stock/search logic, asserted correct but never tested. This rework does not add new untested Spring code to fix that; instead it moves the actual business rules into `../ecommerce-core` -- a module with **zero external dependencies**, so it compiles and runs with nothing but the JDK already on this machine -- and makes both services' repository/model layers thin, honest adapters over it:

- `product-service`'s `ProductRepository` now stores into and reads from `ecommerce-core`'s `ProductCatalog` (translating to/from the REST-facing `Product` entity) instead of maintaining its own untested map.
- `cart-service` deleted its own local `Cart`/`CartItem` model classes entirely and uses `ecommerce-core.model.Cart`/`CartItem` directly -- one real Cart model, not two.
- `CartServiceImpl`'s add/update/remove logic now calls `Cart.addOrMergeItem`, `Cart.setItemQuantity`, and `Cart.removeItemById` -- ecommerce-core methods proven correct by real, run tests (including two added specifically for this use case's itemId-addressed update/remove contract) -- instead of re-implementing merge-by-productId and remove-by-itemId logic locally and unverified.
- `CartServiceImpl`'s `ProductCatalogClient` (an HTTP call to product-service) was left untouched: that's a genuine microservice boundary (two separate deployables), not duplicated business logic, so it correctly stays documented-but-unverified rather than being folded into the shared core.

Net effect: every stock/merge/rollback rule this use case's REST layer depends on is now proven by a real, hand-rolled test suite, not just asserted in a README. The remaining unverified surface is exactly the Spring wiring itself (`@RestController`, `@Repository`, `RestTemplate`, Bean Validation) -- which needs Maven Central, the one thing this sandbox and the delivery machine both block.

## Folder Structure

```
UC2-Backend-API-Scaffolding/
├── README.md
├── product-service/                    Spring Boot 3 / Java 17, port 8081
│   ├── pom.xml                         (depends on ../../ecommerce-core)
│   ├── openapi/product-service.yaml    Contract-first OpenAPI 3.0 spec
│   └── src/main/java/com/retailco/productservice/
│       ├── ProductServiceApplication.java
│       ├── controller/ProductController.java
│       ├── service/ProductService.java, ProductServiceImpl.java
│       ├── repository/ProductRepository.java     (adapter over ecommerce-core's ProductCatalog; PostgreSQL is still the production target per ADR-002)
│       ├── model/Product.java                     (REST-facing entity; translated to/from ecommerce-core's Product)
│       ├── dto/ProductRequest.java, ProductResponse.java, ProductPageResponse.java, ErrorResponse.java
│       └── exception/ProductNotFoundException.java, GlobalExceptionHandler.java
├── cart-service/                       Spring Boot 3 / Java 17, port 8082
│   ├── pom.xml                         (depends on ../../ecommerce-core)
│   ├── openapi/cart-service.yaml
│   └── src/main/java/com/retailco/cartservice/
│       ├── CartServiceApplication.java
│       ├── controller/CartController.java
│       ├── service/CartService.java, CartServiceImpl.java  (delegates merge/update/remove to ecommerce-core's Cart)
│       ├── client/ProductCatalogClient.java       (revalidates price/name against product-service -- a real HTTP boundary, kept as-is)
│       ├── repository/CartRepository.java         (stores ecommerce-core's Cart directly; Redis is still the production target per ADR-002)
│       ├── dto/CartItemRequest.java, CartItemUpdateRequest.java, CartResponse.java, CartItemResponse.java, ErrorResponse.java
│       └── exception/CartNotFoundException.java, CartItemNotFoundException.java, GlobalExceptionHandler.java
├── validation/
│   └── validation-layer-notes.md
└── prompt-engineering/
    └── prompt-refinement.md
```

(`cart-service/src/main/java/com/retailco/cartservice/model/` no longer exists -- its `Cart.java`/`CartItem.java` were deleted in this rework in favor of the shared, tested `../ecommerce-core` model.)

## How to Run Locally

Each service is a standalone Spring Boot app (requires internet access to Maven Central to pull dependencies — this sandbox's and this delivery machine's egress are both restricted to it, so the Spring layer was written and hand-reviewed but not machine-compiled here; see "What's real vs. documented-but-unverified" above):

```bash
cd ../ecommerce-core && mvn install   # publishes ecommerce-core to the local repo so the services above can depend on it
cd product-service    && mvn spring-boot:run   # starts on :8081
cd cart-service        && mvn spring-boot:run   # starts on :8082
```

`ecommerce-core` itself needs no Maven Central dependency to compile -- it can also be verified directly with plain `javac`/`java`, see `../ecommerce-core/README.md`.

Example flow once both are running:
```bash
curl -X POST http://localhost:8081/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Wireless Mouse","price":19.99,"category":"Electronics","stockQuantity":100}'

curl -X POST http://localhost:8082/api/v1/cart/user-123/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"<id-from-above>","quantity":2}'
```

## Known Limitation — No Spring-Layer Build Verification in This Sandbox

`mvn compile` could not be run end-to-end on `product-service`/`cart-service` because outbound access to `repo.maven.apache.org` returns `403 Forbidden, X-Proxy-Error: blocked-by-allowlist` in this cloud sandbox, and the same was independently confirmed on the delivery machine. The Spring-facing code was therefore validated by hand: every OpenAPI `operationId` matches a controller method, every DTO field matches its OpenAPI schema field, and every declared HTTP status code has a corresponding response path. **Please run `mvn -q compile` on both modules the first time you open this on a machine with normal internet access**, and drop the output into `validation/` as described in `validation/validation-layer-notes.md` — that log is itself one of the required deliverables ("validation logs").

This limitation does **not** apply to `../ecommerce-core`, which this rework introduced specifically to remove the business logic these two services depend on from that unverified surface -- it is compiled and run for real, with the evidence in `../ecommerce-core/reports/`.
