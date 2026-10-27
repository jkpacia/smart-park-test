#!/usr/bin/env bash
set -eu
: "${TOKEN:?Run 01-login.sh first, then export TOKEN='your-access-token'}"

curl --silent --show-error --fail-with-body \
  "${BASE_URL:-http://localhost:8080}/api/vehicles" \
  --header "Authorization: Bearer $TOKEN" \
  --header 'Content-Type: application/json' \
  --data '{"licensePlate":"CAR-002","type":"CAR","ownerName":"John Doe"}' \
  --write-out '\n'
