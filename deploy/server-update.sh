#!/bin/bash
set -e

tar xzf /tmp/vazifa-update.tar.gz -C /opt/vazifa-prod
rsync -a /opt/vazifa-prod/backend/ /opt/vazifa-dev/backend/ --exclude .env
rsync -a /opt/vazifa-prod/admin/ /opt/vazifa-dev/admin/

for d in prod dev; do
  dir="/opt/vazifa-$d"
  cd "$dir/backend"
  npm ci
  npm run build

  cd "$dir/admin"
  if [ "$d" = prod ]; then
    echo "VITE_API_URL=https://vazifa.liderplast.uz/api/v1" > .env.production
  else
    echo "VITE_API_URL=https://dev.vazifa.liderplast.uz/api/v1" > .env.production
  fi
  npm install
  npm run build
done

pm2 restart vazifa-api-prod vazifa-api-dev

for f in /etc/nginx/sites-enabled/vazifa-prod.conf /etc/nginx/sites-enabled/vazifa-dev.conf; do
  if ! grep -q 'location = /docs' "$f"; then
    sed -i '/location \/ {/i\  location = /docs {\n    return 404;\n  }\n' "$f"
  fi
done

nginx -t
systemctl reload nginx

echo -n "site: "; curl -sk -o /dev/null -w "%{http_code} " https://vazifa.liderplast.uz/
echo -n "docs: "; curl -sk -o /dev/null -w "%{http_code} " https://vazifa.liderplast.uz/docs
echo -n "api: "; curl -sk -o /dev/null -w "%{http_code}\n" https://vazifa.liderplast.uz/api/v1/auth/time
