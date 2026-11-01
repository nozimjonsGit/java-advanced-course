#!/usr/bin/env bash
#
# Gets an access token through the OAuth2 authorization code flow with PKCE, using only curl.
# It does what a browser and an OAuth2 client would do: log in, receive a code, and exchange it.
#
# Usage:   scripts/get-token.sh <username> <password>
# Example: TOKEN=$(scripts/get-token.sh admin admin-password)
#
# Set BASE_URL and CLIENT_SECRET to use anything other than the local profile defaults.

set -euo pipefail

username=${1:?Usage: get-token.sh <username> <password>}
password=${2:?Usage: get-token.sh <username> <password>}

base_url=${BASE_URL:-http://localhost:8080}
client_id=${CLIENT_ID:-module1-client}
client_secret=${CLIENT_SECRET:-local-secret}
redirect_uri=http://127.0.0.1:8089/callback

cookies=$(mktemp)
trap 'rm -f "$cookies"' EXIT

verifier=$(openssl rand -base64 48 | tr '+/' '-_' | tr -d '=\n')
challenge=$(printf '%s' "$verifier" | openssl dgst -sha256 -binary | openssl base64 -A | tr '+/' '-_' | tr -d '=')

csrf_token=$(curl --silent --show-error --fail --cookie "$cookies" --cookie-jar "$cookies" "$base_url/login" |
    sed -n 's/.*name="_csrf"[^>]*value="\([^"]*\)".*/\1/p')
if [[ -z "$csrf_token" ]]; then
    echo "Could not read the CSRF token from $base_url/login" >&2
    exit 1
fi

login_redirect=$(curl --silent --show-error --output /dev/null --write-out '%{redirect_url}' \
    --cookie "$cookies" --cookie-jar "$cookies" "$base_url/login" \
    --data-urlencode "username=$username" \
    --data-urlencode "password=$password" \
    --data-urlencode "_csrf=$csrf_token")
if [[ "$login_redirect" == *"error"* ]]; then
    echo "Login failed for user '$username'" >&2
    exit 1
fi

authorize_redirect=$(curl --silent --show-error --output /dev/null --write-out '%{redirect_url}' --get \
    --cookie "$cookies" --cookie-jar "$cookies" "$base_url/oauth2/authorize" \
    --data-urlencode "response_type=code" \
    --data-urlencode "client_id=$client_id" \
    --data-urlencode "redirect_uri=$redirect_uri" \
    --data-urlencode "code_challenge=$challenge" \
    --data-urlencode "code_challenge_method=S256")
code=$(printf '%s' "$authorize_redirect" | sed -n 's/.*[?&]code=\([^&]*\).*/\1/p')
if [[ -z "$code" ]]; then
    echo "No authorization code in redirect: '$authorize_redirect'" >&2
    exit 1
fi

response=$(curl --silent --show-error --fail --user "$client_id:$client_secret" "$base_url/oauth2/token" \
    --data-urlencode "grant_type=authorization_code" \
    --data-urlencode "code=$code" \
    --data-urlencode "redirect_uri=$redirect_uri" \
    --data-urlencode "code_verifier=$verifier")

printf '%s\n' "$response" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p'
