#!/bin/bash
set -e

for env in prod dev; do
  dir="/opt/vazifa-${env}"
  cd "$dir/admin"
  if [ "$env" = prod ]; then
    echo "VITE_API_URL=https://vazifa.liderplast.uz/api/v1" > .env.production
  else
    echo "VITE_API_URL=https://dev.vazifa.liderplast.uz/api/v1" > .env.production
  fi
  npm install
  npm run build
done

cd /opt/vazifa-dev/backend
npm ci
npm run build
set -a && source .env && set +a
npm run seed || true

add_chat() {
  local file="$1"
  local port="$2"
  grep -q 'location /chat/' "$file" && return
  sed -i "/location = \\/docs/i\\
  location /chat/ {\\
    proxy_pass http://127.0.0.1:${port}/chat/;\\
    proxy_http_version 1.1;\\
    proxy_set_header Upgrade \$http_upgrade;\\
    proxy_set_header Connection \"upgrade\";\\
    proxy_set_header Host \$host;\\
    proxy_set_header X-Forwarded-Proto \$scheme;\\
    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;\\
  }\\
" "$file"
}

add_chat /etc/nginx/sites-enabled/vazifa-prod.conf 3105
add_chat /etc/nginx/sites-enabled/vazifa-dev.conf 3106

nginx -t
systemctl reload nginx
pm2 restart vazifa-api-prod vazifa-api-dev
sleep 3
curl -sk -o /dev/null -w "site:%{http_code} api:%{http_code}\n" https://vazifa.liderplast.uz/ https://vazifa.liderplast.uz/api/v1/auth/time
