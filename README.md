# LifePath

LifePath is a gamified productivity app: quests, habits, streaks, and a city/garden view that reflects your progress. This repository contains the **Spring Boot** backend, **React (Vite)** web client, **Android** app, API specification, and design docs.

## Stack

| Layer | Technology |
|-------|------------|
| API | Java 21, Spring Boot, PostgreSQL, Flyway |
| Web | React 18, TypeScript, Vite |
| Mobile | Kotlin, Android (widgets & notifications) |
| Contract | OpenAPI (`openapi/lifepath-api.yaml`) |

## Quick start (Docker)

The fastest way to run the full stack locally:

```bash
git clone https://github.com/Vector26/LifePath.git
cd LifePath
cp .env.example .env
docker compose up -d
```

- **Web UI:** http://localhost:39173  
- **REST API:** http://localhost:39081/api/v1  

See [DOCKER.md](./DOCKER.md) for rebuild, logs, and environment variables.

## Development setup

### Prerequisites

- Docker & Docker Compose (for Postgres + optional full stack)
- JDK 21 and Maven (backend)
- Node.js 20+ (web)

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Default dev profile uses an embedded database unless you point `SPRING_PROFILES_ACTIVE` at Postgres (see `application-dev.yml`).

Run tests:

```bash
cd backend && ./mvnw test
```

### Web

```bash
cd web
npm install
npm run dev
```

The dev server runs on port **39173** and proxies `/api` to the backend at **39081**.

```bash
cd web && npm run test
```

### Android

Open the `android/` module in Android Studio. Create `android/local.properties` with your SDK path (see `android/local.properties.example`). Configure the server URL in the app to match your backend (`APP_FRONTEND_URL` / device-reachable host).

## Configuration

**Do not commit `.env`.** Copy [.env.example](./.env.example) and set:

- `POSTGRES_PASSWORD` — database password for Docker Compose  
- `APP_FRONTEND_URL` — browser/mobile URL for the web app (OAuth and deep links)  
- `GOOGLE_REDIRECT_URI` — Google Calendar OAuth callback  

Google Calendar integration requires OAuth client credentials configured in the backend (see `docs/backend/AUTHENTICATION.md`).

## Documentation

- [DOCKER.md](./DOCKER.md) — container deployment  
- [docs/web/ARCHITECTURE.md](./docs/web/ARCHITECTURE.md) — frontend structure  
- [docs/backend/API_SPECIFICATION.md](./docs/backend/API_SPECIFICATION.md) — API overview  
- [openapi/lifepath-api.yaml](./openapi/lifepath-api.yaml) — OpenAPI schema  

## License

MIT — see [LICENSE](./LICENSE).
