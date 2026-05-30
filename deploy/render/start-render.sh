#!/bin/sh
set -eu

PUBLIC_PORT="${PORT:-10000}"
BACKEND_PORT="${LOTTERY_HTTP_PORT:-8080}"
export LOTTERY_HTTP_PORT="$BACKEND_PORT"

sed \
    -e "s/__PUBLIC_PORT__/${PUBLIC_PORT}/g" \
    -e "s/__BACKEND_PORT__/${BACKEND_PORT}/g" \
    /app/nginx.conf.template > /tmp/nginx.conf

java -XX:MaxRAMPercentage=75 -jar /app/lottery-backend.jar &
JAVA_PID="$!"
nginx -c /tmp/nginx.conf -g 'daemon off;' &
NGINX_PID="$!"

shutdown() {
    kill "$JAVA_PID" "$NGINX_PID" 2>/dev/null || true
    wait "$JAVA_PID" 2>/dev/null || true
    wait "$NGINX_PID" 2>/dev/null || true
    exit 0
}

trap shutdown TERM INT

while true; do
    if ! kill -0 "$JAVA_PID" 2>/dev/null; then
        wait "$JAVA_PID" || exit "$?"
        exit 0
    fi
    if ! kill -0 "$NGINX_PID" 2>/dev/null; then
        wait "$NGINX_PID" || exit "$?"
        exit 0
    fi
    sleep 2
done
