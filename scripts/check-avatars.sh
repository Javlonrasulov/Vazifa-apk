#!/bin/bash
set -euo pipefail
cd /opt/vazifa-prod/backend
set -a
source .env
set +a
export PGPASSWORD="${DB_PASSWORD}"
TOKEN=$(node -e "require('dotenv').config(); const jwt=require('jsonwebtoken'); console.log(jwt.sign({sub:'dada6845-ae03-4bf0-a00b-06094e185cd3',login:'javlo',role:'employee'}, process.env.JWT_SECRET.trim(),{expiresIn:'5m'}));")

echo "=== conversations ==="
curl -sS "https://vazifa.liderplast.uz/api/v1/chat/conversations" -H "Authorization: Bearer ${TOKEN}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
for p in d:
    peer=p['peer']
    print(peer.get('fullName'), '| avatar:', peer.get('avatarUrl'))
"

echo "=== contacts (javlon) ==="
curl -sS "https://vazifa.liderplast.uz/api/v1/users/contacts" -H "Authorization: Bearer ${TOKEN}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
for u in d:
    if u.get('login')=='javlon':
        print(u.get('fullName'), '| avatar:', u.get('avatarUrl'))
"
