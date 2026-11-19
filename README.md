# PayFlow

A simulated payment-processing REST API built with Java 17, Spring Boot, Spring Data JPA and PostgreSQL. It does **not** move actual money or connect to banks/payment networks.

## What’s included

- Create payments with amount, currency, customer ID and method (`UPI`, `CARD`, `WALLET`).
- Validate request fields and positive amounts (maximum two decimal places).
- Generate transaction IDs, persist status and timestamps, and return stable results for repeated idempotency keys.
- Retrieve a transaction; list transactions with pagination and status/method filters.
- Analytics for successful payment count, revenue, failure rate and per-method totals.
- Configurable API-key authentication, structured error responses, health endpoint and Swagger/OpenAPI.
- PostgreSQL schema constraints/indexes, JUnit/Mockito service tests, Docker Compose, Dockerfile, Render Blueprint and Postman collection.

## Architecture

`controller` receives HTTP requests and applies DTO validation. `service` coordinates payment creation, transaction reads and analytics. `SimulatedGateway` decides success/failure without an external processor. `repository` uses Spring Data JPA to persist `Payment` entities in PostgreSQL. The API key is checked statelessly on every `/api/v1/**` request.

The demo gateway deterministically fails amounts ending in `.99`; other valid amounts succeed. This behavior is for exercising both outcomes and does not model a real processor.

## Run locally with Docker Compose

Requirements: Docker Desktop with Compose. From the repository root in PowerShell:

```powershell
$env:PAYFLOW_API_KEY = [guid]::NewGuid().ToString('N')
$env:DB_PASSWORD = [guid]::NewGuid().ToString('N')
docker compose up --build
```

The API listens on `http://localhost:8080`. Stop containers with `Ctrl+C`; run `docker compose down` to stop/remove them. The named `postgres_data` volume keeps database data. To delete the data too, run `docker compose down -v`.

## Run with local Java and PostgreSQL

Requirements: JDK 17+, Maven 3.9+, PostgreSQL 14+. Create a PostgreSQL database and user named `payflow`, then set environment variables and start from the repository root:

```powershell
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/payflow'
$env:DB_USERNAME = 'payflow'
$env:DB_PASSWORD = '<your-database-password>'
$env:PAYFLOW_API_KEY = '<a-long-random-key>'
mvn test
mvn spring-boot:run
```

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/payflow` | JDBC URL for PostgreSQL |
| `DB_USERNAME` | empty | Database user |
| `DB_PASSWORD` | empty | Database password |
| `PAYFLOW_API_KEY` | empty (API requests return 401) | Secret sent in `X-API-Key` |
| `PORT` | `8080` | HTTP port |

Use secret/environment-variable configuration on deployment hosts. Compose has a `local-dev-password` fallback for the database only; set `DB_PASSWORD` explicitly for local use. The application intentionally does not accept API requests until `PAYFLOW_API_KEY` is configured.

## API and examples

All `/api/v1/**` endpoints require `X-API-Key`. Payment creation also requires an `Idempotency-Key` header of 1–100 characters.

Create a payment:

```http
POST /api/v1/payments
X-API-Key: <your-key>
Idempotency-Key: order-123-attempt-1
Content-Type: application/json

{"amount":100.00,"currency":"INR","customerId":"customer-123","paymentMethod":"UPI"}
```

Use `99.99` as the amount to see the simulated `FAILED` result. Reusing an idempotency key returns the original payment, including its original result.

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/payments` | Create a simulated payment (`201`) |
| `GET` | `/api/v1/payments/{transactionId}` | Fetch a transaction |
| `GET` | `/api/v1/payments?page=0&size=20&status=SUCCESS&paymentMethod=UPI` | Paginated history with optional filters |
| `GET` | `/api/v1/analytics` | Counts, revenue, failure rate and method statistics |
| `GET` | `/actuator/health` | Health status; no API key required |

Swagger UI: `http://localhost:8080/swagger-ui/index.html`  
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Import `postman/PayFlow.postman_collection.json` into Postman. Set collection variables `baseUrl` and `apiKey`. Set `transactionId` to a returned ID to try the retrieval request.

Errors use JSON with timestamp, HTTP status, error name, message, path and validation details. Missing/invalid API keys return HTTP 401; invalid request data returns HTTP 400; unknown transaction IDs return HTTP 404.

## Tests and build

Run tests and package the executable JAR with:

```powershell
mvn test
mvn package
```

The service tests cover payment creation, idempotent replay, retrieval/not-found behavior and analytics. DTO validation is enforced by Bean Validation annotations.

## Deploy to Render

1. Push the project to a Git provider.
2. In Render, choose **New → Blueprint** and connect the repository containing `render.yaml`.
3. Review and apply the Blueprint. It defines a Docker web service and PostgreSQL database, and generates `PAYFLOW_API_KEY`.
4. Wait until the service health check at `/actuator/health` is healthy.
5. Open `https://<your-render-service>/swagger-ui/index.html`; use the generated `PAYFLOW_API_KEY` as the `X-API-Key` value in Postman or requests.

For another container host, build from the included `Dockerfile` and configure `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PAYFLOW_API_KEY`, and optionally `PORT`.

## Verification status and limitations

In the development environment used to create this project, Maven and Docker were unavailable, so Maven tests, packaging, Docker build and runtime startup could not be verified there. Run the commands above in an environment with Maven/JDK or Docker before relying on a deployment.

Idempotency currently returns the original transaction if a key is reused, even when the second request body differs. Hibernate `ddl-auto=update` is convenient for this demo; production schema changes should use a migration tool. API keys are shared by callers and there is no rotation or tenant management. The gateway remains a deterministic simulation.
