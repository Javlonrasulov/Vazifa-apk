-- Xodimning dam olish vaqti va kunlari (production uchun qo'lda ishga tushiriladi)
ALTER TABLE users ADD COLUMN IF NOT EXISTS "restStart" varchar NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS "restEnd" varchar NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS "restDays" text NULL;
