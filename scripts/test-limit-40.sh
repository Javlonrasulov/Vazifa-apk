#!/bin/bash
set -euo pipefail
cd /opt/vazifa-prod/backend
set -a
source .env
set +a
export PGPASSWORD="${DB_PASSWORD}"
JAVLO=$(psql -h "${DB_HOST}" -U "${DB_USERNAME}" -d "${DB_DATABASE}" -tAc "SELECT id FROM users WHERE login='javlo'" | tr -d '[:space:]')
TOKEN=$(node -e "require('dotenv').config(); const jwt=require('jsonwebtoken'); console.log(jwt.sign({sub:'ef2e231e-32a5-4e75-b56e-58cc11b3cf52',login:'javlon',role:'employee'}, process.env.JWT_SECRET.trim(),{expiresIn:'5m'}));")

echo "=== limit=40 (Android) ==="
curl -sS -o /tmp/h1.json -w "HTTP:%{http_code}\n" "https://vazifa.liderplast.uz/api/v1/chat/${JAVLO}?limit=40" -H "Authorization: Bearer ${TOKEN}"
head -c 120 /tmp/h1.json; echo

echo "=== no limit ==="
curl -sS -o /tmp/h2.json -w "HTTP:%{http_code}\n" "https://vazifa.liderplast.uz/api/v1/chat/${JAVLO}" -H "Authorization: Bearer ${TOKEN}"
python3 -c "import json; d=json.load(open('/tmp/h2.json')); print('count', len(d) if isinstance(d,list) else d)"
