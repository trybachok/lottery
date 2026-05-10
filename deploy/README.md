# Lottery Deployment Notes

## Build

1. Copy `.env.example` to `.env`.
2. Replace every `CHANGE_ME` value with a server-specific secret.
3. Build artifacts and images:

```sh
./scripts/build-production.sh
```

## Start

```sh
docker compose up -d
docker compose ps
./scripts/smoke-test.sh
```

The compose stack runs:

- `postgres` with the named volume `postgres_data`;
- `lottery-backend`, with Flyway migrations enabled by `LOTTERY_DB_MIGRATIONS_ENABLED=true`;
- `lottery-frontend`, an Nginx SPA server bound to `127.0.0.1:${LOTTERY_HOST_HTTP_PORT}`.

## HTTPS Reverse Proxy

Use `deploy/nginx/lottery-https.conf` as the host Nginx site template.
Replace `lottery.example.com` and certificate paths with the real domain paths.
The template proxies HTTPS traffic to the frontend container through `127.0.0.1:8080`.

## Backups

PostgreSQL data lives in the Docker volume `postgres_data`.
Create a logical backup:

```sh
./scripts/backup-postgres.sh
```

Backups are written to `./backups`, which is intentionally ignored by git.
Run this script from cron or systemd timer on the server.

## Logging

Docker services use the `json-file` driver with size/count rotation configured by:

- `LOTTERY_LOG_MAX_SIZE`
- `LOTTERY_LOG_MAX_FILES`

For host Nginx logs, install `deploy/logrotate/lottery` into `/etc/logrotate.d/lottery`.

## Release Step

The default compose configuration runs migrations on backend startup.
For stricter releases, set `LOTTERY_DB_MIGRATIONS_ENABLED=false` for normal app containers and run one temporary backend container with migrations enabled before deployment.
