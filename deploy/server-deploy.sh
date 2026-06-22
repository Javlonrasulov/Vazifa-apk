#!/bin/bash
# Faqat Vazifa loyihasi — boshqa loyihalarga tegmaydi
set -euo pipefail

MODE="${1:-prod}" # prod | dev | all

deploy_one() {
  local env="$1"
  local port="$2"
  local domain="$3"
  local db_name="$4"
  local db_user="$5"
  local dir="/opt/vazifa-${env}"
  local pm2_name="vazifa-api-${env}"

  echo "==> Deploy: $env ($domain, port $port)"

  cd "$dir/backend"

  if [ ! -f .env ]; then
    DB_PASS=$(openssl rand -hex 16)
    JWT_ACCESS=$(openssl rand -hex 32)
    JWT_REFRESH=$(openssl rand -hex 32)

    sudo -u postgres psql -v ON_ERROR_STOP=1 <<SQL
CREATE USER ${db_user} WITH PASSWORD '${DB_PASS}';
CREATE DATABASE ${db_name} OWNER ${db_user};
SQL

    cat > .env <<ENV
NODE_ENV=production
PORT=${port}
CORS_ORIGINS=https://${domain}

DB_HOST=127.0.0.1
DB_PORT=5432
DB_USERNAME=${db_user}
DB_PASSWORD=${DB_PASS}
DB_DATABASE=${db_name}

JWT_SECRET=${JWT_ACCESS}
JWT_REFRESH_SECRET=${JWT_REFRESH}
JWT_ACCESS_EXPIRES=15m
JWT_REFRESH_EXPIRES=7d

FIREBASE_PROJECT_ID=
FIREBASE_CLIENT_EMAIL=
FIREBASE_PRIVATE_KEY=

UPLOAD_DIR=${dir}/backend/uploads
ENV
    chmod 600 .env
  fi

  npm ci
  npm run build
  mkdir -p uploads

  npm run seed || true

  cd "$dir/admin"
  echo "VITE_API_URL=https://${domain}/api/v1" > .env.production
  npm ci
  npm run build

  if pm2 describe "$pm2_name" >/dev/null 2>&1; then
    pm2 restart "$pm2_name"
  else
    pm2 start "$dir/backend/dist/src/main.js" --name "$pm2_name" --cwd "$dir/backend"
    pm2 save
  fi
}

case "$MODE" in
  prod) deploy_one prod 3105 vazifa.liderplast.uz vazifa_prod vazifa_app_prod ;;
  dev)  deploy_one dev 3106 dev.vazifa.liderplast.uz vazifa_dev vazifa_app_dev ;;
  all)
    deploy_one prod 3105 vazifa.liderplast.uz vazifa_prod vazifa_app_prod
    deploy_one dev 3106 dev.vazifa.liderplast.uz vazifa_dev vazifa_app_dev
    ;;
  *) echo "Usage: $0 [prod|dev|all]"; exit 1 ;;
esac

# Nginx — faqat yangi fayllar
cp /opt/vazifa-prod/deploy/nginx-vazifa-prod.conf /etc/nginx/sites-available/vazifa-prod.conf
ln -sf /etc/nginx/sites-available/vazifa-prod.conf /etc/nginx/sites-enabled/vazifa-prod.conf

if [ -d /opt/vazifa-dev ]; then
  cp /opt/vazifa-dev/deploy/nginx-vazifa-dev.conf /etc/nginx/sites-available/vazifa-dev.conf
  ln -sf /etc/nginx/sites-available/vazifa-dev.conf /etc/nginx/sites-enabled/vazifa-dev.conf
fi

nginx -t
systemctl reload nginx

echo "==> Tayyor: $MODE"
