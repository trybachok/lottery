# Заметки по деплою Lottery

## Сборка

1. Скопируйте `.env.example` в `.env`.
2. Замените все значения `CHANGE_ME` на секреты для конкретного сервера.
3. Соберите артефакты и образы:

```sh
./scripts/build-production.sh
```

## Запуск

```sh
docker compose up -d
docker compose ps
./scripts/smoke-test.sh
```

Compose-стек запускает:

- `postgres` с именованным volume `postgres_data`;
- `lottery-backend` с миграциями Flyway, включенными через `LOTTERY_DB_MIGRATIONS_ENABLED=true`;
- `lottery-frontend`, Nginx-сервер для SPA, привязанный к `127.0.0.1:${LOTTERY_HOST_HTTP_PORT}`.

## HTTPS reverse proxy

Используйте `deploy/nginx/lottery-https.conf` как шаблон сайта для host Nginx.
Замените `lottery.example.com` и пути к сертификатам на реальные значения для домена.
Шаблон проксирует HTTPS-трафик во frontend-контейнер через `127.0.0.1:8080`.

## Резервные копии

Данные PostgreSQL хранятся в Docker volume `postgres_data`.
Создать логическую резервную копию можно так:

```sh
./scripts/backup-postgres.sh
```

Резервные копии записываются в `./backups`; эта директория намеренно игнорируется git.
Запускайте этот скрипт на сервере через cron или systemd timer.

## Логирование

Docker-сервисы используют драйвер `json-file` с ротацией по размеру и количеству файлов, которая настраивается переменными:

- `LOTTERY_LOG_MAX_SIZE`
- `LOTTERY_LOG_MAX_FILES`

Для логов host Nginx установите `deploy/logrotate/lottery` в `/etc/logrotate.d/lottery`.

## Релизный шаг

По умолчанию compose-конфигурация запускает миграции при старте backend.
Для более строгого релизного процесса установите `LOTTERY_DB_MIGRATIONS_ENABLED=false` для обычных app-контейнеров 
и перед деплоем запустите один временный backend-контейнер с включенными миграциями.

## Render Dashboard

В репозитории есть Render Blueprint в `render.yaml`.
Он разворачивает один Docker web-service с именем `lottery` и одну базу Render Postgres с именем `lottery-postgres`.

Корневой `Dockerfile` предназначен для Render и реализует простой Docker-подход к деплою:

- Maven собирает Java backend jar;
- pnpm собирает Vue frontend;
- runtime-образ запускает Java backend на внутреннем порту и Nginx на публичном `$PORT` от Render;
- Nginx раздает SPA и проксирует `/api/*` и `/ready` в backend;
- конфиг Nginx хранится в `deploy/render/nginx.conf.template`, а startup-скрипт только подставляет порты запуска.

Шаги деплоя:

1. Запушьте репозиторий в GitHub или GitLab.
2. Откройте `https://dashboard.render.com/`.
3. Создайте новый Blueprint из репозитория.
4. Подтвердите сервисы из `render.yaml`.
5. После первого деплоя откройте URL сервиса Render и зарегистрируйте первого пользователя `owner`.

Backend автоматически принимает `DATABASE_URL` от Render и преобразует его в JDBC-подключение PostgreSQL.
`PORT` от Render также используется, если `LOTTERY_HTTP_PORT` не задан.
