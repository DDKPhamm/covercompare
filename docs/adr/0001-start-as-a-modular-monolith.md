# 1. Start as a modular monolith

Date: 2026-10-04
Status: Accepted

## Context

The end goal is a set of microservices (quotes, pricing, insurer adapters, policies). Building
several services from day one would mean solving networking, service discovery, distributed
tracing and deployment before a single quote can be priced.

## Decision

Phase 1 is one Spring Boot application split into feature packages (`quote`, `pricing`) with
one-way dependencies: `quote` calls `pricing`, never the reverse. `pricing` has no knowledge of HTTP
or the database.

## Consequences

- Fast to build, test and run locally; one deployable.
- The package boundaries are the future service boundaries, so Phase 2 can extract `pricing`
  into its own service with little rework.
- Risk: boundaries can erode if code starts reaching across packages. Package-private classes
  (for example the rating factors) limit this.
