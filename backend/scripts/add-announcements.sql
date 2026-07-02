-- Xabar (announcement) jadvallari (announcement_status enum run-migrations.sh da yaratiladi)
CREATE TABLE IF NOT EXISTS announcements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title VARCHAR NOT NULL,
  description TEXT,
  "deadlineAt" TIMESTAMPTZ NOT NULL,
  "reminderIntervalMinutes" INT NOT NULL,
  status announcement_status NOT NULL DEFAULT 'active',
  "createdById" UUID NOT NULL REFERENCES users(id),
  "createdAt" TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  "updatedAt" TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS announcement_recipients (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "announcementId" UUID NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
  "recipientId" UUID NOT NULL REFERENCES users(id),
  "acknowledgedAt" TIMESTAMPTZ,
  "lastReminderAt" TIMESTAMPTZ,
  "createdAt" TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE ("announcementId", "recipientId")
);

CREATE TABLE IF NOT EXISTS announcement_attachments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "announcementId" UUID NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
  "uploadedById" UUID NOT NULL REFERENCES users(id),
  "fileName" VARCHAR NOT NULL,
  "filePath" VARCHAR NOT NULL,
  "mimeType" VARCHAR NOT NULL,
  "fileSize" INT NOT NULL DEFAULT 0,
  "createdAt" TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_announcement_recipients_pending
  ON announcement_recipients ("announcementId")
  WHERE "acknowledgedAt" IS NULL;
