# 🏛️ MonumentQuest — Heritage Exploration & Gamified AR Platform

**MonumentQuest** is a production-grade, gamified mobile application and backend ecosystem designed for exploring historic monuments, temples, stupas, and living cultural heritage. Powered by **Photorealistic 4K Satellite Imagery**, **Instantaneous 5Hz GPS Tracking**, **AI Cultural Historians**, and **Real-Time Social Expeditions**.

---

## 🌟 Core Feature Matrix

### 📍 1. Photorealistic Map & Instant GPS Engine
- **Photorealistic 4K Satellite Imagery**: High-resolution Google Hybrid Aerial map layer showing 3D terrain, vegetation, and structures globally.
- **5 Hz High-Precision GPS Engine**: 200ms real-time location stream powered by Google Fused Location API and adaptive velocity-aware Kalman filtering.
- **Live Real-World Heritage Pipeline**: Dynamic OpenStreetMap Overpass API queries fetching authentic historic monuments surrounding the user's exact live location.
- **Interactive AR Radar Scan**: Triggers a 180m proximity radar pulse across the map to discover hidden heritage sites and award bonus XP.

### 🎙️ 2. AI Cultural & Architectural Historian
- **In-Character Storytelling**: Interactive AI Cultural Historian grounded in verified architectural history, sacred rituals, and ancient mythology.
- **Quick Question Chips**: 1-tap prompts for construction history, Kalinga/Indo-Islamic architectural styles, and legends.

### 📸 3. AR Discovery & Monument Inspection
- **AR Camera Scan**: Glowing reticle overlay, discovery glow animations, and haptic feedback on successful monument capture.
- **Dynamic Upload Rarity Multipliers**:
  - `5.0x XP` for First Discoverers (0 previous uploads)
  - `3.0x XP` for Early Pioneers (1–5 uploads)
  - `1.5x XP` for Explorers (6–20 uploads)

### 🏆 4. Pokédex Collection Vault & Digital Passport
- **Collection Book**: Grid-based Pokédex model featuring unlocked vibrant entries vs. blurred ghost silhouettes of unexplored monuments.
- **Adopt-a-Monument ("Grand Custodian")**: Top contributors earn Grand Custodian status and daily passive Heritage Coins (`🪙`).
- **Regional Progress Bars**: Tracks completion across regional categories (*e.g., Odisha Temples 50%, Buddhist Pagodas 50%*).

### 📱 5. Chronicles Social Feed & Guilds
- **Live Search Bar**: Real-time filtering by monument name, explorer handle, or `#hashtags` (*#kalinga, #lingaraj*).
- **Interactive Comments Drawer**: Slide-up comments thread with live community replies.
- **Segmented Leaderboards**: Standings split by *Friends*, *Regional*, and *Global* with top 3 podium highlights.
- **Regional Guild Challenges**: Guild events (*"The Odisha Guild Challenge: Visit 500 Shrines this weekend"*).

### 👻 6. Privacy & Sustainability (Journalist Mode)
- **Ghost Mode Privacy**: Hide live position from others on the public map while earning full XP.
- **Journalist Mode**: Write off-season historical reflections from home verified by AI to maintain daily expedition streaks.

---

## 🏗️ Architecture & Technology Stack

```
                     ┌──────────────────────────────────────────┐
                     │    Android Mobile App (Jetpack Compose)  │
                     │  - Kotlin, Hilt, Osmdroid, CameraX, AR   │
                     └────────────────────┬─────────────────────┘
                                          │
                   ┌──────────────────────┴──────────────────────┐
                   │                                             │
                   ▼                                             ▼
┌────────────────────────────────────┐       ┌─────────────────────────────────────┐
│    Node.js / Express API Backend   │       │     OpenStreetMap Overpass API      │
│  - TypeScript, Prisma, PostgreSQL  │       │  - Live Global Heritage Coordinates │
└────────────────────────────────────┘       └─────────────────────────────────────┘
```

### Mobile Tech Stack:
- **UI Framework**: Android Jetpack Compose + Material 3 (Obsidian Dark Glassmorphism)
- **Architecture**: Clean Architecture + MVVM + Dagger Hilt Dependency Injection
- **Map & Isometric Overlay**: Osmdroid + CartoDB Voyager + OSM Overpass geometry
- **Location Engine**: Google Play Services Fused Location Client (5Hz 200ms rate) + Kalman Filter
- **Networking**: Retrofit2 + OkHttp3 + Coroutines Flow
- **Image Loading**: Coil AsyncImage

### Backend Tech Stack:
- **Runtime**: Node.js + Express + TypeScript
- **Database & ORM**: PostgreSQL + Prisma ORM
- **Security**: JWT Authentication, Bcrypt Password Hashing, Helmet, CORS
- **APIs**: `/api/v1/auth`, `/api/v1/monuments`, `/api/v1/feed`, `/api/v1/ai/narrator`

---

## 🚀 Building & Running Locally

### 1. Mobile Android App
```bash
# Clone the repository
git clone https://github.com/Aman-Amarjit/monument-quest.git
cd monument-quest

# Build Debug APK
./gradlew assembleDebug

# Install to connected Android device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Production Node.js Backend
```bash
cd backend

# Install dependencies
npm install

# Build TypeScript
npm run build

# Start production API server (Port 3000)
npm run start
```

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.


## Backend API

The backend in [backend](backend) is now persistent and runnable locally. It uses Prisma with PostgreSQL, JWT authentication, transactional discovery rewards, and a shared PostgreSQL rate-limit bucket for multi-instance deployments.

The API contract is available at [backend/openapi.yaml](backend/openapi.yaml). In production, set a strong JWT_SECRET, a PostgreSQL DATABASE_URL, and an explicit CORS_ORIGIN (comma-separated origins are supported). Run npm run db:push once during initial provisioning, then npm run db:seed to load the catalog.

```bash
cd backend
cp .env.example .env
npm install
npm run db:push
npm run db:seed
npm run dev
```

The API is served under `/api/v1`:

- `POST /auth/register`, `POST /auth/login`, `POST /auth/guest`, and authenticated `GET /auth/me`
- `GET /monuments`, `GET /monuments/nearby?latitude=20.24&longitude=85.83`, and authenticated `POST /monuments/capture`
- `GET /feed`, authenticated `POST /feed/posts`, and authenticated `POST /feed/posts/:id/like`

Use `Authorization: Bearer <token>` for protected routes. A discovery can only award XP once per explorer and only within 100 metres of the seeded monument.
