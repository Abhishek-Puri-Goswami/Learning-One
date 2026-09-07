# CI/CD Pipeline — L2/UC6

Deliverable: "Operational CI/CD pipeline (GitHub Actions / Azure DevOps) with automated validation + deployment," per L2 HLD UseCase6. Pipeline definition: `.github/workflows/ci-cd.yml`.

## What each job does, and what was actually verified where

| Job | LLD 6.1 requirement it satisfies | Actually run in this sandbox? |
|---|---|---|
| `lint` | "Perform lint checks" | **Yes** — `javac -Xlint:all`, real output in `banking-support-core/reports/core-javac-lint-log.txt` |
| `core-tests` | "Run tests" / "Validate RAG components" | **Yes** — compiles + runs `integration.SelfTests` (14/14) + the full demo, real output in `banking-support-core/reports/` |
| `rbac-and-security-tests` | "Validate JWT logic" / "Masking security tests" / "Unauthorized access tests" | **Yes** — `RbacAndAuditSelfTests` (14/14) + the secret-scan grep step, both run directly |
| `evaluation` | "LangSmith evaluation stage" | **Yes** — reruns the demo, validates `integrated-trace-log.jsonl` is well-formed JSONL with a real Python script |
| `frontend-build-and-lint` | "Perform lint checks" (frontend half) | **Yes** — `npm run lint` (oxlint) and `npm run build` (vite), real output in `banking-support-frontend/reports/frontend-build-log.txt` |
| `maven-build-service` | part of "Build container images"'s prerequisite | **No** — needs Maven Central, blocked in this sandbox (same limitation as every Spring Boot module in this submission) |
| `docker-build` | "Build container images" | **No** — no Docker daemon in this sandbox (`docker version` succeeds for the client; the daemon socket doesn't exist) |
| `deploy` | "Approve deployment only when tests pass" | **No** — illustrative only; needs real registry/cluster credentials this submission doesn't provision |

The five jobs marked "Yes" needed no external network access beyond what this sandbox already has (pure-JDK `javac`/`java`, and npm — reachable here even though Maven Central isn't). This is not a coincidence: it's the same "verify what's actually verifiable here" strategy used throughout this submission (pure-JDK core modules, hand-rolled JSON/JWT, etc.), applied to CI/CD design itself — a pipeline whose test stages don't depend on the one thing this environment can't reach.

## "Allow deployment only if tests pass" — how that's enforced, concretely

GitHub Actions' `needs:` field, not a comment. `deploy`'s `needs: [docker-build]`, and `docker-build`'s `needs: [core-tests, rbac-and-security-tests, evaluation, frontend-build-and-lint, maven-build-service]` — a failure anywhere in that chain means `docker-build` never starts, which means `deploy` never starts. This is the literal mechanism, not a policy statement; see `diagrams/architecture-diagrams.md`'s job-graph diagram for the visual.

`deploy` additionally gates on `if: github.ref == 'refs/heads/main' && github.event_name == 'push'` — a pull request run exercises every test/lint/build job (so a reviewer sees pass/fail on the PR itself) but never reaches the deploy step, which only fires on an actual merge to `main`.

## Reproducing the verified jobs locally

```bash
# lint
cd banking-support-core && javac -Xlint:all -d /tmp/out $(find src/main/java -name "*.java") $(find src/test/java -name "*.java")

# core-tests
cd banking-support-core && javac -d out $(find src/main/java -name "*.java") $(find src/test/java -name "*.java")
java -cp out com.retailco.bankrag.integration.SelfTests
java -cp out com.retailco.bankrag.integration.Main corpus

# rbac-and-security-tests
java -cp out com.retailco.bankrag.rbac.RbacAndAuditSelfTests

# frontend-build-and-lint
cd banking-support-frontend && npm ci && npm run lint && npm run build
```

## What a real environment would additionally need to run this end-to-end

1. Network access to Maven Central (or an internal mirror) for `maven-build-service`.
2. A Docker daemon (or a hosted builder like GitHub Actions' own runners, which have one) for `docker-build`.
3. Real deployment credentials (registry push token, cluster kubeconfig or Azure DevOps service connection) for `deploy` — see `deployment/deployment-model.md`.

None of these are exotic requirements — they're exactly what a real GitHub Actions runner already provides, which is why this workflow is expected to run correctly there even though it could only be partially exercised here.
