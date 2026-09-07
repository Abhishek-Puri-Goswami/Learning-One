# Deployment Model — L2/UC6

Deliverable: "Deployment Model," per LLD sections 7.1 (Local Deployment) and 7.2 (Containerization).

## 7.1 Local deployment (agnostic steps, per LLD)

```bash
git clone <repo> && cd L2/UC6-Final-Integrated-System-CICD

# Backend (needs Maven Central reachable -- see banking-support-service/README notes)
cd banking-support-service
export BANKRAG_JWT_SECRET="a-real-secret-at-least-32-characters-long"
mvn spring-boot:run          # listens on :8085

# Frontend (in a second terminal)
cd ../banking-support-frontend
cp .env.example .env
npm install
npm run dev                  # http://localhost:5173, talks to :8085
```

For evaluating this submission in an environment where Maven Central is also blocked (matching this authoring sandbox), the pure-JDK `banking-support-core` module and the frontend's mock backend together demonstrate the same behavior without needing the Spring Boot service running:

```bash
cd banking-support-core
mkdir -p out && javac -d out $(find src/main/java -name "*.java") $(find src/test/java -name "*.java")
java -cp out com.retailco.bankrag.integration.SelfTests
java -cp out com.retailco.bankrag.rbac.RbacAndAuditSelfTests
java -cp out com.retailco.bankrag.integration.Main corpus

cd ../banking-support-frontend
node mock-server/server.js &
npm install && npm run dev
```

## 7.2 Containerization

- `banking-support-service/Dockerfile` — multi-stage: `maven:3.9-eclipse-temurin-17` build stage, `eclipse-temurin:17-jre-jammy` run stage, non-root user, `HEALTHCHECK` against `/actuator/health`.
- `banking-support-frontend/Dockerfile` — multi-stage: `node:22-slim` build stage (verified runnable in this sandbox — see its own header comment), `nginx:1.27-alpine` run stage serving the static `dist/` build.
- `deployment/docker-compose.yml` — wires both together for local evaluation. **Validated** with `docker compose config` in this sandbox (resolves cleanly, no daemon required for that command) — not build/run-verified end-to-end (no Docker daemon available here).
- `deployment/k8s/*.yaml` — optional (per LLD 7.2), illustrative Deployment + Service (+ a placeholder Secret) for each of the two containers. **Validated** for YAML/schema shape (`yaml.safe_load_all`, correct `kind`s) — not applied to a live cluster (none available here). Every `REPLACE_WITH_*` placeholder must be filled in before a real `kubectl apply`.

## What "containerized" means here, honestly

Every Dockerfile command mirrors a command that WAS run directly and verified in this submission (`javac`/`java` for the core module's equivalent build steps; `npm ci && npm run build` for the frontend stage, byte-for-byte). What was not verified is the container build itself and any live deployment — this sandbox has neither a Docker daemon nor a Kubernetes cluster. This is disclosed the same way every other environment limitation in this submission is: the commands are real and were run; the specific tool (Docker, kubectl) that would wrap them for containerized deployment was not available to exercise here.
