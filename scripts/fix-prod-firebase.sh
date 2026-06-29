#!/bin/bash
set -euo pipefail

PROD_ENV=/opt/vazifa-prod/backend/.env
DEV_ENV=/opt/vazifa-dev/backend/.env

get_val() {
  local file="$1" key="$2"
  grep -m1 "^${key}=" "$file" | sed "s/^${key}=//"
}

PID=$(get_val "$DEV_ENV" FIREBASE_PROJECT_ID)
PEMAIL=$(get_val "$DEV_ENV" FIREBASE_CLIENT_EMAIL)
PKEY=$(get_val "$DEV_ENV" FIREBASE_PRIVATE_KEY)

if [ -z "$PID" ] || [ -z "$PEMAIL" ] || [ -z "$PKEY" ]; then
  echo "DEV_FIREBASE_MISSING"
  exit 1
fi

cp "$PROD_ENV" "${PROD_ENV}.bak.$(date +%Y%m%d%H%M%S)"

# Replace or append Firebase lines
for key in FIREBASE_PROJECT_ID FIREBASE_CLIENT_EMAIL FIREBASE_PRIVATE_KEY; do
  if grep -q "^${key}=" "$PROD_ENV"; then
    sed -i "/^${key}=/d" "$PROD_ENV"
  fi
done

{
  echo "FIREBASE_PROJECT_ID=$PID"
  echo "FIREBASE_CLIENT_EMAIL=$PEMAIL"
  echo "FIREBASE_PRIVATE_KEY=$PKEY"
} >> "$PROD_ENV"

echo "UPDATED prod Firebase (project=$PID)"

cd /opt/vazifa-prod/backend
set -a
source .env
set +a
echo "VERIFY lens: pid=${#FIREBASE_PROJECT_ID} email=${#FIREBASE_CLIENT_EMAIL} key=${#FIREBASE_PRIVATE_KEY}"

pm2 restart vazifa-api-prod
sleep 3
pm2 logs vazifa-api-prod --lines 30 --nostream 2>/dev/null | grep -iE 'firebase|push|FCM' | tail -10
