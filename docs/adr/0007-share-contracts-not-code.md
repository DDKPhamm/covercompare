# 7. Share contract files between services, not Java classes

Date: 2026-10-04
Status: Accepted

## Context

quote-service sends a rating request to pricing-service and reads the rating it returns. The
easy option is a shared library of request and response classes that both services depend on.
That couples their release cycles: changing the library means rebuilding and redeploying both
services together, which removes most of the reason for splitting them.

## Decision

Each service keeps its own copy of the request and response classes. The agreed messages are
stored once, as example JSON files in `contracts/`, and both sides test against the same files:

- pricing-service (the provider) posts `rating-request.json` to its real controller and checks
  the response matches `rating-response.json` exactly.
- quote-service (the consumer) points its real HTTP client at a WireMock server that only answers
  if the request body equals `rating-request.json`, and replies with `rating-response.json`.

## Consequences

- If either side changes the message shape, its own build fails before anything is deployed.
- The services stay independently releasable.
- This is a lightweight form of consumer-driven contract testing. A tool such as Pact or Spring
  Cloud Contract would add versioning and a broker, which is worth it once teams and repositories
  are separate, but is unnecessary overhead in a single repository.
