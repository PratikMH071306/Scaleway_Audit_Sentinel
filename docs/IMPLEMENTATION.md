# Secure Monitor — Implementation Guide

This document explains what Secure Monitor is, why it exists, how it is built, and how to run and test it. It is written for a developer joining the project.

## 1. What the project does

Secure Monitor watches a Scaleway organization for suspicious sign-in activity and gives an operator a place to review and respond.

The product does four jobs:

1. It pulls recent audit events from Scaleway and stores the ones it has not seen before.
2. It looks for a burst of failed password logins for the same user, from the same IP, in the same organization.
3. It opens an alert when that pattern is found.
4. An administrator can lock or unlock the matching Scaleway IAM user, or mark the alert as resolved.

The user interface is a small web app: a login page, an alert list, and an alert detail page.

## 2. Why it was built

Cloud audit logs are detailed, but they are hard to watch by hand. A password-guessing attempt shows up as many failed `CreatePasswordLogin` calls. A later successful `CommitLogin` from the same place is more serious, because the attacker may have gotten in.

Secure Monitor exists to:

- Collect those events in one database.
- Apply one clear detection rule instead of asking a person to scan the raw log.
- Keep an alert open until someone handles it, so the same spike is not turned into a second open alert.
- Record who locked or unlocked a user, and whether Scaleway accepted the change.

It is an internal security operations tool. It does not replace Scaleway IAM. It uses Scaleway as the source of truth for users and for the lock state.

## 3. How it was designed

The system has three runtime parts:

| Part | Technology | Role |
| --- | --- | --- |
| API | Java 21, Spring Boot 4.1 | Auth, detection, alerts, remediation, Scaleway calls |
| Database | PostgreSQL 16 | Users, stored audit events, alerts, remediation history |
| Web UI | React 19, Vite, Tailwind | Login and alert screens |

The API is stateless. The browser sends a JWT on each request. There is no server session.

Packages follow the feature, not a single giant layer:

- `security` — login, JWT, roles, bootstrap admin
- `scaleway` — audit trail client and event mapping
- `scaleway.iam` — IAM user lookup, lock, and unlock
- `detection` — the failed-login rule
- `alert` — create, list, and resolve alerts
- `remediation` — lock and unlock, plus the history of those actions
- `domain` and `repository` — JPA entities and database access
- `controller` — HTTP endpoints (auth lives next to the security package)

The UI is a separate app. It talks only to the API. It does not call Scaleway.

```text
Browser
  |  HTTPS / JSON + Bearer JWT
  v
React UI (nginx in Docker, Vite in local dev)
  |
  v
Spring Boot API :8084
  |                         \
  v                          v
PostgreSQL              Scaleway API
  users                   audit-trail events
  security_events         IAM get / lock / unlock
  alerts
  remediations
```

Hibernate updates the schema on startup (`ddl-auto: update`). That is convenient for this project. It is not a migration tool. Schema changes should still be reviewed before a shared environment is updated.

## 4. How each milestone was implemented

The code was built in layers. Each layer can be understood on its own.

### Milestone 1 — Application shell and stored data

The Spring Boot application (`SecureMonitorApplication`) and PostgreSQL connection were put in place first. Four tables hold the product data:

| Entity | Table | What it stores |
| --- | --- | --- |
| `UserEntity` | users | Local operator accounts (username, BCrypt hash, role, enabled flag) |
| `SecurityEventEntity` | security events | A copy of a Scaleway audit event, keyed by Scaleway event id |
| `AlertEntity` | alerts | One open or resolved finding |
| `RemediationEntity` | remediations | One lock or unlock attempt, including success or failure |

Repositories are Spring Data interfaces: `UserRepository`, `SecurityEventRepository`, `AlertRepository`, `RemediationRepository`.

### Milestone 2 — Scaleway audit ingestion

`ScalewayClient` calls `GET /audit-trail/v1alpha1/regions/{region}/events` with the organization id, a lookback window (default one day, `P1D`), and a page size of 100. The secret key is sent as `X-Auth-Token`.

`ScalewayEventMapper` turns each audit JSON object into a `SecurityEventEntity`. `ScalewayService.syncEvents()` drops events with no id, drops duplicates inside the same response, skips ids already in the database, and saves the rest. A Scaleway outage is logged and does not crash the process.

`POST /api/scaleway/events/sync` runs that sync. `GET /api/scaleway/events` returns the live Scaleway response without saving it. `GET /api/security-events` lists what is already stored.

### Milestone 3 — Detection

`DetectionService.detectFailedAuthentication()` loads authentication events from the last seven days and passes them to `FailedAuthenticationRule`.

The rule groups failed password logins by organization, username, and source IP. It finds the densest 10-minute window. Five or more failures in that window is a detection. If a successful `CommitLogin` for the same subject happens inside that window, the result is marked `followedBySuccess`.

`GET /api/detection/failed-authentication` runs the rule and creates alerts for positive results.

### Milestone 4 — Alerts

`AlertService.record()` writes an alert only when detection is positive. If an **open** alert already exists for the same rule, organization, username, and source IP, that existing alert is returned. A new alert is not opened.

Severity:

- `HIGH` for a failed-login spike
- `CRITICAL` when that spike is followed by a successful login
- `MEDIUM` for any other rule name

`GET /api/alerts` lists alerts, newest first. `GET /api/alerts/{id}` returns one alert and refreshes the Scaleway lock flag when a user id is known. `POST /api/alerts/{id}/resolve` is admin-only and sets the status to `RESOLVED`.

### Milestone 5 — Authentication

Local users live in PostgreSQL. `POST /api/auth/login` checks the username and password and returns a JWT.

`AdminUserInitializer` creates one admin on startup when `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD` are set and that username does not exist yet. Later startups do not reset the password.

`JwtAuthenticationFilter` reads `Authorization: Bearer ...` and sets the Spring Security user. Sessions are not used. CSRF protection is off because the API does not use cookies for auth.

Roles are `USER` and `ADMIN`. Any signed-in user can read alerts. Only `ADMIN` can resolve an alert or lock and unlock a user.

### Milestone 6 — Remediation

`POST /api/alerts/{alertId}/remediate` accepts `LOCK_USER` or `UNLOCK_USER`.

`RemediationService`:

1. Requires an open alert.
2. Resolves the Scaleway IAM user id from the alert, related events, or an IAM username search.
3. Reads the user from Scaleway.
4. If the user is already in the requested state, records success without calling lock or unlock again.
5. Otherwise calls Scaleway, reads the user again, and records success only if the lock flag matches the request.
6. On success, updates `scalewayUserLocked` on the alert.
7. Tries to sync audit events so the lock action can appear in stored events. A failed sync does not undo the lock.

`GET /api/alerts/{alertId}/remediations` returns that history, newest first.

### Milestone 7 — Web UI

The React app has three routes:

| Route | Page | Who can open it |
| --- | --- | --- |
| `/login` | `LoginPage` | Anyone |
| `/alerts` | `DashboardPage` | Signed-in user |
| `/alerts/:id` | `AlertDetailsPage` | Signed-in user |

`AuthContext` stores the login response in the browser session. `http.js` attaches the bearer token and sends the user back to login on HTTP 401. The dashboard lists and filters alerts. The detail page shows the alert and, for an admin, the resolve and lock or unlock actions.

### Milestone 8 — Deployment

`docker-compose.yml` starts PostgreSQL, the API, and the UI.

- Postgres waits until `pg_isready` succeeds.
- The API waits until Postgres is healthy, then exposes port 8084. Its health check is `GET /actuator/health`.
- The UI image is built with `VITE_API_BASE_URL=http://localhost:8084` and is served by nginx on port 8080. nginx falls back to `index.html` so client-side routes work.

## 5. Files and classes

### Backend entry and configuration

| File | Purpose |
| --- | --- |
| `SecureMonitorApplication` | Spring Boot main class |
| `application.yml` | Port 8084, database, JWT, CORS, Scaleway, health endpoint |
| `CorsConfig`, `CorsProperties` | Browser origins allowed to call the API |
| `SecurityConfig` | Which paths are public, which need a JWT, which need admin |
| `ApiExceptionHandler` | Turns validation and status errors into JSON |

### Security

| Class | Purpose |
| --- | --- |
| `AuthController` | `POST /api/auth/login` |
| `AuthenticationService` | Checks password and issues a token |
| `JwtService`, `JwtProperties` | Creates and reads JWTs |
| `JwtAuthenticationFilter` | Puts the user on the request |
| `CustomUserDetailsService`, `MonitorUserDetails` | Loads the local user for Spring Security |
| `AuthenticationContext` | Current user id and username during a request |
| `AdminUserInitializer`, `BootstrapProperties` | Optional first admin user |
| `Role` | `USER`, `ADMIN` |
| `LoginRequest`, `LoginResponse` | Login JSON |

### Scaleway

| Class | Purpose |
| --- | --- |
| `ScalewayConfig` | Secret key, access key, organization, region, lookback |
| `ScalewayClient` | Audit Trail HTTP client |
| `ScalewayService` | Fetch and sync events |
| `ScalewayEventMapper` | Audit JSON to `SecurityEventEntity` |
| `ScalewayController` | Sync and raw-event endpoints |
| `ScalewayIamClient` | List, get, lock, and unlock IAM users |
| `ScalewayUserResolver` | Fill in user id and current lock state on an alert |
| `ScalewayApiException` | Safe error text for callers; raw Scaleway body stays in logs |
| DTOs under `scaleway.dto` | `AuditEventDto`, `AuditEventsResponse`, `PrincipalDto`, `ResourceDto`, `ScalewayUserDto`, `ScalewayUsersResponse` |

### Detection, alerts, remediation

| Class | Purpose |
| --- | --- |
| `DetectionController` | Runs failed-authentication detection |
| `DetectionService` | Loads recent events and records alerts |
| `DetectionRule` | Rule interface |
| `FailedAuthenticationRule` | Five failures in ten minutes |
| `DetectionResult` | One subject’s evaluation |
| `AlertController`, `AlertService`, `AlertResponse` | List, get, resolve |
| `AlertStatus`, `AlertSeverity` | `OPEN` / `RESOLVED`, `CRITICAL` / `HIGH` / `MEDIUM` |
| `RemediationController`, `RemediationService` | Lock, unlock, history |
| `RemediationAction`, `RemediationRequest`, `RemediationResponse`, `RemediationStatus` | Action contract and stored outcome |
| `SecurityEventController` | Lists stored events |

### Frontend

| Path | Purpose |
| --- | --- |
| `src/App.jsx` | Routes |
| `src/auth/AuthContext.jsx`, `session.js`, `authApi.js`, `ProtectedRoute.jsx` | Login session |
| `src/pages/LoginPage.jsx` | Sign-in form |
| `src/pages/DashboardPage.jsx` | Alert list |
| `src/pages/AlertDetailsPage.jsx` | One alert and actions |
| `src/api/http.js`, `alertApi.js` | API client |
| `src/components/*` | Table, filters, badges, header |
| `src/model/alert.js`, `src/alerts/view.js` | Display helpers |

### Deployment

| File | Purpose |
| --- | --- |
| `docker-compose.yml` | Postgres, API, UI |
| `backend/Dockerfile` | Maven build, then a JRE image |
| `frontend/Dockerfile` | Vite build, then nginx |
| `frontend/nginx.conf` | Single-page app routing |
| `frontend/.env.example` | `VITE_API_BASE_URL` |

## 6. How the components communicate

A normal operator flow:

1. The UI posts username and password to `/api/auth/login`.
2. The API checks the user row and returns a JWT.
3. The UI stores the token and sends `Authorization: Bearer <token>` on later calls.
4. Someone (or a scheduled caller) posts to `/api/scaleway/events/sync`. New audit rows are inserted.
5. Someone calls `/api/detection/failed-authentication`. The rule reads stored events and `AlertService` inserts alerts.
6. The dashboard calls `GET /api/alerts`.
7. Opening an alert calls `GET /api/alerts/{id}`. The API may call Scaleway IAM to refresh whether the user is locked.
8. An admin posts `LOCK_USER` or `UNLOCK_USER`. The API calls Scaleway, writes a remediation row, and updates the alert’s lock flag.
9. Resolve is a separate call. Locking a user does not by itself mark the alert resolved.

All of those API calls except login and the health check require a valid JWT. Resolve and remediate also require the `ADMIN` role.

## 7. How to run the project

### With Docker Compose

From the repository root, create a `.env` file. Compose passes it to the API. Postgres also reads `DB_USERNAME` and `DB_PASSWORD` from the shell environment.

```text
DB_USERNAME=secure_monitor
DB_PASSWORD=choose-a-database-password
DB_URL=jdbc:postgresql://postgres:5432/secure_monitor
SCW_SECRET_KEY=your-scaleway-secret-key
SCW_ACCESS_KEY=your-scaleway-access-key
SCW_ORGANIZATION_ID=your-organization-id
SCW_REGION=fr-par
JWT_SECRET=a-long-random-string
JWT_EXPIRATION=3600
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=choose-an-admin-password
```

`JWT_SECRET` must be long enough for HMAC-SHA (use at least 32 bytes of random data). Do not commit `.env`.

```text
docker compose up --build
```

- UI: http://localhost:8080
- API: http://localhost:8084
- Health: http://localhost:8084/actuator/health
- OpenAPI UI (when the API is up): http://localhost:8084/swagger-ui.html

Sign in with the bootstrap admin username and password. Then sync events and run detection (any API client, with the bearer token):

```text
POST /api/scaleway/events/sync
GET  /api/detection/failed-authentication
```

### Without Docker

1. Start PostgreSQL and create a database named `secure_monitor`.
2. Export the same variables. Set `DB_URL` to `jdbc:postgresql://localhost:5432/secure_monitor`.
3. From `backend`, run `./mvnw spring-boot:run` (or `mvnw.cmd` on Windows).
4. From `frontend`, copy `.env.example` to `.env` and run `npm install` then `npm run dev`.

The Vite dev server proxies nothing by itself. `VITE_API_BASE_URL` must point at the API. CORS already allows `http://localhost` and `http://127.0.0.1` on any port.

## 8. How to test the project

### Backend

From `backend`:

```text
./mvnw test
```

Repository tests (`UserRepositoryTest`, `SecurityEventRepositoryTest`, `AlertRepositoryTest`, `RemediationRepositoryTest`) use Testcontainers and need Docker. They start `postgres:16-alpine`.

Other tests use JUnit and Spring’s test slice or Mockito:

| Area | Test class |
| --- | --- |
| Login | `AuthenticationServiceTest` |
| JWT | `JwtServiceTest`, `JwtAuthenticationFilterTest` |
| CORS | `CorsConfigTest` |
| Scaleway mapping and client | `ScalewayEventMapperTest`, `ScalewayClientTest`, `ScalewayServiceTest` |
| Detection | `DetectionServiceTest` |
| Remediation | `RemediationServiceTest` |
| HTTP | `AlertControllerTest`, `DetectionControllerTest`, `SecurityEventControllerTest` |
| Context load | `SecureMonitorApplicationTests` |

`src/test/resources/application.yml` supplies test values so the suite does not need real Scaleway keys.

### Frontend

From `frontend`:

```text
npm test
```

Vitest and Testing Library cover the HTTP client, alert API, alert view helpers, the dashboard, and the alert detail page. Tests use fixtures in `src/test/fixtures.js`. They do not call a running API.

## 9. How Scaleway integration works

Two Scaleway APIs are used. Both use `https://api.scaleway.com` and the secret key header `X-Auth-Token`.

**Audit Trail.** `ScalewayClient.getEvents()` requests events recorded after `now - event-lookback` for `SCW_ORGANIZATION_ID` in `SCW_REGION` (default `fr-par`). Only the first page (100 events) is requested. Sync is idempotent: the primary key is the Scaleway event id.

**IAM.** `ScalewayIamClient` reads users and posts to `/iam/v1alpha1/users/{userId}/lock` and `.../unlock`. The secret key’s IAM policy must allow those actions. HTTP errors are mapped to short messages (invalid credentials, permission denied, user not found, rate limit, Scaleway unavailable). Those messages are what the API returns. The raw response body is not sent to the browser.

`ScalewayUserResolver` fills `scalewayUserId` when it is missing. It looks at related stored events first, then lists IAM users in the organization and matches the username.

Startup prints whether a secret key is present and which organization and region are configured. It does not print the key.

## 10. How authentication works

1. Passwords are stored as BCrypt hashes.
2. Login always runs a password check, even when the username is unknown, so the response time does not reveal whether the account exists. The error text is always `Invalid username or password`. Disabled users get the same error.
3. A valid login returns a bearer token, the expiry in seconds (default 3600), the username, and the role.
4. The filter rejects missing or bad tokens with HTTP 401 and JSON `{"message":"Authentication is required"}`.
5. A valid token with the wrong role gets HTTP 403 and `{"message":"You do not have permission to perform this action"}`.
6. Public paths are `/api/auth/**` and `/actuator/health`. Browser preflight `OPTIONS` requests under `/api/**` are allowed. Everything else requires a token.

The UI keeps the token in session storage and clears it on logout or on 401.

## 11. How detection works

Input: stored events whose service name is `scaleway.iam.v1alpha1.UnauthenticatedApi` and whose `recordedAt` is within the last seven days.

A failure counts only when all of these are true:

- Method name is `CreatePasswordLogin`
- HTTP status is 401
- Username, source IP, organization id, and timestamp are present

Events are grouped by organization + username + source IP. For each group, a sliding 10-minute window finds the largest cluster of failures. If that cluster has at least five events, `detected` is true.

A success counts when the method is `CommitLogin`, the status is 200, and it falls in the same window for the same subject. That sets `followedBySuccess` and adds the success event id to `relatedEventIds`.

The IAM user id is taken from a resource whose type is `iam_user`, if the audit event includes one.

Calling the detection endpoint is what creates alerts. Syncing events does not run the rule by itself.

## 12. How alerts work

An alert is created only from a positive `DetectionResult`.

Duplicate protection: one open alert per rule + organization + username + source IP. Running detection again updates nothing on that open alert; it returns the existing one. After the alert is resolved, a later detection can open a new one.

Fields operators see include title (`Suspicious authentication activity`), description (counts, user, IP, and whether a success followed), severity, status, detection time, source IP, organization, username, event count, and related event ids.

`GET /api/alerts/{id}` also tries to attach a Scaleway user id and the current locked flag. If Scaleway cannot be reached, the stored alert is still returned.

Resolve sets status to `RESOLVED`. It does not unlock the user.

## 13. How remediation works

Only an open alert can be remediated. The body is:

```json
{ "action": "LOCK_USER" }
```

or `UNLOCK_USER`.

If the alert has no IAM user id after resolution, the API returns HTTP 422 with `This alert has no Scaleway IAM user id`.

Each attempt is stored:

- alert id, action, target user id and username
- the local admin who requested it
- lock state before the call
- status (`SUCCESS` or `FAILED`), message, time, and lock state after

If Scaleway says the user is already locked and the action is lock (or already unlocked and the action is unlock), the row is a success and no lock API call is made.

If the call succeeds but a follow-up read does not show the expected lock flag, the row is a failure and the alert’s lock flag is left unchanged.

After a successful remediation the API tries `syncEvents()` so the IAM change can be copied into `security_events`. Failure of that sync is ignored so the admin still sees the remediation result.

Remediation history is available to any authenticated user. Performing remediation requires `ADMIN`.

## 14. How the project is deployed

Docker Compose is the deployment unit in this repository.

**Postgres** uses image `postgres:16-alpine`, database name `secure_monitor`, and a named volume `postgres_data`. Credentials come from `DB_USERNAME` and `DB_PASSWORD`.

**Backend image** (`backend/Dockerfile`):

1. Build stage: `eclipse-temurin:21-jdk-alpine`, Maven wrapper, `./mvnw -B -DskipTests package`.
2. Run stage: JRE 21 plus `wget` (for the health check), jar `secure-monitor-0.0.1-SNAPSHOT.jar`.
3. Listens on 8084. `env_file: .env` supplies secrets. Compose overrides `DB_URL` so the API uses the `postgres` service hostname, not localhost.

**Frontend image** (`frontend/Dockerfile`):

1. `npm ci` and `npm run build`. `VITE_API_BASE_URL` is baked in at build time. Changing the API URL means rebuilding the UI image.
2. `nginx:1.27-alpine` serves `dist` on port 80, published as host port 8080.
3. `try_files` sends unknown paths to `index.html`.

Startup order is Postgres healthy, then API healthy, then UI. The API health check allows 40 seconds of start time because the JVM and Hibernate schema update take longer than a simple HTTP process.

There is no Kubernetes manifest and no CI pipeline in the repo. A production host needs the same environment variables, a reachable Postgres, and a UI build whose `VITE_API_BASE_URL` matches the public API origin. CORS must include that UI origin if the browser calls the API on a different host. The current allowed origins are localhost and 127.0.0.1 only.
