#!/bin/bash
set -euo pipefail
ENV=/opt/vazifa-prod/backend/.env
sed -i 's/\r$//' "$ENV"
cd /opt/vazifa-prod/backend
pm2 restart vazifa-api-prod
sleep 4
pm2 logs vazifa-api-prod --lines 20 --nostream 2>/dev/null | grep NotificationsService | tail -2

set -a
source .env
set +a
export PGPASSWORD="${DB_PASSWORD}"
FCM=$(psql -h "${DB_HOST}" -U "${DB_USERNAME}" -d "${DB_DATABASE}" -tAc \
  "SELECT \"fcmToken\" FROM users WHERE login='javlo' LIMIT 1" | tr -d '[:space:]')

node -e "
require('dotenv').config();
const admin = require('firebase-admin');
const pid = (process.env.FIREBASE_PROJECT_ID || '').trim();
const email = (process.env.FIREBASE_CLIENT_EMAIL || '').trim();
const key = (process.env.FIREBASE_PRIVATE_KEY || '').replace(/\\\\n/g, '\n');
console.log('project', JSON.stringify(pid));
admin.initializeApp({ credential: admin.credential.cert({ projectId: pid, clientEmail: email, privateKey: key }) });
admin.messaging().send({
  token: process.argv[1],
  notification: { title: 'Vazifa', body: 'Push test OK' },
  data: { type: 'chat', title: 'Vazifa', body: 'Push test OK' },
  android: { priority: 'high' },
}).then(r => console.log('SENT', r)).catch(e => console.log('ERR', e.code, e.message));
" "$FCM"
