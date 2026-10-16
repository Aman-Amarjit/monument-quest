# MonumentQuest - Quick Start Guide

## For Local Development

### 1. Start Backend
```bash
cd backend
cp .env.example .env
# Update .env with your PostgreSQL connection
npm install
npx prisma generate
npm run db:push
npm run db:seed
npm run dev
```

Backend runs on http://localhost:3000
Health check: http://localhost:3000/health/live

### 2. Run Android App
```bash
cd /home/aman-amarjit/Desktop/monument/monument-quest
./gradlew assembleDebug
# Install on emulator (auto-connects to 10.0.2.2:3000)
```

## For Production Deployment

### Backend Options
- **Render** (Recommended): Deploy from GitHub, set env vars, auto-build
- **Railway**: Simple, includes PostgreSQL
- **Fly.io**: Global Docker deployment
- **Vercel**: Serverless (needs connection pooling)

See [DEPLOY.md](DEPLOY.md) for detailed steps.

### Android
```bash
./gradlew assembleRelease -PMONUMENTQUEST_API_BASE_URL="https://your-backend.com/api/v1/"
```

## Data Flow Summary

1. **App → Server**: Retrofit sends JSON over HTTP with JWT token
2. **Server → DB**: Prisma ORM executes queries/transactions on PostgreSQL
3. **DB → Server**: Results mapped to API response objects
4. **Server → App**: JSON response parsed to Kotlin data classes
5. **App displays**: UI updated with data

All authentication, validation, and business logic happens on the backend. Database is only accessed via Prisma through the backend API.
