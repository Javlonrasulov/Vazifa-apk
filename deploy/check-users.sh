#!/bin/bash
sudo -u postgres psql -d vazifa_prod <<'SQL'
SELECT login, role, "isActive", "passwordPlain", "canAccessAdminPanel"
FROM users
ORDER BY role, login;
SQL

echo "--- admin login test ---"
curl -sk -X POST https://vazifa.liderplast.uz/api/v1/auth/admin/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"admin123"}' | head -c 200
echo

echo "--- director login test ---"
curl -sk -X POST https://vazifa.liderplast.uz/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"director1","password":"director123","deviceId":"diag-test-001"}' | head -c 200
echo
