# ecom

Spring Boot e-commerce backend with JWT authentication, Redis cache/session helpers, Flyway migrations, and OpenAPI docs.

## Run

0. Prepare environment variables:

```bash
cp .env.example .env
set -a && source .env && set +a
```

1. Start infrastructure:

```bash
docker compose up -d postgres redis
```

2. Start app (dev profile is default):

```bash
mvn spring-boot:run
```

## Profiles

- `dev` (default): local defaults, Flyway enabled, `ddl-auto=validate`.
- `prod`: requires all DB/Redis/JWT env vars, Flyway enabled, `ddl-auto=validate`.

Set profile:

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

## API docs

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## API versioning

- Preferred endpoints use `/api/v1/...`
- Legacy endpoints `/auth` and `/user` are kept temporarily for backward compatibility

## Security hardening included

- Public signup always assigns role `USER` server-side.
- Login lockout after repeated failed attempts.
- Refresh token endpoint rate-limited per client IP.
- JWT secret rotation support via `APP_JWT_PREVIOUS_SECRETS` (comma-separated old secrets still accepted for validation).

## Environment variables

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `SPRING_DATA_REDIS_PASSWORD`
- `APP_JWT_SECRET`
- `APP_JWT_PREVIOUS_SECRETS` (optional)
- `APP_SECURITY_CORS_ALLOWED_ORIGINS`
