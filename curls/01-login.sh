#!/usr/bin/env bash
set -eu

curl --silent --show-error --fail-with-body \
  "${BASE_URL:-http://localhost:8080}/api/auth/login" \
  --header 'Content-Type: application/json' \
  --data '{"username":"admin","password":"smartpark-demo"}' \
  --write-out '\n'
