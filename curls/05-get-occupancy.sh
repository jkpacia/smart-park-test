#!/usr/bin/env bash
set -eu
: "${TOKEN:?Run 01-login.sh first, then export TOKEN='your-access-token'}"

curl --silent --show-error --fail-with-body \
  "${BASE_URL:-http://localhost:8080}/api/lots/LOT-A/occupancy" \
  --header "Authorization: Bearer $TOKEN" \
  --write-out '\n'
