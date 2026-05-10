#!/usr/bin/env sh
set -eu

BASE_URL="${LOTTERY_SMOKE_BASE_URL:-http://127.0.0.1:8080}"
BACKEND_URL="${LOTTERY_SMOKE_BACKEND_URL:-$BASE_URL}"

curl -fsS "$BASE_URL/health" >/dev/null
curl -fsS "$BACKEND_URL/ready" >/dev/null
curl -fsS "$BACKEND_URL/api/v1/openapi.yaml" >/dev/null

printf '%s\n' "Smoke test passed for $BASE_URL"
