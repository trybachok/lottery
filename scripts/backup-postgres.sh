#!/usr/bin/env sh
set -eu

COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-lottery}"
BACKUP_DIR="${LOTTERY_BACKUP_DIR:-./backups}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_DIR"

docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom' \
    > "$BACKUP_DIR/${COMPOSE_PROJECT}_postgres_${TIMESTAMP}.dump"

printf '%s\n' "Backup written to $BACKUP_DIR/${COMPOSE_PROJECT}_postgres_${TIMESTAMP}.dump"
