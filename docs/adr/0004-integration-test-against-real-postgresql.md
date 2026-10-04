# 4. Integration tests run against real PostgreSQL via Testcontainers

Date: 2026-10-04
Status: Accepted

## Context

An in-memory database such as H2 is fast but is not PostgreSQL: SQL dialect, types and
constraint behaviour differ, so tests can pass while production fails.

## Decision

Integration tests (`*IT` classes, run by Maven Failsafe during `verify`) start a disposable
`postgres:18-alpine` container through Testcontainers. Spring Boot's `@ServiceConnection` points the
app at it automatically. Tests are skipped when Docker is unavailable, and always run in CI.

H2 is kept only for the `local` profile, so the app can be run without Docker.

## Consequences

- The Flyway migration and JPA mappings are proven against the same database engine as
  production.
- Integration tests need Docker and take a few seconds to start a container.
