# 8. Guard every remote call with timeouts, retries, circuit breakers and a deadline

Date: 2026-10-04
Status: Accepted

## Context

A quote needs one call to pricing-service and one call to each insurer. Without protection, the
slowest insurer decides how long every customer waits, one broken insurer fails every quote, and
a hung connection holds a thread forever (the JDK HTTP client's default timeout is infinite).

## Decision

- **Timeouts on every connection.** Each client has an explicit connect timeout (500 ms) and
  read timeout (1 s for insurers, 2 s for pricing).
- **Retry only what might succeed next time.** HTTP 5xx and refused connections are retried with
  a short pause. Timeouts are not retried (the service is slow, so trying again doubles the wait)
  and nor are 4xx responses or responses we cannot parse (they will be the same next time).
- **A circuit breaker per insurer** (Resilience4j). If half of the last 10 calls to an insurer
  failed, stop calling it for 30 seconds, then let trial calls through. Retry wraps the circuit
  breaker, so every attempt counts towards the failure rate and an open breaker stops retries.
- **Parallel calls with an overall deadline.** All insurers are called at the same time on
  virtual threads. Whatever has not answered within 3 seconds is shown as unavailable.
- **Degrade, don't fail.** An insurer that cannot be reached is stored and shown as
  `UNAVAILABLE` with a customer-safe reason; the other prices are still returned. Pricing is
  different: without a rating there is nothing to show, so the request fails with `503` and a
  `Retry-After` header, and nothing is saved.
- **No database transaction during remote calls.** `createQuote` is not `@Transactional`; only
  the final save opens a transaction, so slow insurers cannot exhaust the connection pool.

## Consequences

- The worst-case response time is bounded (about 3 seconds plus pricing) regardless of how badly
  insurers behave.
- Customers sometimes see fewer prices than there are insurers, which is how real comparison
  sites behave.
- All thresholds are configuration, so they can be tuned from production metrics without a code
  change.
