# 3. Flyway owns the database schema

Date: 2026-10-04
Status: Accepted

## Context

Hibernate can create tables from entity classes (`ddl-auto=update`), but it cannot rename
columns safely, never deletes anything, produces no reviewable history and behaves differently
across environments.

## Decision

Schema changes are versioned SQL files in `src/main/resources/db/migration`, applied by Flyway at
startup. Hibernate runs with `ddl-auto=validate`, so the app fails to start if the entities and
the schema disagree.

## Consequences

- Every schema change is code-reviewed SQL with a version number, applied identically in every
  environment.
- Migrations that have been applied must never be edited; changes go in a new `V2__...` file.
- Database constraints (CHECKs, foreign keys) back up the Java validation.
