#!/usr/bin/env bash
# End-to-end check against the running compose stack: real containers, real network, real database.
# Usage: scripts/smoke-test.sh [base-url]
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
HERE="$(cd "$(dirname "$0")" && pwd)"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

echo "Creating a quote..."
created="$(curl --silent --show-error --fail-with-body --max-time 10 \
  -H 'Content-Type: application/json' \
  -d @"$HERE/sample-quote.json" \
  "$BASE_URL/api/v1/quotes")" || fail "quote request was not successful: $created"

id="$(jq -r '.id' <<<"$created")"
[[ "$id" =~ ^[0-9a-f-]{36}$ ]] || fail "response has no quote id: $created"

insurers="$(jq '.insurerQuotes | length' <<<"$created")"
[[ "$insurers" == 3 ]] || fail "expected an outcome for all 3 insurers, got $insurers"

factors="$(jq '.ratingFactors | length' <<<"$created")"
[[ "$factors" == 7 ]] || fail "expected 7 rating factors from pricing-service, got $factors"

echo "Insurer outcomes:"
jq -r '.insurerQuotes[] | "  \(.insurerName): \(.status) \(.totalAnnualPremium // .declineReason // .unavailableReason)"' \
  <<<"$created"

echo "Reading it back..."
fetched="$(curl --silent --show-error --fail-with-body --max-time 5 "$BASE_URL/api/v1/quotes/$id")" \
  || fail "could not read quote $id back"
[[ "$(jq -r '.id' <<<"$fetched")" == "$id" ]] || fail "read back a different quote"

echo "Checking internal services are not published to the host..."
for port in 8081 8082 5432; do
  if timeout 2 bash -c "</dev/tcp/127.0.0.1/$port" 2>/dev/null; then
    fail "port $port is reachable from the host; only quote-service should be"
  fi
done

echo "Smoke test passed."
