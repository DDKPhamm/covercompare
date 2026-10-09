# 6. Split into three services along business boundaries

Date: 2026-10-04
Status: Accepted. Supersedes [1](0001-start-as-a-modular-monolith.md).

## Context

Phase 1 was one application with `quote` and `pricing` packages. The package boundaries held up:
`quote` only used `pricing` through `PricingEngine`. Real comparison sites also talk to insurers
over the network, each with its own API, and those calls are slow and unreliable in ways an
in-process method call never is.

## Decision

Three Spring Boot services in one Maven multi-module build:

- **pricing-service** rates a risk profile and returns the market risk premium with an
  explanation of every factor. It is stateless and owns no data.
- **insurer-simulator** stands in for three insurers with deliberately different APIs (camelCase
  JSON in pounds, snake_case JSON in pence, XML with HTTP status codes) and can inject latency
  and failures on demand.
- **quote-service** is the only public service. It validates the customer's details, calls
  pricing-service, asks every insurer for a price in parallel and stores the result.

Each service has its own `pom.xml`, configuration, tests and coverage gate. They share no Java
code; see [7](0007-share-contracts-not-code.md).

## Consequences

- Each service can be built, tested, deployed and scaled on its own.
- Insurer quirks live in one adapter per insurer inside quote-service, so adding an insurer means
  adding one class, not changing the panel.
- Failures that were impossible in-process (timeouts, refused connections, malformed responses)
  now have to be handled explicitly; see [8](0008-guard-every-remote-call.md).
- Local development needs several processes, so Docker Compose becomes the normal way to run the
  whole system; see [10](0010-one-dockerfile-and-compose.md).
