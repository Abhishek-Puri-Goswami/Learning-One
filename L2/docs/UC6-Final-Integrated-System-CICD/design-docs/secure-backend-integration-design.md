# Secure Backend Integration Design

Deliverable: "Secure backend integration design," per L2 HLD UseCase6. Extends L2/UC3's JWT + masking design (see `L2/UC3-Secure-Banking-Data-Integration/`) with this use case's two additions: RBAC as an explicit, named policy, and structured audit/access logging.

## RBAC model

### Roles

| Role | Defined in | Meaning |
|---|---|---|
| `CUSTOMER` | `security/Role.java` | Default role. Can only access its own subject's account data. |
| `SUPPORT_AGENT` | `security/Role.java` | Read-only cross-customer access. Everything this role can see was already masked by UC3's `PiiMasking`/`BankingToolService` before it leaves the service — this role never grants visibility into anything a CUSTOMER-scoped call wouldn't also have masked. |
| `ADMIN` | `security/Role.java` | Full cross-customer access (unchanged from UC3's original single-role check). |

### Permission matrix

| Role | Own data | Other customer's data |
|---|---|---|
| CUSTOMER | allow | deny |
| SUPPORT_AGENT | allow | allow (read-only, masked) |
| ADMIN | allow | allow |
| (no recognized role) | allow | deny |

This table is not just documentation — it is exactly what `security/AccessPolicy.evaluate()` implements, and exactly what `rbac/RbacAndAuditSelfTests.java`'s 7 `AccessPolicy`-level tests assert against, one row at a time. A reviewer can diff this table against that method's `if`/`switch` logic directly.

### Two enforcement layers, deliberately both present

1. **Service-layer RBAC** (`AccessPolicy`, called from `BankingToolService.verifyAndAuthorize`): the real decision for `/api/v1/support/ask`, because that one endpoint serves both POLICY_QUESTION (no token needed at all) and LIVE_DATA (token-gated, and gated differently per role) intents — a flat Spring Security rule can't express "allow this endpoint, but the allowed *content* depends on role and on which customer is being asked about." UC3 already made this architectural choice (`/api/v1/support/**` stays `permitAll()` at the Spring Security gate); UC6 keeps it and gives the actual decision logic a name and its own test suite.
2. **Method-level RBAC** (`@PreAuthorize("hasRole('ADMIN')")` on `AuditController.tail`, `banking-support-service/`): for endpoints that ARE single-purpose — there's no per-intent nuance to delegate for "view the audit log," it's a flat ADMIN-or-nothing gate — Spring Security's own annotation-based RBAC is the more direct fit, and demonstrates the framework-native RBAC mechanism a reviewer would also expect to see.

Both layers read from the exact same JWT claims (`JwtService.Claims.roles()` → Spring `GrantedAuthority` list, prefixed `ROLE_`, in `JwtAuthenticationFilter`) — there is no second, separately-maintained role source.

## Authentication (unchanged from UC3, re-verified here)

Real HS256 JWTs, `javax.crypto.Mac`-only implementation (`JwtService`), independently re-checked against Python's `hmac`/`base64` in UC3. Nothing about the cryptography changed for UC6 — RBAC sits entirely in the authorization layer described above, on top of the same authentication primitive.

## Structured logging & audit trail (new in UC6)

Two separate structured logs, kept separate on purpose:

| Log | Class | What it records | What it never contains |
|---|---|---|---|
| `reports/audit-log.jsonl` | `logging/StructuredAuditLogger` | Every access *decision*: correlation id, actor subject, actor roles, requested customer id, tool name, decision (`ALLOWED_SELF`/`ALLOWED_ELEVATED`/`DENIED_AUTHENTICATION`/`DENIED_AUTHORIZATION`), reason | Any raw PII field — account numbers, balances, transaction amounts. The class has no method that accepts a free-form message string, so there's no code path by which a caller could log raw data through it even by mistake. |
| `reports/http-access-log.jsonl` | `logging/HttpAccessLogFilter` (service module only) | HTTP method, path, status, duration, a generated correlation id | Headers or request/response bodies — never captured at all, so a Bearer token or a request payload can never end up in this log either. |

Both are hand-rolled JSON (no Jackson/Logstash-encoder reachable in this sandbox — the same reasoning `JwtService`'s Javadoc gives for its own JSON handling), and both are independently unit-tested for exactly this "never contains X" property — see `RbacAndAuditSelfTests.testAuditLogNeverContainsRawAccountNumber`, which asserts it against a real `BankingDataStore` account number, not a hypothetical.

## What did NOT change from UC3/UC5

- `JwtService`'s cryptographic implementation
- `PiiMasking`'s field-level masking rules
- `BankingDataStore`'s data model
- The self-access rule (`subject == requestedCustomerId` always allowed, regardless of role) — UC6 only *adds* who else can be allowed, never narrows what a customer can see of their own data
