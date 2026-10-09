# 10. One Dockerfile for every service, and Docker Compose for local and CI end-to-end runs

Date: 2026-10-04
Status: Accepted

## Context

Three services need container images, and the whole system needs to be started together for
local development and for an end-to-end test in CI.

## Decision

- **One multi-stage `Dockerfile`** at the repository root, choosing the service with a `SERVICE`
  build argument. The build stage compiles with the Maven wrapper; the runtime stage contains only
  a JRE and the application.
- **Layered jars.** Spring Boot's `jarmode=tools extract --layers` splits dependencies from
  application code, so a code change only rebuilds the small top layer.
- **Hardened runtime.** The process runs as a non-root user; the heap is sized from the container
  memory limit (`MaxRAMPercentage`); the JVM exits on `OutOfMemoryError` so it is restarted clean.
- **Compose with health checks.** Every service has a readiness health check, and quote-service
  only starts once PostgreSQL, pricing-service and the simulator are healthy. Only quote-service
  is published, and only on `127.0.0.1`; everything else is reachable only on the private
  network.
- **Chaos on by default.** The simulator runs with some latency and a 20% failure rate on one
  insurer, so the resilience behaviour is exercised every time the system is started.
- **End-to-end job in CI.** After the build job passes, CI starts the stack with a random
  database password, runs `scripts/smoke-test.sh`, prints logs on failure and tears down.

## Consequences

- A new service needs no new Dockerfile, just a compose entry.
- The same images will be deployed to AWS in Phase 3.
- The CI run takes longer because it builds three images, but it proves the system works as
  deployed, not just as tested.
