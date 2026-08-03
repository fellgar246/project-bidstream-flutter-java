#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
AUTH_URL="${BASE_URL}/auth/login"

export $(grep -v '^#' .env 2>/dev/null | xargs) || true

echo "==> Demo seed via actuator"
curl -sf -X POST http://localhost:8080/actuator/demoSeed | tee /tmp/demo-seed.json
echo ""
echo "Demo credentials:"
echo "  buyer:  demo-buyer@bidstream.demo / demo1234"
echo "  seller: demo-seller@bidstream.demo / demo1234"
