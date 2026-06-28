#!/bin/bash
set -euo pipefail
cd /opt/vazifa-prod/backend
set -a
source .env
set +a

export PGPASSWORD="${DB_PASSWORD}"
PSQL=(psql -h "${DB_HOST}" -U "${DB_USERNAME}" -d "${DB_DATABASE}" -tAc)

JAVLON=$("${PSQL[@]}" "SELECT id FROM users WHERE login='javlon' LIMIT 1" | tr -d '[:space:]')
JAVLO=$("${PSQL[@]}" "SELECT id FROM users WHERE login='javlo' LIMIT 1" | tr -d '[:space:]')
MSG_COUNT=$("${PSQL[@]}" "SELECT count(*) FROM chat_messages WHERE ((\"senderId\"='$JAVLON' AND \"receiverId\"='$JAVLO') OR (\"senderId\"='$JAVLO' AND \"receiverId\"='$JAVLON')) AND \"isDeleted\"=false")
echo "JAVLON=$JAVLON"
echo "JAVLO=$JAVLO"
echo "DB_MSG_COUNT=$MSG_COUNT"

PORT="${PORT:-3000}"
LOGIN_RESP=$(curl -sS -X POST "http://127.0.0.1:${PORT}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"login":"javlon","password":"123456","deviceId":"verify-script"}')
TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken','')); import sys as s; s.exit(0 if d.get('accessToken') else 1)" 2>/dev/null || echo "")
if [ -z "$TOKEN" ]; then
  echo "LOGIN_FAIL=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('message',''))" 2>/dev/null || echo "$LOGIN_RESP")"
  TOKEN=$(node -e "
    require('dotenv').config();
    const jwt = require('jsonwebtoken');
    console.log(jwt.sign(
      { sub: '$JAVLON', login: 'javlon', role: 'employee' },
      process.env.JWT_SECRET,
      { expiresIn: '5m' }
    ));
  ")
  echo "JWT_FALLBACK_LEN=${#TOKEN}"
fi

HIST=$(curl -sS -w "\nHTTP:%{http_code}" "http://127.0.0.1:${PORT}/api/v1/chat/${JAVLO}" \
  -H "Authorization: Bearer ${TOKEN}")
HTTP=$(echo "$HIST" | tail -1)
BODY=$(echo "$HIST" | sed '$d')
COUNT=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d) if isinstance(d,list) else 'NOT_ARRAY')")
echo "LOCAL_HISTORY $HTTP count=$COUNT"

echo "$BODY" | python3 -c "
import sys, json
d = json.load(sys.stdin)
if not d:
    print('EMPTY')
    sys.exit(0)
m = d[0]
print('sample_keys:', sorted(m.keys()))
print('has_nested_sender:', isinstance(m.get('sender'), dict))
rt = m.get('replyTo')
if rt:
    print('replyTo_keys:', sorted(rt.keys()) if isinstance(rt, dict) else type(rt).__name__)
"

PUBLIC=$(curl -sS -w "\nHTTP:%{http_code}" "https://vazifa.liderplast.uz/api/v1/chat/${JAVLO}" \
  -H "Authorization: Bearer ${TOKEN}")
PHTTP=$(echo "$PUBLIC" | tail -1)
PBODY=$(echo "$PUBLIC" | sed '$d')
PCOUNT=$(echo "$PBODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d) if isinstance(d,list) else 'NOT_ARRAY')")
echo "PUBLIC_HISTORY $PHTTP count=$PCOUNT"

CONV=$(curl -sS -w "\nHTTP:%{http_code}" "https://vazifa.liderplast.uz/api/v1/chat/conversations" \
  -H "Authorization: Bearer ${TOKEN}")
CHTTP=$(echo "$CONV" | tail -1)
CBODY=$(echo "$CONV" | sed '$d')
echo "CONVERSATIONS $CHTTP"
echo "$CBODY" | python3 -c "
import sys, json
d = json.load(sys.stdin)
for c in d:
    p = c.get('peer') or {}
    if p.get('id') == '$JAVLO':
        lm = c.get('lastMessage') or {}
        print('FOUND_JAVLO lastMsgId=', lm.get('id'), 'body=', (lm.get('body') or '')[:40])
        print('lastMsg_keys=', sorted(lm.keys()) if lm else [])
        break
else:
    print('JAVLO_NOT_IN_CONVERSATIONS peers=', [ (c.get('peer') or {}).get('fullName') for c in d[:5] ])
"

grep -c toHistoryPayload /opt/vazifa-prod/backend/dist/src/chat/chat.service.js || true
