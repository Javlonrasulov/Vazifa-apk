#!/bin/bash
set -euo pipefail
cd /opt/vazifa-prod/backend

echo "=== dotenv Firebase ==="
node << 'NODE'
require('dotenv').config();
const p = process.env.FIREBASE_PROJECT_ID || '';
const e = process.env.FIREBASE_CLIENT_EMAIL || '';
const k = process.env.FIREBASE_PRIVATE_KEY || '';
console.log('project', p, 'len', p.length);
console.log('email len', e.length, 'key len', k.length);
NODE

set -a
source .env
set +a
export PGPASSWORD="${DB_PASSWORD}"

echo "=== FCM tokens ==="
psql -h "${DB_HOST}" -U "${DB_USERNAME}" -d "${DB_DATABASE}" -c \
  "SELECT login, length(\"fcmToken\") AS token_len, \"notificationsEnabled\" FROM users WHERE login IN ('javlon','javlo') ORDER BY login;"

echo "=== PM2 Firebase log ==="
pm2 logs vazifa-api-prod --lines 300 --nostream 2>/dev/null | grep NotificationsService | tail -3

FCM=$(psql -h "${DB_HOST}" -U "${DB_USERNAME}" -d "${DB_DATABASE}" -tAc \
  "SELECT \"fcmToken\" FROM users WHERE login='javlo' AND \"fcmToken\" IS NOT NULL AND \"notificationsEnabled\"=true LIMIT 1" | tr -d '[:space:]')

if [ -n "$FCM" ] && [ "${#FCM}" -gt 20 ]; then
  echo "=== Test push to javlo (token len ${#FCM}) ==="
  TEST_FCM="$FCM" node << 'NODE'
require('dotenv').config();
const admin = require('firebase-admin');
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: process.env.FIREBASE_PROJECT_ID,
      clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
      privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n'),
    }),
  });
}
admin.messaging().send({
  token: process.env.TEST_FCM,
  notification: { title: 'Vazifa', body: 'Push test — ishlayapti' },
  data: { type: 'chat', title: 'Vazifa', body: 'Push test' },
  android: { priority: 'high' },
}).then(id => console.log('TEST_PUSH_OK', id)).catch(e => console.log('TEST_PUSH_ERR', e.code || e.message));
NODE
else
  echo "NO_JAVLO_TOKEN — ilovadan qayta kiring"
fi
