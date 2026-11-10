# Restaurant Review Platform
Spring Boot 4 + Elasticsearch + Keycloak backend, Next.js 16 / React 19 frontend.
Based on the Devtiro restaurant review app (MIT, see `LICENSE`), reworked with role-based access,
Keycloak-only registration, an owner-approval workflow and a deployable setup.

| Part | Version |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1.2 (Spring Security 7, Spring Data Elasticsearch 6) |
| Search | Elasticsearch 9.2 |
| Identity | Keycloak 26.7 |
| Frontend | Next.js 16.4, React 19.3, Tailwind 3 |

Java package: `com.paras.restaurant`.

## Roles

All roles live in Keycloak (realm roles). Composite roles mean each role includes the ones below it.

| Role | Includes | Can do |
| --- | --- | --- |
| `USER` | | Browse and search, write one review per restaurant, edit own review (48 h), delete own review, ask for owner access |
| `RESTAURANT_OWNER` | `USER` | Everything above, plus create restaurants and edit/delete **only their own**. Cannot review their own restaurant |
| `ADMIN` | `RESTAURANT_OWNER`, `USER` | Approve/reject owner requests, edit/delete **any** restaurant, delete **any** review (moderation) |

**Registration** is Keycloak's own sign-up page (the "Sign up" button). There is no register endpoint in
the API. Every new account gets `USER` through the realm's default roles.

**Becoming an owner:** a user sends a request (`/become-owner`), an admin approves it (`/admin`).
On approval the API calls the Keycloak Admin API with a service account (`restaurant-review-backend`) to add
`RESTAURANT_OWNER`, and only then marks the request `APPROVED`. If Keycloak is down the request stays
`PENDING` and the admin can retry. The user's token only picks up the new role after a refresh; the
page does that automatically (or press "Refresh my session").

## Run it locally

Needs JDK 21, Maven, Node 22+, Docker.

```bash
docker compose up -d                 # Elasticsearch, Kibana, Keycloak (realm imported automatically)

cd backend
GEOCODING_PROVIDER=random mvn spring-boot:run     # "random" skips real address lookup for local testing

cd ../frontend
npm install
npm run dev                          # http://localhost:3000
```

Sign in as `admin` / `admin` (development default) to approve owner requests. Keycloak admin console:
http://localhost:9090 with `admin` / `admin`.

Remove `GEOCODING_PROVIDER=random` to use real addresses via OpenStreetMap Nominatim (needs internet and
a contact in `GEOCODING_USER_AGENT`, see below).

> **Upgrading an old local setup:** the `restaurants` index mapping changed (`createdBy`). Reset the data once:
> `docker compose down -v && docker compose up -d`.

Tests: `cd backend && mvn test` (pure unit tests, no Docker needed).
Frontend checks: `npm run typecheck && npm run lint && npm run build`.

## Deploy

1. Copy `.env.example` to `.env` and fill in every value (public URLs, passwords, a random
   `KEYCLOAK_ADMIN_CLIENT_SECRET` from `openssl rand -hex 32`, a real contact e-mail in `GEOCODING_USER_AGENT`).
2. Put HTTPS in front: one hostname for the site (port 3000) and one for Keycloak (port 9090), e.g. with Caddy:
   ```
   reviews.example.com { reverse_proxy localhost:3000 }
   auth.example.com    { reverse_proxy localhost:9090 }
   ```
3. Start everything:
   ```bash
   docker compose -f docker-compose.prod.yml --env-file .env up -d --build
   ```
4. Sign in at your site as `admin` with `INITIAL_ADMIN_PASSWORD`, then change that password in Keycloak.

Things to know:
- The realm file is only imported the **first time** Keycloak starts. Later changes are made in the admin console.
- `NEXT_PUBLIC_*` values and `BACKEND_URL` are fixed when the frontend image is built, so rebuild after changing them.
- Uploaded photos live in the `uploads` Docker volume; Elasticsearch data in `es-data`. Back both up.
- Email verification and password-reset mails need SMTP configured in Keycloak (Realm settings, Email). Until
  then `verifyEmail` is off. Turn it on once SMTP works.
- Elasticsearch has no authentication here and is not published to the host in the production compose file. Keep it that way.

## Configuration (backend)

| Variable | Default | Purpose |
| --- | --- | --- |
| `ELASTICSEARCH_URIS` | `http://localhost:9200` | Elasticsearch |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:9090/realms/restaurant-review` | Must equal the `iss` in tokens (the URL browsers use) |
| `KEYCLOAK_JWK_SET_URI` | issuer + `/protocol/openid-connect/certs` | Internal URL for signing keys |
| `KEYCLOAK_URL` | `http://localhost:9090` | Keycloak base URL for the Admin API |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | `change-me-dev-secret` | Secret of the `restaurant-review-backend` client |
| `GEOCODING_PROVIDER` | `nominatim` | `nominatim` or `random` (fake London coordinates, dev only) |
| `GEOCODING_USER_AGENT` | placeholder | Required by Nominatim: identify yourself with a contact |
| `UPLOAD_DIR` | `uploads` | Photo folder |

## API summary

| Endpoint | Who |
| --- | --- |
| `GET /api/restaurants`, `/{id}`, `/{id}/reviews`, `GET /api/photos/{file}` | Anyone |
| `POST /api/restaurants` | Owner, Admin |
| `GET /api/restaurants/mine` | Owner, Admin |
| `PUT/DELETE /api/restaurants/{id}` | Its owner, or Admin |
| `POST /api/restaurants/{id}/reviews` | User (not the restaurant's owner) |
| `PUT /api/restaurants/{id}/reviews/{rid}` | Review author |
| `DELETE /api/restaurants/{id}/reviews/{rid}` | Review author, or Admin |
| `POST /api/photos` | Any signed-in user (JPEG/PNG/WebP, 5 MB) |
| `POST /api/owner-requests`, `GET /api/owner-requests/me` | User |
| `GET /api/admin/owner-requests?status=`, `POST .../{id}/approve`, `POST .../{id}/reject` | Admin |

Errors always look like `{ "status": 403, "message": "...", "timestamp": "..." }`. Paged lists return
`{ content, page (1-based), size, totalElements, totalPages, first, last }`.

## What changed from the original

**Security fixes:** any signed-in user could edit or delete any restaurant and delete any review; restaurants had no owner;
photo downloads allowed `../` path traversal; uploads trusted the client's file name and type; the catch-all error handler turned
"forbidden" into HTTP 500; a test page for uploads was exposed at `/admin`.
**Correctness:** restaurant search ignored location whenever text was given (now one combinable query); concurrent review writes could
silently overwrite each other (optimistic locking, 409 on conflict); timestamps are UTC; missing review returned 204 (now 404); delete of a
missing restaurant succeeded silently; photo ids are validated; fake London geocoding replaced by real geocoding.
**Frontend:** Next 16/React 19, typed code with build checks enabled, token read fresh per request, providers moved inside `<body>`,
role-aware menus/pages, unused UI kit and libraries removed.
