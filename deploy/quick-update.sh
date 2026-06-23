#!/bin/bash
set -e
tar xzf /tmp/vazifa-update.tar.gz -C /opt/vazifa-prod
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
