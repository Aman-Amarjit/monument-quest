# MonumentQuest Deployment Guide

## Overview
MonumentQuest has two components:
1. **Backend API** (Node.js + Express + TypeScript + Prisma + PostgreSQL)
2. **Android App** (Jetpack Compose + Kotlin)

## Backend Deployment

### Prerequisites
- PostgreSQL database (Render, Neon, Supabase, Railway, etc.)
- Node.js 20+

### Environment Variables
Copy `backend/.env.example` to `.env` and configure:

```env
DATABASE_URL="postgresql://user:password@host:port/dbname"
DIRECT_URL="postgresql://user:password@host:port/dbname"
PORT=3000
NODE_ENV=production
TRUST_PROXY=true
CORS_ORIGIN=*  # or your frontend domain
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters
JWT_EXPIRES_IN=7d
GMAIL_USER=your-email@gmail.com  # Optional for OTP
GMAIL_APP_PASSWORD=your-app-password  # Optional for OTP
```

### Render.com (Recommended)
1. Create new Web Service from GitHub repo
2. Root Directory: `backend`
3. Build Command: `npm run build`
4. Start Command: `npm run start`
5. Add environment variables from above
6. Render will auto-run Prisma generate (via postinstall)

### Railway
1. Create new project from GitHub
2. Select backend directory
3. Add PostgreSQL service
4. Set environment variables
5. Deploy

### Vercel
1. Import project to Vercel
2. Framework Preset: Other
3. Root Directory: `backend` (or use root vercel.json)
4. Build Command: `prisma generate && tsc`
5. Install Command: `npm ci`
6. Add environment variables
7. Note: Vercel serverless works with PostgreSQL + connection pooling (PgBouncer)

### Database Setup
After deploying, run:
```bash
npx prisma db push
npx prisma db seed
```

## Android App Deployment

### Configure API URL
Create `local.properties` or pass gradle property:
```bash
./gradlew assembleRelease -PMONUMENTQUEST_API_BASE_URL="https://your-backend-url.com/api/v1/"
```

### Build Release APK
```bash
cd /home/aman-amarjit/Desktop/monument/monument-quest
./gradlew assembleRelease
```
APK will be at: `app/build/outputs/apk/release/`

### Build Release AAB (for Play Store)
```bash
./gradlew bundleRelease
```
AAB will be at: `app/build/outputs/bundle/release/`

### Generate Signing Key (for Play Store)
```bash
keytool -genkey -v -keystore release.keystore -alias monumentquest -keyalg RSA -keysize 2048 -validity 10000
```
Configure signing in `app/build.gradle.kts` if needed.

## API Health Check
Once backend is deployed, verify:
- `GET https://your-backend-url.com/health/live` → `{"status":"ok"}`
- `GET https://your-backend-url.com/health/ready` → `{"status":"ready","database":"ok"}`
- `GET https://your-backend-url.com/api/v1/health` → `{"status":"online",...}`

## Local Development

### Backend
```bash
cd backend
cp .env.example .env  # Update with local DB
npm install
npx prisma generate
npm run db:push
npm run db:seed
npm run dev
```

### Android
```bash
./gradlew assembleDebug  # Uses 10.0.2.2:3000 for emulator
```
