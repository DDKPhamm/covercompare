# CoverCompare

A car insurance price comparison platform built with Java 21 and Spring Boot 4. You send in
driver and vehicle details, and it returns prices from a panel of (fictional) insurers, cheapest
first, with a breakdown of every factor that affected the price.

Built as a portfolio project to practise enterprise Java, APIs, testing, CI/CD, cloud and
AI-assisted development.

![CI](https://github.com/DDKPhamm/covercompare/actions/workflows/ci.yml/badge.svg)

## Roadmap

| Phase | Scope | Status |
|-------|-------|--------|
| 1 | Quote API and pricing engine in one Spring Boot app, PostgreSQL, tests, CI | Done |
| 2 | Split into microservices, mock insurer APIs with resilience (timeouts, retries, circuit breakers), Docker Compose | Planned |
| 3 | AWS deployment with Terraform, security scanning in the pipeline, live demo | Planned |
| 4 | Kubernetes, messaging between services, AI feature | Planned |

## Architecture (Phase 1)

```
HTTP client
    |
    v
QuoteController  (quote.api)    validates JSON, maps errors to RFC 9457 problem details
    |
    v
QuoteService     (quote)        business rules, builds the risk profile, saves the quote
    |                 \
    v                  v
PricingEngine    QuoteRepository (quote.domain)  ->  PostgreSQL (schema managed by Flyway)
(pricing)
    |
    v
RatingFactor x7  (pricing.factors)  driver age, experience, no claims, postcode, vehicle group, vehicle age, excess
```

Design decisions are recorded in [`docs/adr`](docs/adr).

## API

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/quotes` | Price a driver and vehicle against every insurer. Returns `201` with a `Location` header |
| GET | `/api/v1/quotes/{id}` | Fetch a saved quote |
| GET | `/actuator/health` | Health check |
| GET | `/swagger-ui.html` | Interactive API documentation |

Example request:

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
together (such as a licence held longer than the driver has been 17), and `404` for unknown quotes.

## Running locally

Requires Java 21. Maven is downloaded automatically by the wrapper (`mvnw`).

**Without Docker** (in-memory H2 database, data lost on restart):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**With PostgreSQL** (needs Docker):

```bash
export DB_PASSWORD=choose-a-local-password
docker compose up -d
DB_URL=jdbc:postgresql://localhost:5432/covercompare DB_USERNAME=covercompare ./mvnw spring-boot:run
```

On Windows PowerShell, set variables with `$env:DB_PASSWORD="..."` and use `.\mvnw.cmd`.

Then open http://localhost:8080/swagger-ui.html.

## Tests

```bash
./mvnw verify
```

- Unit and web-layer tests run with Surefire.
- `*IT` integration tests run with Failsafe against PostgreSQL in Docker via Testcontainers. They
  are skipped if Docker is not running, and always run in CI.
- JaCoCo fails the build if line coverage drops below 80%. The report is at
  `target/site/jacoco/index.html`.

## Development with AI tools

AI coding assistants are used for scaffolding, test generation and refactoring. Every change is
reviewed, run and tested before it is committed.

## License

MIT
