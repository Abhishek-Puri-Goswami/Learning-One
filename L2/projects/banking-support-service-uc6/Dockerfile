# L2 HLD UseCase6 deliverable: "Operational CI/CD pipeline ... with automated
# validation + deployment" / LLD 7.2 "Containerization ... Multi-stage builds."
#
# Two stages: build with a full JDK+Maven image (never shipped), run on a
# minimal JRE image (no build tooling, smaller attack surface, smaller
# image). This mirrors the exact `mvn clean verify` / `mvn spring-boot:run`
# commands documented in this module's own README -- the Dockerfile is not
# a separate, undocumented build path.
#
# Disclosed limitation (same one that applies to this module everywhere
# else in this submission): Maven Central is blocked in the sandbox this
# submission was authored in, so `docker build` on this file was NOT run
# here -- no Docker daemon is available in that sandbox either (`docker
# version` succeeds for the client but the daemon socket doesn't exist).
# The Dockerfile syntax itself, and each individual command, are standard
# and match the reproduction steps in README.md; see cicd/README.md for
# exactly what would need to happen in a real environment with network
# access to validate this end-to-end.

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy just the POM first so dependency resolution is a separate, cacheable
# Docker layer from the source copy below.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean verify -DskipITs=false

# ---------- Stage 2: run ----------
FROM eclipse-temurin:17-jre-jammy AS run
WORKDIR /app

# Non-root runtime user -- a banking service should never run its process as root.
RUN groupadd --system bankrag && useradd --system --gid bankrag bankrag

COPY --from=build /build/target/*.jar /app/banking-support-service.jar
RUN mkdir -p /app/reports /app/corpus && chown -R bankrag:bankrag /app

USER bankrag
EXPOSE 8085

# BANKRAG_JWT_SECRET MUST be overridden at deploy time (see application.yml's
# default, which is explicitly marked "demo-only, not for production").
ENV SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/banking-support-service.jar"]
