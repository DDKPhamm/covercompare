# CoverCompare

A car insurance price comparison platform built with Java 21 and Spring Boot 4. You send in
driver and vehicle details, and it returns prices from a panel of (fictional) insurers, cheapest
first, with a breakdown of every factor that affected the price.

Built as a portfolio project to practise enterprise Java, microservices, APIs, testing, CI/CD,
cloud and AI-assisted development.

![CI](https://github.com/DDKPhamm/covercompare/actions/workflows/ci.yml/badge.svg)

## Roadmap

| Phase | Scope | Status |
|-------|-------|--------|
| 1 | Quote API and pricing engine in one Spring Boot app, PostgreSQL, tests, CI | Done |
| 2 | Split into microservices, mock insurer APIs with resilience (timeouts, retries, circuit breakers), Docker Compose | Done |
| 3 | AWS deployment with Terraform, security scanning in the pipeline, live demo | Planned |
| 4 | Kubernetes, messaging between services, AI feature | Planned |

## Architecture

```
                 customer
                    |
                    v
          +-------------------+        +------------+
          |   quote-service   |------->| PostgreSQL |
          |   :8080 (public)  |        +------------+
          +-------------------+
            |              | in parallel, each with timeout, retry,
            |              | circuit breaker, inside a 3 s deadline
            v              v
 +-----------------+   +----------------------------------------+
 | pricing-service |   |           insurer-simulator            |
 |      :8081      |   |                 :8082                  |
 | 7 rating factors|   | Pennine (JSON, pounds)                 |
 |   risk premium  |   | Lighthouse (snake_case JSON, pence)    |
 +-----------------+   | Redbrick (XML, HTTP status codes)      |
                       | + configurable latency and failures    |
                       +----------------------------------------+
```

1. **quote-service** validates the request and works out the driver's age, postcode area and
   vehicle age.
2. It sends only those derived values to **pricing-service**, which applies seven rating factors
   and returns the market risk premium with an explanation for each factor.
3. It asks every insurer for a price **at the same time**, through one adapter per insurer API.
   Each insurer has its own circuit breaker, and the panel waits at most 3 seconds.
4. It stores and returns every outcome: `QUOTED` (cheapest first), `DECLINED` with the insurer's
   reason, or `UNAVAILABLE` if the insurer timed out or is failing.

The services share no Java code. The messages between quote-service and pricing-service are
pinned by example files in [`contracts/`](contracts) that both sides test against.

Design decisions are recorded in [`docs/adr`](docs/adr).

## API

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/quotes` | Price a driver and vehicle against every insurer. Returns `201` with a `Location` header |
| GET | `/api/v1/quotes/{id}` | Fetch a saved quote |
| GET | `/actuator/health` | Health check |
| GET | `/swagger-ui.html` | Interactive API documentation |

Example request ([`scripts/sample-quote.json`](scripts/sample-quote.json)):

```json
{
  "driver": { "dateOfBirth": "1991-03-15", "licenceHeldYears": 10, "noClaimsYears": 5, "postcode": "SK1 3AB" },
  "vehicle": { "make": "Ford", "model": "Fiesta", "year": 2021, "insuranceGroup": 20 },
  "coverType": "COMPREHENSIVE",
  "voluntaryExcess": 250
}
```

Errors use [RFC 9457 problem details](https://www.rfc-editor.org/rfc/rfc9457): `400` for
malformed or invalid fields, `422` for details that are valid individually but impossible
together (such as a licence held longer than the driver has been 17), `404` for unknown quotes,
and `503` with `Retry-After` if pricing-service is unavailable.

## Running locally

Requires Docker. Everything else is built inside containers.

```bash
export DB_PASSWORD=choose-a-local-password
docker compose up --build --wait
bash scripts/smoke-test.sh
```

On Windows PowerShell, set the password with `$env:DB_PASSWORD="..."`.

Then open http://localhost:8080/swagger-ui.html. Only quote-service is published to the host.

The simulator misbehaves a little by default (Pennine is slowish, Lighthouse fails 20% of the
time, Redbrick sometimes exceeds the timeout) so you can watch the resilience features work. For
a perfectly behaved panel:

```bash
PENNINE_LATENCY=0ms LIGHTHOUSE_FAILURE_RATE=0 REDBRICK_LATENCY=0ms REDBRICK_JITTER=0ms docker compose up --build --wait
```

To work on a single service, run the others in Compose and start that service from your IDE or
with `./mvnw -pl <service> spring-boot:run`.

## Tests

Requires Java 21. Maven is downloaded automatically by the wrapper (`mvnw`).

```bash
./mvnw verify
```

- Unit and web-layer tests run with Surefire in every module.
- HTTP clients and insurer adapters are tested against WireMock, including server errors,
  timeouts, malformed responses and an XML external entity (XXE) attack.
- `*IT` integration tests run with Failsafe against PostgreSQL in Docker via Testcontainers. They
  are skipped if Docker is not running, and always run in CI.
- JaCoCo fails the build if line coverage in any module drops below 80%.
- CI then builds the images, starts the whole stack with Docker Compose and runs the smoke test.

## Development with AI tools

AI coding assistants are used for scaffolding, test generation and refactoring. Every change is
reviewed, run and tested before it is committed.

## License

MIT
