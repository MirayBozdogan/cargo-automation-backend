# Cargo Automation Backend

Spring Boot API for a cargo automation project. Customers create and track their own shipments, staff manage acceptance and delivery, and admins manage staff and view shipment reports.

## Technologies

Java 21, Spring Boot 4.1.0, Spring Security, Spring Data JPA, PostgreSQL, Flyway, BCrypt and JJWT. The Maven Wrapper and a Docker Compose setup are included.

Frontend: [cargo-automation-frontend](https://github.com/MirayBozdogan/cargo-automation-frontend).

## Roles

| Role | Main permissions |
| --- | --- |
| `USER` | Register, sign in, read own profile, add own addresses, create and track own shipments |
| `STAFF` | View customers, create shipments on their behalf, view operations, accept and prepare shipments, assign couriers |
| `ADMIN` | Staff operations, staff management and aggregate shipment reports |

Courier is an assignment to an active `STAFF` account, not a separate role. Delivery can be recorded by the assigned staff member or an admin. The assigned courier must still be active. Public registration always creates a `USER`; roles cannot be chosen through registration.

## Quick start with Docker

Requirements: Git and Docker (Docker Desktop on macOS/Windows). Java and PostgreSQL do not need to be installed.

```sh
git clone https://github.com/MirayBozdogan/cargo-automation-backend.git
cd cargo-automation-backend
cp .env.example .env
```

Fill in `.env`: a local database password, a JWT key from `openssl rand -base64 32` and, for the first admin, `DEV_ADMIN_EMAIL` / `DEV_ADMIN_PASSWORD` (at least 12 characters). Then:

```sh
docker compose up --build
```

This starts PostgreSQL and the backend at `http://localhost:8080` with the `dev` profile: Flyway creates the tables, loads the sample data and the first admin is created. Data is kept in the `pgdata` Docker volume across restarts; `docker compose down -v` deletes it. The database is also reachable from the host on `localhost:5433` (change with `DB_HOST_PORT`). Keep `.env` out of Git.

## Local setup without Docker

Requirements: Java 21, PostgreSQL, Git and internet access for the first Maven dependency download. The isolated test command also needs Bash and `psql` on PATH.

```sh
git clone https://github.com/MirayBozdogan/cargo-automation-backend.git
cd cargo-automation-backend
```

Create the development database using your PostgreSQL account; the example below uses `postgres`:

```sh
createdb -U postgres cargo_automation_dev
cp .env.local.properties.example .env.local.properties
```

Fill in the local file with your own connection values:

```properties
DB_URL=jdbc:postgresql://localhost:5432/cargo_automation_dev
DB_USERNAME=postgres
DB_PASSWORD=your-local-database-password
JWT_SECRET=your-generated-base64-key
```

Generate a JWT key locally with `openssl rand -base64 32` and put the output in `JWT_SECRET`. Keep `.env.local.properties` out of Git. Run the backend from the repository root so the local file is found.

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows use `mvnw.cmd` instead of `./mvnw` and `copy` instead of `cp`.

API base URL: `http://localhost:8080`. Flyway creates the tables from `src/main/resources/db/migration`; Hibernate only validates the schema (`ddl-auto=validate`). The `dev` profile also loads 81 Turkish provinces and 973 districts, plus 7 fictional price ranges, from `src/main/resources/db/dev`. This seed only runs against a database named `cargo_automation_dev`; restarting does not duplicate those sample rows. No account is created by default.

A database created by an earlier version (Hibernate `ddl-auto=update`, no Flyway history) is marked as version 1 on first start and its rows are kept. See the [migration notes](src/main/resources/db/manual/README.md), including how to add the next schema change. Older shipments whose creation time was missing receive the migration time, not a reconstructed historical time.

## First local admin

The development initializer is opt-in and only runs with the `dev` profile on `cargo_automation_dev`. Add these keys to your ignored local properties file and fill in your own email and password:

```properties
DEV_ADMIN_ENABLED=true
DEV_ADMIN_EMAIL=
DEV_ADMIN_PASSWORD=
```

The password must be at least 12 characters and at most 72 UTF-8 bytes. Start the backend; the log shows `Yerel yönetici oluşturuldu`. The initializer hashes the password and creates an `ADMIN`; it never promotes an existing customer, resets an existing admin password or reactivates an existing account, so the setting can stay enabled on later starts. Use a different email if it is already registered under another role. With Docker, the same three keys are read from `.env`.

An admin can create `STAFF` accounts through Personel Yönetimi. Additional admin creation is not exposed in the application UI. This initializer is for local development.

## Shipment lifecycle

```text
CREATED → ACCEPTED → READY_FOR_DISTRIBUTION → OUT_FOR_DELIVERY → DELIVERED
```

- A shipment starts as `CREATED` with a server-generated 12-character barcode and creation time.
- Only the sender can edit or soft-delete it, and only while it is `CREATED`.
- Acceptance and preparation require staff/admin access and must follow the sequence.
- Courier assignment moves a ready shipment to `OUT_FOR_DELIVERY` in the same transaction.
- Delivery stores `deliveredAt` once. Repeated or out-of-order actions return `409`.
- Customers can read shipments where they are the sender or receiver; knowing a barcode alone does not grant access.

Price calculation uses `width × length × height / 3000`, rounded to two decimals with HALF_UP, then takes the larger of desi and weight. Tariff lower bounds are inclusive and upper bounds exclusive: `[minDesi, maxDesi)`. Price is recalculated on the server when a shipment is saved. Sample prices are fictional.

## Main API routes

`POST /auth/login` sets the JWT in an `HttpOnly` cookie named `cargo_session` (`SameSite=Strict`, 24 hours) and does not return the token in the response body, so browser JavaScript cannot read it. `POST /auth/logout` expires the cookie. An `Authorization: Bearer <token>` header is still accepted for API tools. Accounts and roles are rechecked from the database on each request; deactivation rejects an existing token too. Logout is stateless: a token copied before logout stays valid until it expires.

| Method | Route | Access / purpose |
| --- | --- | --- |
| POST | `/auth/register`, `/auth/login`, `/auth/logout` | Public registration, login (sets session cookie) and logout |
| GET | `/users/me` | Current authenticated account |
| GET, POST | `/users/{id}/address` | Read permitted addresses / add own address |
| GET | `/users`, `/users/customers/{id}` | Staff/admin customer list and detail |
| GET, POST | `/staff` | Admin staff list and creation |
| PUT | `/staff/{id}` | Admin staff update |
| PATCH | `/staff/{id}/active` | Admin activation/deactivation |
| GET | `/city`, `/district`, `/price` | Public reference data |
| POST | `/shipment/calculate-price` | Public price preview |
| GET, POST | `/shipment` | Own shipments / create shipment |
| GET, PUT, DELETE | `/shipment/{id}` | Read own shipment / edit or soft-delete as sender |
| GET | `/shipment/tracking/{barcode}` | Safe tracking response for sender/receiver |
| GET | `/shipment/operations` | Staff/admin list, status filter and pagination |
| GET | `/shipment/operations/{id}` | Staff/admin operation detail |
| GET | `/shipment/couriers` | Staff/admin active courier options |
| PATCH | `/shipment/{id}/accept`, `/shipment/{id}/prepare` | Staff/admin ordered transitions |
| PATCH | `/shipment/{id}/assign-courier` | Staff/admin; body contains `courierId` |
| PATCH | `/shipment/{id}/deliver` | Assigned staff/admin delivery |
| GET | `/reports/shipments` | Admin totals and counts for all five statuses |

Reports exclude soft-deleted shipments and include zero counts for empty statuses. Shipment responses contain snapshot names/addresses and small participant/courier DTOs instead of full account objects. OpenAPI/Swagger routes are protected by the current security configuration; they are not public quick-start pages.

## Tests and packaging

```sh
bash scripts/test-isolated.sh
```

The command creates a temporary PostgreSQL schema, runs `./mvnw clean verify` and removes the schema on exit. It requires a database name ending in `_dev` or `_test`; tests should not be run directly against populated application tables. See [TESTING.md](TESTING.md) for the connection format and details.

The packaged application is `target/staj1-0.0.1-SNAPSHOT.jar`. It can be started from the repo root with:

```sh
java -jar target/staj1-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## Configuration and local integration

| Variable | Default | Purpose |
| --- | --- | --- |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated frontend origins allowed to call the API with the session cookie. `*` is rejected. |
| `AUTH_COOKIE_SECURE` | `false` | Set to `true` when the API is served over HTTPS. |
| `AUTH_COOKIE_SAME_SITE` | `Strict` | `Strict` or `Lax`. `None` is not supported because it would require CSRF tokens. |

The frontend reads the API address from `NEXT_PUBLIC_API_URL` (default `http://localhost:8080`). When either address changes, update both sides. Because the cookie is `SameSite`, the frontend and API must be on the same site (for example `app.example.com` and `api.example.com`, or two ports on `localhost`).

## Scope

This first version covers registration → creation → acceptance → preparation → courier assignment → delivery → tracking. Branch transfers, returns, redelivery, failed-delivery workflows, payments, SMS and maps are outside the first version.

## Author

Miray Bozdoğan · Computer Engineering Student

[GitHub](https://github.com/MirayBozdogan) · [LinkedIn](https://www.linkedin.com/in/miraybozdoğan)
