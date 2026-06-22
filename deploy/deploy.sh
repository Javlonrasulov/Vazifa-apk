#!/bin/bash
# Serverda ishga tushirish: bash deploy/deploy.sh
set -euo pipefail

APP_DIR="/var/www/vazifa"
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> Loyiha papkasi: $APP_DIR"

sudo mkdir -p "$APP_DIR"
sudo rsync -a --delete \
  --exclude node_modules \
  --exclude .git \
  --exclude android-app \
  --exclude 'backend/uploads' \
  --exclude 'backend/.env' \
  "$REPO_DIR/" "$APP_DIR/"

cd "$APP_DIR"

# PostgreSQL (Docker)
if [ ! -f deploy/.db.env ]; then
  echo "deploy/.db.env yarating (DB_PASSWORD=...)"
  exit 1
fi
set -a && source deploy/.db.env && set +a
docker compose -f deploy/docker-compose.prod.yml up -d

# Backend
cd "$APP_DIR/backend"
if [ ! -f .env ]; then
  cp ../deploy/backend.env.example .env
  echo ".env ni tahrirlang va qayta ishga tushiring"
  exit 1
fi
npm ci
npm run build
sudo mkdir -p uploads
sudo chown -R www-data:www-data uploads

# Birinchi marta: jadval + demo foydalanuvchilar
if ! npm run seed 2>/dev/null; then
  echo "Seed allaqachon bajarilgan yoki xato — davom etamiz"
fi

# Admin panel (static)
cd "$APP_DIR/admin"
cp ../deploy/admin.env.production .env.production
npm ci
npm run build
sudo chown -R www-data:www-data dist

# systemd
sudo cp "$APP_DIR/deploy/vazifa-api.service" /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable vazifa-api
sudo systemctl restart vazifa-api

# nginx
sudo cp "$APP_DIR/deploy/nginx-vazifa.conf" /etc/nginx/sites-available/vazifa.liderplast.uz
sudo ln -sf /etc/nginx/sites-available/vazifa.liderplast.uz /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

echo ""
echo "Tayyor! Keyingi qadam: certbot --nginx -d vazifa.liderplast.uz"
echo "Admin: https://vazifa.liderplast.uz"
