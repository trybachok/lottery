#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

cd "$ROOT_DIR/backend"
mvn -DskipTests package

cd "$ROOT_DIR/frontend"
pnpm build:prod

cd "$ROOT_DIR"
docker compose build
