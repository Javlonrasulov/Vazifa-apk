-- Shaxsiy kontakt aliaslari (har foydalanuvchi o'z nomini belgilaydi)
CREATE TABLE IF NOT EXISTS chat_contact_aliases (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "ownerId" UUID NOT NULL,
  "peerId" UUID NOT NULL,
  alias VARCHAR(120) NOT NULL,
  "updatedAt" TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE ("ownerId", "peerId")
);

CREATE INDEX IF NOT EXISTS idx_chat_contact_aliases_owner ON chat_contact_aliases ("ownerId");
