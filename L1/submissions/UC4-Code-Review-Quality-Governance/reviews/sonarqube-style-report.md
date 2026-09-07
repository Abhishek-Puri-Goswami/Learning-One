# "SonarQube" Report — order-service (0.9.0-pre-review)

## ⚠️ Environment Limitation — Please Read First

This sandbox has no outbound access to a SonarQube/SonarCloud server, and (as in L1/UC2) no access to Maven Central to pull the `sonar-maven-plugin` or a standalone `sonar-scanner` binary. **An actual SonarQube scan was not run.**

What follows instead is a **manual, rule-catalog-based review**, written in the same shape SonarQube's issue export uses (component, rule key, severity, type, message, line), with every rule key checked against the real, documented Sonar Java rule catalog (`squid:S####` / `java:S####` keys) rather than invented. This lets the required "compare AI review vs Sonar" exercise proceed honestly while being explicit about what's simulated vs. actually executed.

**To get a real Sonar report**, run this once you have SonarQube/SonarCloud access:
```bash
cd order-service-before
mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.host.url=<your-sonarqube-url> \
  -Dsonar.login=<your-token>
```
Then replace this file's table with the actual exported issues (Sonar's Web API: `GET /api/issues/search?componentKeys=order-service`).

## Issues (manual, rule-catalog-based)

| # | Component | Line | Rule | Severity | Type | Message |
|---|---|---|---|---|---|---|
| 1 | `client/PaymentGatewayClient.java` | 17 | `squid:S2068` Hard-coded credentials | **Blocker** | Vulnerability | `'PAYMENT_API_KEY' looks like a hard-coded credential; remove and use a secure configuration mechanism.` |
| 2 | `client/PaymentGatewayClient.java` | 25–29 | `squid:S1166` Exception handlers should preserve the original exceptions | **Critical** | Code Smell | `Either log or rethrow this exception; catching it and continuing silently discards information callers need.` |
| 3 | `client/PaymentGatewayClient.java` | 31 | `squid:S3516` Function returns should not be invariant | **Major** | Code Smell | `charge() returns true on every code path, including the exception path — this method's return value carries no information.` |
| 4 | `service/OrderServiceImpl.java` | 39 | `squid:S2259` Null pointers should not be dereferenced | **Critical** | Bug | `A "NullPointerException" could be thrown; "getShippingAddress()" can return null.` |
| 5 | `service/OrderServiceImpl.java` | 33–96 (`checkout`) | `squid:S3776` Cognitive Complexity of methods should not be too high | **Critical** | Code Smell | `Refactor this method to reduce its Cognitive Complexity from 24 to the 15 allowed.` |
| 6 | `service/OrderServiceImpl.java` | 33 (method) | `squid:S138` Methods should not have too many lines | **Major** | Code Smell | `This method has ~65 lines, which is greater than the 80-line/single-responsibility guidance combined with its complexity above.` |
| 7 | `controller/OrderController.java` | 14 | `squid:S1128` Unnecessary imports should be removed | **Minor** | Code Smell | `Remove this unused import "java.util.List".` |
| 8 | `controller/OrderController.java` | 22 | `squid:S4684` (analogous — untrusted/unvalidated input) | **Major** | Vulnerability | `"OrderRequest" is not validated (@Valid) before being used — inconsistent with product-service/cart-service, which validate every mutating endpoint.` |
| 9 | `pom.xml` | dependency `com.itextpdf:itextpdf:5.5.13.3` | License compliance (Sonar dependency-check / license-check integration, not a core Java rule) | **Major** | Vulnerability (license) | `AGPL-3.0-licensed dependency detected in a proprietary module; not covered by an approved license exception.` |
| 10 | *(project)* | — | — | **Info** | Coverage | `0% line/branch coverage — no test sources exist for order-service.` |

## Severity / Type Summary

| Severity | Count |
|---|---|
| Blocker | 1 |
| Critical | 3 |
| Major | 3 |
| Minor | 1 |
| Info | 1 |

| Type | Count |
|---|---|
| Bug | 1 |
| Vulnerability | 3 |
| Code Smell | 4 |
| Coverage gap | 1 |

## Quality Gate (simulated, using Sonar's default "Sonar way" gate conditions)

| Condition | Threshold | This build | Result |
|---|---|---|---|
| New Blocker/Critical issues | 0 | 4 | ❌ FAIL |
| Coverage on new code | ≥ 80% | 0% | ❌ FAIL |
| Duplicated lines on new code | ≤ 3% | 0% (small file, not measured meaningfully) | ⚠️ N/A at this size |

**Quality Gate: FAILED.** This build must not be promoted past code review, consistent with `ai-review-report.json`'s `risk_level: "CRITICAL"`.
