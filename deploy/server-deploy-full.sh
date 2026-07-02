#!/bin/bash
set -e
grep -q "^PORT=3106" /opt/vazifa-dev/backend/.env || sed -i "s/^PORT=3105/PORT=3106/" /opt/vazifa-dev/backend/.env
bash /tmp/run-migrations.sh
cp /opt/vazifa-prod/backend/.env /tmp/vazifa-prod.env.bak
tar xzf /tmp/vazifa-deploy2.tar.gz -C /opt/vazifa-prod
cp /tmp/vazifa-prod.env.bak /opt/vazifa-prod/backend/.env
if ! grep -q vazifa_app_prod /opt/vazifa-prod/backend/.env; then
  cp /opt/vazifa-prod/backend/.env.bak /opt/vazifa-prod/backend/.env
fi
rsync -a /opt/vazifa-prod/backend/ /opt/vazifa-dev/backend/ --exclude .env
rsync -a /opt/vazifa-prod/admin/ /opt/vazifa-dev/admin/
for env in prod dev; do
  dir="/opt/vazifa-${env}"
  cd "$dir/backend" && npm ci && npm run build
  cd "$dir/admin"
  if [ "$env" = prod ]; then
    echo "VITE_API_URL=https://vazifa.liderplast.uz/api/v1" > .env.production
  else
    echo "VITE_API_URL=https://dev.vazifa.liderplast.uz/api/v1" > .env.production
  fi
  npm install
  chmod -R +x node_modules/.bin 2>/dev/null || true
  npm run build
done
pm2 restart vazifa-api-dev
sleep 4
pm2 restart vazifa-api-prod
sleep 6
grep DB_USERNAME /opt/vazifa-prod/backend/.env
curl -sk -o /dev/null -w "prod:%{http_code} dev:%{http_code} site:%{http_code}\n" \
  https://vazifa.liderplast.uz/api/v1/auth/time \
  https://dev.vazifa.liderplast.uz/api/v1/auth/time \
  https://vazifa.liderplast.uz/
