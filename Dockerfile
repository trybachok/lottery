FROM maven:3.9.11-eclipse-temurin-25-alpine AS backend-build
WORKDIR /workspace

COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f backend/pom.xml -DskipTests package

FROM node:24-alpine AS frontend-build

ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH

RUN corepack enable
WORKDIR /workspace/frontend

COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN --mount=type=cache,id=pnpm,target=/pnpm/store \
    pnpm install --frozen-lockfile

COPY frontend ./
COPY backend/src/main/resources/openapi /workspace/backend/src/main/resources/openapi
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN pnpm api:generate && pnpm build

FROM eclipse-temurin:25-jre-alpine

RUN apk add --no-cache nginx && \
    addgroup -S lottery && \
    adduser -S lottery -G lottery && \
    mkdir -p /app /run/nginx /var/lib/nginx /var/log/nginx /usr/share/nginx/html && \
    chown -R lottery:lottery /app /run/nginx /var/lib/nginx /var/log/nginx /usr/share/nginx/html

WORKDIR /app

COPY --from=backend-build /workspace/backend/target/lottery-backend-0.1.0-SNAPSHOT.jar /app/lottery-backend.jar
COPY --from=frontend-build /workspace/frontend/dist /usr/share/nginx/html
COPY deploy/render/start-render.sh /app/start-render.sh
COPY deploy/render/nginx.conf.template /app/nginx.conf.template

RUN chmod +x /app/start-render.sh && chown lottery:lottery /app/start-render.sh

ENV PORT=10000
ENV LOTTERY_HTTP_PORT=8080
ENV LOTTERY_DB_MIGRATIONS_ENABLED=true

EXPOSE 10000

USER lottery
ENTRYPOINT ["/app/start-render.sh"]
