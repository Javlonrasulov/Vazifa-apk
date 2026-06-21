# Vazifa — Xodimlar Vazifalarini Boshqarish Tizimi

Korxona ichida vazifalar berish, nazorat qilish va avtomatik eslatmalar.

## Tuzilma

```
vazifa/
├── backend/       NestJS API (PostgreSQL, JWT, FCM, WebSocket)
├── admin/         Web panel — faqat xodimlar boshqaruvi
├── android-app/   Bitta APK — direktor va xodim rollari
└── docker-compose.yml
```

## Rol taqsimoti

| Rol | Platforma | Vazifa |
|-----|-----------|--------|
| **Admin** | Web panel | Xodim/direktor qo'shish, login, parol, qurilma |
| **Direktor** | Android ilova | Vazifa yaratish, kuzatish, dashboard |
| **Xodim** | Android ilova | Vazifalarni bajarish, hisobot |

## Ishga tushirish

### 1. Database

```powershell
cd c:\Users\javlo\Desktop\vazifa
docker compose up -d
```

### 2. Backend

```powershell
cd backend
copy .env.example .env
npm install
npm run start:dev
```

Boshqa terminalda seed:

```powershell
cd backend
npm run seed
```

**Loginlar (seed):**
- Admin panel: `admin` / `admin123`
- Direktor (ilova): `director1` / `director123`
- Xodim (ilova): `xodim1` / `xodim123`

API: http://localhost:3000/api/v1  
Swagger: http://localhost:3000/docs

### 3. Admin Panel

```powershell
cd admin
copy .env.example .env
npm install
npm run dev
```

http://localhost:5173

### 4. Android APK

1. Android Studio'da `android-app/` oching
2. `local.properties` yarating: `api.host=10.0.2.2` (emulator) yoki kompyuter IP
3. Firebase `google-services.json` ni haqiqiy loyiha bilan almashtiring
4. Run yoki `./gradlew assembleDebug`

## Dizayn

- **Liquid Glass** uslubi (Lider Navoiy client APK asosida)
- Asosiy rang: `#2563EB`
- Dark / Light mode

## Muhim qoidalar

- **Push notification majburiy** — ruxsat yo'q bo'lsa ilova bloklanadi
- **Device binding** — yangi telefon admin tasdig'ini talab qiladi
- **Vaqt** — Toshkent (`Asia/Tashkent`), serverdan onlayn olinadi

## API modullar

| Modul | Endpoint |
|-------|----------|
| Auth | POST /auth/login, /auth/admin/login, /auth/fcm |
| Users | CRUD (admin), contacts (mobile) |
| Tasks | CRUD, status, fayl, izoh |
| Chat | Xabarlar, fayl |
| Audit | Faollik loglari |
| Dashboard | Direktor statistikasi |

## Firebase (Push)

Backend `.env` ga Firebase Admin SDK kalitlarini qo'shing.  
Android `google-services.json` ni Firebase Console'dan yuklab oling.
