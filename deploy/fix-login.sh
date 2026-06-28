#!/bin/bash
set -e

echo "=== DB ustunlarini tekshirish ==="
sudo -u postgres psql -d vazifa_prod -c "\d users" | head -40

echo "=== Seed (schema sync) ==="
cd /opt/vazifa-prod/backend
set -a && source .env && set +a
npm run seed

echo "=== Admin parolni tiklash ==="
node -e "
const bcrypt = require('bcrypt');
const { Client } = require('pg');
require('dotenv').config();
(async () => {
  const c = new Client({
    host: process.env.DB_HOST,
    port: +process.env.DB_PORT,
    user: process.env.DB_USERNAME,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_DATABASE,
  });
  await c.connect();
  const hash = await bcrypt.hash('admin123', 10);
  await c.query(
    'UPDATE users SET \"passwordHash\"=\$1, \"passwordPlain\"=\$2 WHERE login=\$3',
    [hash, 'admin123', 'admin']
  );
  console.log('admin paroli: admin123 ga tiklandi');
  await c.end();
})();
"

pm2 restart vazifa-api-prod

sleep 3
echo "=== Login test ==="
curl -sk -X POST https://vazifa.liderplast.uz/api/v1/auth/admin/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"admin123"}' | head -c 150
echo
