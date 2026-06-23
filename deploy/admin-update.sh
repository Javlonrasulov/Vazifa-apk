#!/bin/bash
set -e
tar xzf /tmp/vazifa-admin.tar.gz -C /opt/vazifa-prod
rsync -a /opt/vazifa-prod/admin/ /opt/vazifa-dev/admin/
for env in prod dev; do
  dir="/opt/vazifa-${env}/admin"
  cd "$dir"
  if [ "$env" = prod ]; then
    echo "VITE_API_URL=https://vazifa.liderplast.uz/api/v1" > .env.production
  else
    echo "VITE_API_URL=https://dev.vazifa.liderplast.uz/api/v1" > .env.production
  fi
  npm install
  npm run build
done
curl -sk -o /dev/null -w "prod:%{http_code} dev:%{http_code}\n" https://vazifa.liderplast.uz/ https://dev.vazifa.liderplast.uz/
