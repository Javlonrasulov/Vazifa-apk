# Vazifa — serverga joylash (vazifa.liderplast.uz)

**Server:** `89.39.94.188` · Ubuntu 24.04 · 1 GB RAM

Uchta loyiha allaqachon ishlayapti — Vazifa uchun alohida port (`3040`) va alohida PostgreSQL (`5433`) ishlatiladi.

---

## 1. DNS (domen paneli)

DNS-menedjerda **«Добавить запись»** → yangi **A** yozuvi:

| Maydon | Qiymat |
|--------|--------|
| Turi | A |
| Nomi | `vazifa` |
| IP | `89.39.94.188` |
| TTL | 14400 |

Natija: `vazifa.liderplast.uz` → server IP.

Tekshirish (5–30 daqiqadan keyin):

```bash
ping vazifa.liderplast.uz
```

---

## 2. Serverga ulanish

SSH kalit yoki konsol orqali:

```bash
ssh root@89.39.94.188
# yoki paneldagi «Консоль»
```

Kerakli dasturlar (agar yo'q bo'lsa):

```bash
sudo apt update
sudo apt install -y nginx docker.io docker-compose-plugin git rsync
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
sudo systemctl enable docker nginx
```

---

## 3. Loyihani serverga yuklash

**Variant A — Git (tavsiya):**

```bash
sudo mkdir -p /var/www/vazifa
sudo git clone <SIZNING_REPO_URL> /var/www/vazifa
sudo chown -R $USER:$USER /var/www/vazifa
```

**Variant B — Kompyuterdan SCP (Windows PowerShell):**

```powershell
scp -r C:\Users\javlo\Desktop\vazifa root@89.39.94.188:/var/www/
```

`node_modules` va `.git` ni yuklamaslik yaxshiroq — serverda `npm ci` ishlaydi.

---

## 4. Sozlash va ishga tushirish

```bash
cd /var/www/vazifa

# DB paroli
echo 'DB_PASSWORD=KUCHLI_PAROL_BU_YERGA' > deploy/.db.env

# Backend .env
cp deploy/backend.env.example backend/.env
nano backend/.env   # JWT va DB parollarini o'zgartiring

# Deploy skripti
chmod +x deploy/deploy.sh
bash deploy/deploy.sh
```

**Birinchi marta** `.env` bo'lmasa skript to'xtaydi — `backend/.env` ni to'ldiring va qayta `bash deploy/deploy.sh`.

---

## 5. SSL (HTTPS)

Boshqa loyihalarda Certbot bo'lsa, xuddi shu usul:

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d vazifa.liderplast.uz
```

Keyin `backend/.env` da `CORS_ORIGINS=https://vazifa.liderplast.uz` ekanligini tekshiring va:

```bash
sudo systemctl restart vazifa-api
```

---

## 6. Tekshirish

| Xizmat | URL |
|--------|-----|
| Admin panel | https://vazifa.liderplast.uz |
| API | https://vazifa.liderplast.uz/api/v1 |

**Seed loginlar** (birinchi `npm run seed` dan keyin):

- Admin: `admin` / `admin123`
- Direktor: `director1` / `director123`

Productionda parollarni darhol o'zgartiring.

---

## 7. Android APK (production)

`android-app/local.properties`:

```properties
api.scheme=https
api.host=vazifa.liderplast.uz
api.port=
```

Keyin release APK yig'ing va xodimlarga tarqating.

---

## 8. Yangilash (keyingi versiyalar)

```bash
cd /var/www/vazifa
git pull
bash deploy/deploy.sh
```

---

## 9. Foydali buyruqlar

```bash
sudo systemctl status vazifa-api
sudo journalctl -u vazifa-api -f
docker ps | grep vazifa
sudo nginx -t
```

---

## Portlar (konflikt bo'lmasligi uchun)

| Xizmat | Port |
|--------|------|
| Vazifa API | `127.0.0.1:3040` |
| Vazifa PostgreSQL | `127.0.0.1:5433` |
| Nginx | `80` / `443` (umumiy) |

Agar `3040` yoki `5433` band bo'lsa, `backend/.env` va `deploy/docker-compose.prod.yml` dagi portlarni o'zgartiring.
