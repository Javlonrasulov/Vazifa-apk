#!/bin/bash
set -euo pipefail
cd /opt/vazifa-prod/backend
set -a
source .env
set +a
export PGPASSWORD="${DB_PASSWORD}"
TOKEN=$(node -e "require('dotenv').config(); const jwt=require('jsonwebtoken'); console.log(jwt.sign({sub:'ef2e231e-32a5-4e75-b56e-58cc11b3cf52',login:'javlon',role:'employee'}, process.env.JWT_SECRET.trim(),{expiresIn:'5m'}));")
curl -sS -w "\nHTTP:%{http_code}\n" "https://vazifa.liderplast.uz/api/v1/chat/conversations" -H "Authorization: Bearer ${TOKEN}" | python3 -c "
import sys,json
raw=sys.stdin.read().rsplit('HTTP:',1)
body=raw[0].strip(); code=raw[1].strip() if len(raw)>1 else '?'
print('HTTP', code)
d=json.loads(body)
print('count', len(d))
if d: print('first peer', d[0].get('peer',{}).get('fullName')); lm=d[0].get('lastMessage') or {}; print('lastMsg keys', list(lm.keys())[:10])
"
