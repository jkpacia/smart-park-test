#!/usr/bin/env bash
set -eu
: "${TOKEN:?Run 01-login.sh first, then export TOKEN='your-access-token'}"

curl --silent --show-error --fail-with-body \
  "${BASE_URL:-http://localhost:8080}/api/lots" \
  --header "Authorization: Bearer $TOKEN" \
  --header 'Content-Type: application/json' \
  --data '{"lotId":"LOT-A","location":"Main Street","capacity":1,"costPerMinute":0.50}' \
  --write-out '\n'
