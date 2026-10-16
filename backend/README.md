# MonumentQuest Backend API

Production API for MonumentQuest heritage discovery platform.

## Tech Stack
- Node.js + Express + TypeScript
- PostgreSQL + Prisma ORM
- JWT Authentication
- OTP via Nodemailer (Gmail)

## Quick Start

```bash
# Install dependencies
npm install

# Setup environment
cp .env.example .env
# Edit .env with your database credentials

# Setup database
npx prisma generate
npm run db:push
npm run db:seed

# Start development server
npm run dev
```

## Scripts
- `npm run dev` - Development server with hot reload
- `npm run build` - Build TypeScript to dist/ (also runs prisma generate)
- `npm run start` - Start production server
- `npm run db:push` - Push schema changes to database
- `npm run db:seed` - Seed database with monuments
- `npm run db:migrate` - Run migrations in production

## API Endpoints
Base URL: `/api/v1`

- Auth: `POST /auth/send-otp`, `POST /auth/login-with-otp`, `POST /auth/register-with-otp`, `POST /auth/guest`, `GET /auth/me`
- Monuments: `GET /monuments`, `GET /monuments/nearby`, `GET /monuments/:id`, `POST /monuments/capture`
- Hotels: `GET /hotels`, `POST /hotels/claim-voucher`
- Feed: `GET /feed`, `POST /feed/posts`, `DELETE /feed/posts/:id`, `POST /feed/posts/:id/like`, `GET/POST /feed/posts/:id/comments`
- Social: Leaderboards, guilds, stories, follows, saves, reports
- AI: `POST /ai/narrator`, `POST /ai/verify-reflection`

See `openapi.yaml` for full API specification.

## Health Checks
- `/health/live` - Liveness probe
- `/health/ready` - Readiness probe (checks DB)
- `/api/v1/health` - Detailed health info
