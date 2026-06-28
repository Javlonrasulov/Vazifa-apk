#!/bin/bash
set -e
cp /opt/vazifa-prod/backend/.env /tmp/vazifa-prod.env.bak
tar xzf /tmp/vazifa-backend.tar.gz -C /opt/vazifa-prod
cp /tmp/vazifa-prod.env.bak /opt/vazifa-prod/backend/.env
rsync -a /opt/vazifa-prod/backend/ /opt/vazifa-dev/backend/ --exclude .env
for env in prod dev; do
  cd "/opt/vazifa-${env}/backend"
  npm ci
  npm run build
done
pm2 restart vazifa-api-prod vazifa-api-dev
curl -sk https://vazifa.liderplast.uz/api/v1/auth/time
