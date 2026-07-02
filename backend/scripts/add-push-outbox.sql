-- Push navbat jadvali (offline qurilmalarga qayta yuborish uchun)
CREATE TABLE IF NOT EXISTS push_outbox (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "userId" uuid NOT NULL,
  title varchar NOT NULL,
  body text NOT NULL,
  data jsonb NOT NULL DEFAULT '{}',
  status varchar(16) NOT NULL DEFAULT 'pending',
  attempts int NOT NULL DEFAULT 0,
  "nextRetryAt" timestamptz NOT NULL DEFAULT NOW(),
  "sentAt" timestamptz NULL,
  "createdAt" timestamptz NOT NULL DEFAULT NOW(),
  "updatedAt" timestamptz NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_push_outbox_user_status ON push_outbox ("userId", status);
CREATE INDEX IF NOT EXISTS idx_push_outbox_status_retry ON push_outbox (status, "nextRetryAt");
