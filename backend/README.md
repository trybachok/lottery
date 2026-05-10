# Lottery Backend

Backend lottery system is a Java service that exposes a JSON REST API for client lottery workflows, administrative operations, payments, reports, auditing and UI configuration.

## What The Backend Does

The backend implements the core business logic of the lottery platform:

- User registration, password login and bearer token authentication.
- Role-based access control with backend as the source of truth.
- Administrative user, role and permission management.
- Draw lifecycle management: draft, scheduling, activation, sales closing, drawing, completion, cancellation and archival statuses.
- Ticket creation, bulk ticket creation, cancellation, soft deletion and result checking.
- Business rule enforcement for draws and tickets:
  - only `PAID` tickets participate in a draw;
  - a draw can be executed only once;
  - a completed draw result is immutable;
  - ownership checks protect client resources from IDOR.
- Payment invoice creation, provider webhook processing, invoice cancellation, expiration and refunds.
- Payment outbox worker for asynchronous provider-side actions.
- Operational reports for draws and tickets with JSON and CSV export.
- Audit log recording and audit log inspection.
- OpenAPI contract serving, protected so only system administrators can access it.
- Public Home page configuration API with active UI template and available themes.
- Administrative UI theme, UI template and system settings management.
- Health and readiness endpoints for container orchestration.

## Architecture

The service follows Clean Architecture with explicit dependency injection and no Spring/Spring Boot.

Main layers:

- `domain`: framework-independent business models, value objects, policies, services and repository interfaces.
- `application`: use cases, DTOs, mappers, authorization ports, transaction ports and validation logic.
- `infrastructure`: JDBC repositories, PostgreSQL/Flyway integration, security adapters, payment provider adapter, OpenAPI resource, configuration and runtime wiring.
- `presentation`: Servlet REST adapters, middleware, error handling and JSON request/response mapping.

Controllers/servlets are intentionally thin: they parse HTTP input, build use case context and call application use cases. Business rules live in domain/application code, not in repositories or HTTP adapters.

## API

Base path: `/api/v1`

The API is JSON-first and documented by OpenAPI:

- `POST /auth/register`
- `POST /auth/login`
- `/draws`
- `/tickets`
- `/invoices`
- `/payments`
- `/payment-providers`
- `/reports`
- `/admin/users`
- `/admin/roles`
- `/admin/permissions`
- `/admin/draws`
- `/admin/audit-logs`
- `/admin/settings/home-page`
- `/admin/ui-themes`
- `/admin/ui-templates`
- `GET /home-page`
- `GET /openapi.yaml`

OpenAPI is stored in `src/main/resources/openapi/openapi.yaml` and must be kept in sync with every API change.

## Security

Implemented security features:

- Password hashing with bcrypt.
- HMAC-signed access tokens.
- Backend-enforced RBAC permissions.
- Admin-only access to the OpenAPI document.
- Client ownership checks for client-owned resources.
- Prepared SQL statements in JDBC repositories.
- Validation of request data in application use cases.
- CORS configuration via environment variables.
- No secrets are stored in code; secrets are supplied through environment variables.

Important environment variables:

- `LOTTERY_ACCESS_TOKEN_SECRET`
- `LOTTERY_ACCESS_TOKEN_TTL_SECONDS`
- `LOTTERY_BCRYPT_COST`
- `LOTTERY_MOCK_PAYMENT_WEBHOOK_SECRET`
- `LOTTERY_CORS_ALLOWED_ORIGINS`
- `LOTTERY_JDBC_URL`
- `LOTTERY_JDBC_USER`
- `LOTTERY_JDBC_PASSWORD`

## Database

The backend uses PostgreSQL as the primary database.

Database management:

- Flyway migrations under `src/main/resources/db/migration`.
- UUID primary keys.
- `timestamptz` timestamps.
- JSONB fields for flexible schemas, draw results, UI templates, UI themes, audit snapshots and webhook payloads.
- Partitioned high-volume tables where required by the specification, including tickets, audit logs and webhook events.

Seeded data includes:

- System roles and permissions.
- Default UI themes.
- Default Home page template.
- Home page system setting.

## Payments

Payment integration is represented by a mock provider adapter suitable for local and staged development.

Implemented payment flows:

- Create invoice for ticket.
- Queue provider invoice creation in payment outbox.
- Process provider webhook events idempotently.
- Cancel and expire invoices.
- Queue and process refunds.
- Prevent duplicate payment processing through idempotency and webhook event tracking.

## UI Settings Backend

The backend provides system settings used by the frontend Home page:

- `GET /api/v1/home-page` returns the active Home template, default theme and all available themes.
- Admins can create and update UI themes.
- Admins can create and update Home page templates.
- Admins can choose the active Home template and default theme.

Template validation requires these regions:

- `header`
- `banner`
- `sidebar`
- `main`
- `footer`

Theme validation requires basic token structure with mode and color values.

## Technologies

Runtime and framework:

- Java 25
- Maven
- Jetty 12
- Jakarta Servlet API

Persistence:

- PostgreSQL
- JDBC
- HikariCP
- Flyway

Serialization and API:

- Jackson
- OpenAPI 3.1

Security:

- bcrypt
- HMAC token service

Logging and tests:

- SLF4J
- JUnit 5

Packaging and deployment:

- Maven Shade Plugin
- Docker multi-stage build
- Docker Compose

## Local Development

Run backend tests:

```bash
mvn -f backend/pom.xml test
```

Build the backend jar:

```bash
mvn -f backend/pom.xml package
```

Run the full local stack with Docker Compose:

```bash
LOTTERY_HOST_HTTP_PORT=8090 docker compose --env-file .env.example up -d --build
```

Useful local checks:

```bash
curl http://127.0.0.1:8090/health
curl http://127.0.0.1:8090/ready
curl http://127.0.0.1:8090/api/v1/home-page
```

The OpenAPI document is protected and requires an admin bearer token:

```bash
curl -H "Authorization: Bearer <ADMIN_TOKEN>" http://127.0.0.1:8090/api/v1/openapi.yaml
```

## Testing

The backend test suite covers:

- Authentication and token logic.
- User/admin RBAC use cases.
- Draw lifecycle rules.
- Draw execution and ticket participation rules.
- Ticket ownership and business workflows.
- Payment invoice, webhook and refund flows.
- Reports.
- OpenAPI contract smoke checks.
- Migration resource checks.
- UI settings, Home page template and theme business logic.

Run all backend tests before changing business logic or API contracts.
