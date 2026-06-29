#!/bin/bash
set -e
cp /opt/vazifa-prod/backend/.env /tmp/vazifa-prod.env.bak
tar xzf /tmp/vazifa-update.tar.gz -C /opt/vazifa-prod 2>/dev/null || tar xzf /tmp/vazifa-deploy2.tar.gz -C /opt/vazifa-prod
cp /tmp/vazifa-prod.env.bak /opt/vazifa-prod/backend/.env
# Agar backup noto'g'ri bo'lsa (local .env arxivdan kelgan) — .env.bak dan tikla
if ! grep -q 'vazifa_app_prod' /opt/vazifa-prod/backend/.env 2>/dev/null; then
  cp /opt/vazifa-prod/backend/.env.bak /opt/vazifa-prod/backend/.env
fi
rsync -a /opt/vazifa-prod/backend/ /opt/vazifa-dev/backend/ --exclude .env
rsync -a /opt/vazifa-prod/admin/ /opt/vazifa-dev/admin/

for env in prod dev; do
  dir="/opt/vazifa-${env}"
  cd "$dir/backend"
  npm ci
  npm run build
  cd "$dir/admin"
  if [ "$env" = prod ]; then
    echo "VITE_API_URL=https://vazifa.liderplast.uz/api/v1" > .env.production
  else
    echo "VITE_API_URL=https://dev.vazifa.liderplast.uz/api/v1" > .env.production
  fi
  npm install
  npm run build
done

pm2 restart vazifa-api-prod vazifa-api-dev
sleep 2
curl -sk -o /dev/null -w "site:%{http_code} api:%{http_code}\n" https://vazifa.liderplast.uz/ https://vazifa.liderplast.uz/api/v1/auth/time
