# L2 HLD UseCase6 / LLD 7.2 "Containerization ... Multi-stage builds,"
# frontend half. Unlike banking-support-service's Dockerfile, THIS one's
# build stage was actually exercised in this sandbox -- npm's registry is
# reachable here even though Maven Central isn't (see ../README.md) -- so
# `npm run build` in the build stage below is the exact command verified
# in reports/frontend-build-log.txt, not just documented.
#
# Disclosed limitation: `docker build` itself was not run (no Docker
# daemon available in this sandbox -- see the top-level README's CI/CD
# section), but every command inside each stage was run directly and
# produces the same `dist/` this stage produces.

# ---------- Stage 1: build ----------
FROM node:22-slim AS build
WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# VITE_API_BASE_URL is baked in at build time (Vite inlines import.meta.env.*
# into the bundle) -- pass it as a build arg per deployment target.
ARG VITE_API_BASE_URL=http://localhost:8085
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ---------- Stage 2: serve ----------
FROM nginx:1.27-alpine AS run
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:80/ || exit 1
