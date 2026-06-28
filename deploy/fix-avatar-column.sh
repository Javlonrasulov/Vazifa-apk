#!/bin/bash
set -e

sudo -u postgres psql -d vazifa_prod <<'SQL'
ALTER TABLE users ADD COLUMN IF NOT EXISTS "avatarUrl" varchar NULL;
SQL

sudo -u postgres psql -d vazifa_dev <<'SQL'
ALTER TABLE users ADD COLUMN IF NOT EXISTS "avatarUrl" varchar NULL;
SQL

pm2 restart vazifa-api-prod vazifa-api-dev
sleep 3

echo "=== admin login ==="
curl -sk -X POST https://vazifa.liderplast.uz/api/v1/auth/admin/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"admin123"}' | head -c 200
echo

echo "=== javlo app login ==="
curl -sk -X POST https://vazifa.liderplast.uz/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"javlo","password":"123456","deviceId":"fix-test-001","deviceName":"Test"}' | head -c 200
echo
