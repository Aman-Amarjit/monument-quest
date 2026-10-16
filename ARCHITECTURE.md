# MonumentQuest - Data Flow Architecture

## Overview
This document explains how data flows between the Android app, backend server, and database.

## Data Flow Diagram

```
Android App (Kotlin/Jetpack Compose)
           ↓ (Retrofit + OkHttp)
Backend API (Node.js/Express/TypeScript)
           ↓ (Prisma ORM)
PostgreSQL Database
```

## 1. Frontend → Backend

### Communication
- **Protocol**: HTTP/HTTPS
- **Client**: Retrofit 2.9.0 with OkHttp
- **Authentication**: JWT Bearer tokens in `Authorization` header
- **Request Body**: JSON (automatic serialization via Gson)
- **Timeouts**: 30s connect/read/write, retry on connection failure

### Example Flow - Monument Capture
1. User clicks "Capture" in app
2. App gets current GPS coordinates (FusedLocationProvider)
3. Creates `CaptureRequest(latitude, longitude, monumentId?)`
4. Sends POST to `${API_BASE_URL}/monuments/capture` with Bearer token
5. Backend validates token via `AuthInterceptor` (frontend) → `authenticateToken` (backend)
6. Backend validates request (Zod/schema validation)
7. Backend computes distance using Haversine formula
8. If within 100m, creates Discovery in transaction, awards XP
9. Returns `CaptureResponse` with result

### Request Headers
```http
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

### API Endpoints (Android → Backend)
| Operation | Method | Endpoint | Auth Required |
|-----------|--------|----------|---------------|
| Send OTP | POST | /api/v1/auth/send-otp | No |
| Login/Register with OTP | POST | /api/v1/auth/login-with-otp, /api/v1/auth/register-with-otp | No |
| Guest Login | POST | /api/v1/auth/guest | No |
| Get Profile | GET | /api/v1/auth/me, /api/v1/user/profile | Yes |
| Sync Progress | PATCH | /api/v1/user/progress | Yes |
| Get Monuments | GET | /api/v1/monuments?lat=...&lon=... | No |
| Capture Monument | POST | /api/v1/monuments/capture | Yes |
| Get Feed | GET | /api/v1/feed | Optional |
| Create Post | POST | /api/v1/feed/posts | Yes (Registered) |
| Like/Comment | POST | /api/v1/feed/posts/:id/like, /api/v1/feed/posts/:id/comments | Yes |
| Leaderboard/Guilds | GET | /api/v1/leaderboard, /api/v1/guilds | No |

## 2. Backend → Database

### ORM
- **Prisma Client 5.x** - Type-safe database access
- **Connection Management**: Singleton pattern (prevents multiple instances in dev)
- **Transactions**: Used for critical operations (capture, guild updates)
- **Migrations**: `prisma db push` (schema sync) or `prisma migrate deploy`

### Database Models
Core entities: User, Monument, Discovery, Post, PostLike, Comment, Guild, Follow, SavedPost, Story, StoryView, PostReport, TimeCapsule, RateLimitBucket

### Example: Capture Flow
```typescript
prisma.$transaction(async (tx) => {
  const existing = await tx.discovery.findUnique({...});
  if (existing) return { alreadyCaptured: true };
  const previousUploads = await tx.discovery.count({...});
  const reward = rarity(previousUploads);
  await tx.discovery.create({...});
  await tx.user.update({ data: { points: { increment: reward.points } }});
  await tx.monument.update({ data: { totalContributionPoints: { increment: reward.points } }});
  if (guildId) await tx.guild.update({...});
  return { success: true, pointsEarned: reward.points, ...reward };
});
```

### Connection String
Uses `DATABASE_URL` from env. For serverless (Vercel), should use pooled connection (PgBouncer) with `?pgbouncer=true&connection_limit=1`.

## 3. Backend → Frontend (Response)

### Response Format
All successful responses follow consistent structure:
```json
{
  "success": true,
  "data": {...},
  "count": N (for lists),
  "pagination": { "nextCursor": "..." }
}
```

Error responses:
```json
{
  "success": false,
  "error": "Error message description"
}
```

### Error Handling
- Global error middleware catches all errors
- Proper HTTP status codes (400, 401, 403, 404, 409, 429, 500)
- Stack traces hidden in production
- CORS headers properly set

## 4. Android Data Handling

### Data Models
Defined as Kotlin data classes (Gson serializable) in `data/model/` and API request/response classes in `MonumentApi.kt`.

### Repository Pattern
ViewModels → Repositories → API Service (Retrofit) for separation of concerns.

### Authentication
- `TokenManager` stores JWT in encrypted SharedPreferences (AndroidX Security Crypto)
- `AuthInterceptor` automatically adds token to all API requests
- Token refreshed implicitly on 401 responses where applicable

### Image Upload
Images uploaded to Cloudinary via unsigned uploads (separate from backend). URLs returned and stored in backend posts.

## 5. Key Data Transformations

### Backend API Response Flexibility
Backend returns both `data` and alternative keys (e.g., `monuments`, `feed`) for compatibility:
```typescript
res.json({ success: true, count: data.length, data, monuments: data });
```

### Android API Parsing
Some endpoints return wrapped responses - app handles both cases where needed.

## 6. Security

- **CORS**: Configurable via `CORS_ORIGIN` (supports comma-separated or `*`)
- **Helmet**: Security headers (XSS, CSP, etc.)
- **Rate Limiting**: Per-endpoint limits (auth: 20/min, social: 60/min, AI: 60/min)
- **Input Sanitization**: Text fields sanitized (HTML stripped, XSS prevention)
- **JWT**: Signed with HS256, configurable expiry
- **Password Hashing**: bcrypt for stored passwords (even for guests)
- **Geo Validation**: Coordinates validated (-90/90, -180/180), capture radius enforced

## 7. Deployment Considerations

### Backend
- Use PostgreSQL with SSL in production
- Enable `TRUST_PROXY=true` if behind reverse proxy (Vercel/Render)
- Set strong `JWT_SECRET` (≥32 chars)
- Configure `CORS_ORIGIN` for your Android app distribution
- For serverless: use connection pooling

### Android
- Debug: uses `10.0.2.2:3000` for local emulator
- Release: must point to production HTTPS backend
- `usesCleartextTraffic="true"` allows HTTP in dev (ok for local testing)
- For production, use HTTPS only
