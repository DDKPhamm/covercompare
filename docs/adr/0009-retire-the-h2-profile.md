# 9. Retire the in-memory H2 profile and use PostgreSQL everywhere

Date: 2026-10-04
Status: Accepted

## Context

Phase 1 had a `local` profile with an in-memory H2 database so the app could run without
Docker. Phase 2 adds a second migration that drops and re-creates CHECK constraints. The first
migration created one of those constraints without a name, and H2 and PostgreSQL generate
different names for it, so no single migration script works on both. More generally, H2 only
imitates PostgreSQL, and every difference is a bug that the local profile hides.

## Decision

Remove the H2 profile and dependency. Migrations are written for PostgreSQL only. Locally, the
system runs with Docker Compose; tests use PostgreSQL in Docker through Testcontainers.

## Consequences

- One database engine in every environment, so a migration that works in tests works in
  production.
- Running quote-service now needs Docker. pricing-service and insurer-simulator have no database
  and still run with plain `./mvnw spring-boot:run`.
- Lesson recorded for future migrations: name every constraint explicitly.
