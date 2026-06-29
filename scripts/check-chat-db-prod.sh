#!/bin/bash
set -euo pipefail
cd /opt/vazifa-prod/backend
set -a
source .env
set +a
export PGPASSWORD="${DB_PASSWORD}"

echo "=== Oxirgi 10 xabar (javlon-javlo) ==="
psql -h "${DB_HOST}" -U "${DB_USERNAME}" -d "${DB_DATABASE}" -c \
  "SELECT m.id, u1.login AS sender, u2.login AS receiver, m.type, left(coalesce(m.body,''),30) AS body, m.\"createdAt\"
   FROM chat_messages m
   JOIN users u1 ON u1.id = m.\"senderId\"
   JOIN users u2 ON u2.id = m.\"receiverId\"
   WHERE (u1.login IN ('javlon','javlo') AND u2.login IN ('javlon','javlo'))
     AND m.\"isDeleted\" = false
   ORDER BY m.\"createdAt\" DESC LIMIT 10;"

echo "=== Jami xabarlar ==="
psql -h "${DB_HOST}" -U "${DB_USERNAME}" -d "${DB_DATABASE}" -tAc \
  "SELECT count(*) FROM chat_messages m
   JOIN users u1 ON u1.id = m.\"senderId\"
   JOIN users u2 ON u2.id = m.\"receiverId\"
   WHERE u1.login IN ('javlon','javlo') AND u2.login IN ('javlon','javlo') AND m.\"isDeleted\"=false;"

JAVLON=$(psql -h "${DB_HOST}" -U "${DB_USERNAME}" -d "${DB_DATABASE}" -tAc "SELECT id FROM users WHERE login='javlon'" | tr -d '[:space:]')
JAVLO=$(psql -h "${DB_HOST}" -U "${DB_USERNAME}" -d "${DB_DATABASE}" -tAc "SELECT id FROM users WHERE login='javlo'" | tr -d '[:space:]')

TOKEN=$(node -e "
require('dotenv').config();
const jwt=require('jsonwebtoken');
console.log(jwt.sign({sub:'$JAVLON',login:'javlon',role:'employee'}, process.env.JWT_SECRET.trim(), {expiresIn:'5m'}));
")

echo "=== API javlon -> javlo history ==="
curl -sS -w "\nHTTP:%{http_code}\n" "https://vazifa.liderplast.uz/api/v1/chat/${JAVLO}?limit=5" \
  -H "Authorization: Bearer ${TOKEN}" | tail -8

echo "=== PM2 oxirgi chat xatolari ==="
pm2 logs vazifa-api-prod --lines 150 --nostream 2>/dev/null | grep -iE 'chat|401|403|error|history' | tail -15
