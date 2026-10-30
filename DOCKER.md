# LifePath Docker Deployment

Web and API listen on **39173** and **39081** by default to reduce port clashes with other local services.

## Quick start

From the repository root:

```bash
cp .env.example .env   # optional: customize POSTGRES_PASSWORD and public URLs
docker compose up -d
```

- Web: http://localhost:39173  
- API: http://localhost:39081/api/v1  
- Health: http://localhost:39081/api/v1/actuator/health  

Stop the stack:

```bash
docker compose down
```

Follow logs:

```bash
docker compose logs -f
```

Rebuild after code changes:

```bash
docker compose up -d --build
```

## Environment

Copy `.env.example` to `.env` and adjust:

| Variable | Purpose |
|----------|---------|
| `POSTGRES_PASSWORD` | Database password (defaults to `lifepath` in compose) |
| `APP_FRONTEND_URL` | Public web URL (needed for OAuth redirects from mobile) |
| `GOOGLE_REDIRECT_URI` | Google Calendar OAuth callback URL |

When exposing the app on a LAN or VPN, set `APP_FRONTEND_URL` and `GOOGLE_REDIRECT_URI` to reachable hostnames or IPs instead of `localhost`.

## APK in the web image

The web Docker build can bundle `web/public/lifepath.apk`. From `web/`, run `npm run sync-apk` before `docker compose build web` if you ship an Android APK with the site.
