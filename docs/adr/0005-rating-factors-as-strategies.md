# 5. Each rating factor is a separate strategy

Date: 2026-10-04
Status: Accepted

## Context

Insurance pricing changes often: new factors are added, and bands are tuned. A single large
pricing method with nested `if` statements becomes hard to read, test and change safely.

## Decision

Each factor implements `RatingFactor` and is a Spring bean. `PricingEngine` receives all of them
as a `List<RatingFactor>`, ordered by `@Order`, and multiplies the base premium by each factor's
result. Every adjustment carries a human-readable reason, returned to the customer as a breakdown.

## Consequences

- Adding a factor means adding one class and its tests; `PricingEngine` does not change
  (open/closed principle).
- Each factor is tested in isolation with parameterised boundary tests.
- Factors are independent multipliers; rules that combine factors (for example, young driver in a
  high group) would need a different design.
