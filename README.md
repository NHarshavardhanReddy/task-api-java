# Task Management API — Java (Spring Boot + Spring Data JPA)

REST API for managing tasks with JWT authentication, SQLite persistence (Spring Data JPA / Hibernate),
validation, pagination, Swagger docs, tests and Docker support.

## Stack
Java 17 · Spring Boot 3.3 · Spring Data JPA/Hibernate · SQLite · jjwt · springdoc-openapi · JUnit 5 + Mockito

## Run locally
Requires JDK 17+ and Maven 3.9+.
```bash
mvn spring-boot:run
```
- API: http://localhost:8081
- Swagger UI: http://localhost:8081/swagger-ui.html  ·  OpenAPI JSON: http://localhost:8081/v3/api-docs

## Run with Docker (one command)
```bash
docker compose up --build
```
Data persists in the `task-data` volume.

## Configuration (environment variables)
| Variable | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:sqlite:tasks.db` | JDBC URL |
| `APP_USERNAME` / `APP_PASSWORD` | `admin` / `admin123` | Demo login credentials |
| `JWT_SECRET` | dev value | **Set a long random value (>= 32 chars) in production** |
| `JWT_EXPIRATION_MINUTES` | `60` | Token lifetime |
| `PORT` | `8081` | HTTP port |

## Authentication
1. `POST /login` with `{"username":"admin","password":"admin123"}` → `{"accessToken":"...","tokenType":"bearer","expiresIn":3600}`
2. Send `Authorization: Bearer <token>` on all `/tasks` calls. Missing/invalid/expired token → `401`.

In Swagger UI: call `/login`, click **Authorize**, paste the token.

## Endpoints
| Method | Path | Description | Success |
|---|---|---|---|
| POST | `/login` | Get JWT | 200 |
| POST | `/tasks` | Create task | 201 |
| GET | `/tasks?status=&page=&limit=` | List (filter + pagination, default `page=1&limit=20`, max 100) | 200 |
| GET | `/tasks/{id}` | Get one | 200 |
| PUT | `/tasks/{id}` | Replace/update | 200 |
| DELETE | `/tasks/{id}` | Delete | 204 |

Task JSON: `id`, `title`, `description`, `status` (`TODO`/`IN_PROGRESS`/`DONE`), `dueDate` (`yyyy-MM-dd`), `createdAt`.

List response: `{"items":[...], "page":1, "limit":20, "total":42}`

## Errors
| Code | When |
|---|---|
| 400 | Invalid input (blank title, bad status/date, malformed JSON, bad `page`/`limit`) |
| 401 | Missing/invalid/expired token, bad login |
| 404 | Task not found |

```json
{"error": "Validation failed", "details": [{"field": "title", "message": "title must not be blank"}]}
```

## Examples
See `examples.sh` (curl): `./examples.sh http://localhost:8081`

## Tests
```bash
mvn test
```
- `TaskServiceTest` — unit tests with Mockito
- `TaskApiIntegrationTest` — MockMvc end-to-end tests against a real SQLite file (`target/test-tasks.db`)

## Deploy (Render / Railway / Fly.io)
Deploy from the Dockerfile (the app reads `$PORT`). Set `JWT_SECRET`, `APP_USERNAME`, `APP_PASSWORD`.
SQLite on free tiers may be ephemeral — attach a volume, or switch to a free Postgres
(add the `org.postgresql:postgresql` dependency and set `SPRING_DATASOURCE_URL`, `SPRING_JPA_DATABASE_PLATFORM`).

## Design notes
- Layers: `controller` → `service` (transactions, business rules) → `repository` (Spring Data JPA) → DB; DTO records keep the entity out of the API.
- `GlobalExceptionHandler` gives every error the same `{"error": ...}` shape.
- A servlet filter (`JwtAuthFilter`) guards every `/tasks/**` request, so new task endpoints are protected by default.
- SQLite is single-writer, so the Hikari pool is capped at 1 connection.

## Windows (PowerShell) quick start
Run these from the folder that contains `pom.xml`:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.19"      # your JDK folder (the one containing bin\javac.exe)
$env:Path = "$env:JAVA_HOME\bin;C:\tools\apache-maven-3.9.11\bin;" + $env:Path
mvn spring-boot:run
# second terminal:
.\examples.ps1
```
Default port is **8081**. Port busy? `$env:PORT = "8090"; mvn spring-boot:run`.
Troubleshooting: if Maven says "No plugin found for prefix 'spring-boot'", you are not in the folder with `pom.xml`.
