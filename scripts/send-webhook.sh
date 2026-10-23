#!/usr/bin/env bash
# Sends a Paystack-style webhook, signed with PAYSTACK_SECRET_KEY, to the local app.
#
# Usage:
#   scripts/send-webhook.sh <event> <reference> [reason]
#
# Examples:
#   scripts/send-webhook.sh charge.success THRIFT-1234
#   scripts/send-webhook.sh transfer.success payout-65f1c0ffee
#   scripts/send-webhook.sh transfer.failed payout-65f1c0ffee "Account closed"
#
# Environment:
#   PAYSTACK_SECRET_KEY  Must match the app's key. Read from .env if not set.
#   WEBHOOK_URL          Default: http://localhost:8080/api/v1/webhook/paystack
#
# Note: charge.success is re-verified with Paystack by the app, so without a real
# test-mode payment it ends up FAILED. Transfer events are fully handled locally.

set -euo pipefail

if [[ $# -lt 2 ]]; then
    sed -n '2,17p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
fi

event="$1"
reference="$2"
reason="${3:-}"

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -z "${PAYSTACK_SECRET_KEY:-}" && -f "$repo_root/.env" ]]; then
    PAYSTACK_SECRET_KEY="$(grep -E '^PAYSTACK_SECRET_KEY=' "$repo_root/.env" | tail -1 | cut -d= -f2-)"
fi
if [[ -z "${PAYSTACK_SECRET_KEY:-}" ]]; then
    echo "PAYSTACK_SECRET_KEY is not set (and not found in .env)" >&2
    exit 1
fi

url="${WEBHOOK_URL:-http://localhost:8080/api/v1/webhook/paystack}"

# Escape for JSON so references or reasons with quotes can't break the payload
json_escape() {
    local s="${1//\\/\\\\}"
    s="${s//\"/\\\"}"
    printf '%s' "$s"
}

data="\"reference\":\"$(json_escape "$reference")\""
if [[ -n "$reason" ]]; then
    data="$data,\"reason\":\"$(json_escape "$reason")\""
fi
payload="{\"event\":\"$(json_escape "$event")\",\"data\":{$data}}"

# Paystack signs the exact request body with HMAC-SHA512 using the secret key
signature="$(printf '%s' "$payload" | openssl dgst -sha512 -hmac "$PAYSTACK_SECRET_KEY" | sed 's/^.*= //')"

echo "POST $url"
echo "$payload"
echo

response_file="$(mktemp)"
trap 'rm -f "$response_file"' EXIT

status="$(curl -sS -o "$response_file" -w '%{http_code}' \
    -X POST "$url" \
    -H 'Content-Type: application/json' \
    -H "x-paystack-signature: $signature" \
    --data-binary "$payload")"
echo "HTTP $status: $(cat "$response_file")"
[[ "$status" == "200" ]]
