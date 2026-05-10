# Lottery System

Lottery System - это fullstack-приложение для управления онлайн-лотереей. Система включает клиентскую часть, административную панель, backend API, PostgreSQL и Docker-инфраструктуру для локального запуска.

## Что Реализовано

Основные возможности системы:

- регистрация и вход пользователей;
- RBAC: роли `ADMIN`, `MANAGER`, `CLIENT` и набор permission-кодов;
- клиентские сценарии просмотра розыгрышей, покупки билетов, просмотра билетов, invoice/payment статусов и результатов;
- управление пользователями, ролями и permissions в админ-панели;
- управление розыгрышами и назначение менеджера;
- создание билетов и bulk-создание билетов через API;
- invoice/payment/refund/webhook flows через mock payment provider;
- выполнение розыгрыша только по оплаченным билетам;
- защита от повторного запуска розыгрыша;
- неизменяемый результат после завершения розыгрыша;
- отчеты по розыгрышам и билетам;
- аудит административных действий;
- защищенная страница API-документации `/docs`;
- настраиваемая Home page через шаблоны;
- светлая и темная темы с сохранением выбора в браузере;
- Docker-сборка frontend, backend и PostgreSQL.

## Стек

Backend:

- Java 25
- Maven
- Jetty 12
- Jakarta Servlet API
- PostgreSQL
- JDBC + HikariCP
- Flyway migrations
- Jackson
- OpenAPI 3.1
- bcrypt
- HMAC access tokens
- SLF4J
- JUnit 5

Frontend:

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Axios
- Swagger UI
- Storybook
- `@hey-api/openapi-ts`
- Nginx для production runtime

Infrastructure:

- Docker
- Docker Compose
- PostgreSQL volume
- healthchecks
- JSON-file log rotation для контейнеров

## Структура Проекта

```text
.
├── backend/                 # Java backend, REST API, business logic, migrations
├── frontend/                # Vue frontend, admin panel, client UI
├── deploy/                  # deploy templates: env, nginx, backup, smoke test
├── scripts/                 # helper scripts for build, backup, smoke test
├── docker-compose.yml       # local/full stack compose
├── .env.example             # example env without real secrets
```

Дополнительные README:

- `backend/README.md` - описание backend-части.
- `frontend/README.md` - описание frontend-части.
- `deploy/README.md` - production/deploy notes.

## Быстрый Локальный Запуск В Docker

### 1. Требования

Установите:

- Docker Desktop или Docker Engine;
- Docker Compose plugin;
- `curl` для smoke-проверок.

Опционально для разработки без Docker:

- Java 25;
- Maven;
- Node.js;
- pnpm.

### 2. Запустите Стек

Из корня проекта:

```bash
LOTTERY_HOST_HTTP_PORT=8090 docker compose --env-file .env.example up -d --build
```

Почему `8090`: локальный порт `8080` часто уже занят. Если `8080` свободен, можно использовать значение из `.env.example`:

```bash
docker compose --env-file .env.example up -d --build
```

Compose поднимет:

- `postgres`;
- `lottery-backend`;
- `lottery-frontend`.

### 3. Проверьте Статус

```bash
docker compose --env-file .env.example ps
```

Ожидаемый результат: все сервисы `healthy`.

Проверьте frontend health:

```bash
curl http://127.0.0.1:8090/health
```

Проверьте backend API через Nginx proxy:

```bash
curl http://127.0.0.1:8090/api/v1/home-page
```

### 4. Откройте Приложение

Откройте в браузере:

```text
http://127.0.0.1:8090
```

Вы увидите Home page, которая загружается из backend-настроек. На странице доступен переключатель темы Light/Dark.

## Первый Вход И Первый Администратор

Миграции создают роли и permissions, но не создают пользователя-администратора. Первый администратор создается автоматически при первой регистрации пользователя с логином `owner`, если в системе еще нет пользователя с ролью `ADMIN`.

1. Откройте приложение.
2. Перейдите на `/register`.
3. Зарегистрируйте пользователя, например:

```text
email: owner@example.com
login: owner
password: admin-password
```

4. Войдите под этим пользователем.

После входа будут доступны:

- `/admin`;
- `/docs`;
- административные разделы.

Важно: автоматическое назначение `ADMIN` работает только для первого пользователя с логином `owner`, пока в системе еще нет роли `ADMIN` у другого пользователя. Все остальные регистрации получают роль `CLIENT`.

## Как Пользоваться Системой

### Главная Страница И Темы

Откройте `/`.

На Home page:

- отображается шаблон, загруженный из backend;
- есть header, banner, sidebar, main и footer;
- можно переключить тему Light/Dark;
- выбранная тема сохраняется в браузере.

Администратор может менять:

- активный шаблон: `/admin/settings`;
- темы: `/admin/ui-themes`;
- шаблоны Home page: `/admin/ui-templates`.

### Регистрация И Вход

Клиентский пользователь:

1. Открывает `/register`.
2. Создает аккаунт.
3. Входит через `/login`.
4. Получает роль `CLIENT`.

Backend остается источником прав доступа: frontend route guards только улучшают UX.

### Админ-Панель

Откройте:

```text
http://127.0.0.1:8090/admin
```

Доступные разделы:

- `Users` - управление пользователями;
- `Roles` - управление ролями;
- `Permissions` - управление permissions;
- `Draws` - создание и управление розыгрышами;
- `Reports` - отчеты по розыгрышам и билетам;
- `Audit` - аудит действий;
- `Settings` - настройки Home page;
- `UI Themes` - управление темами;
- `UI Templates` - управление шаблонами.

### API Документация

Swagger UI доступен по адресу:

```text
http://127.0.0.1:8090/docs
```

Важно:

- страница `/docs` доступна только пользователю с ролью `ADMIN`;
- backend endpoint `/api/v1/openapi.yaml` также защищен;
- Swagger UI использует текущий access token пользователя.

### Подготовка Данных Для Розыгрыша

В текущем интерфейсе создание combination schema, prizes и winning rules еще не вынесено в отдельные UI-разделы. Для локальной проверки розыгрышей можно добавить базовую combination schema через SQL.

Создайте схему комбинации:

```bash
docker compose --env-file .env.example exec postgres psql -U lottery -d lottery -c "insert into combination_schemas (id, name, schema_json, created_at) values ('11111111-1111-1111-1111-111111111111', 'Four numbers 1-50', '{\"positions\":[{\"type\":\"NUMBER\",\"min\":1,\"max\":50},{\"type\":\"NUMBER\",\"min\":1,\"max\":50},{\"type\":\"NUMBER\",\"min\":1,\"max\":50},{\"type\":\"NUMBER\",\"min\":1,\"max\":50}],\"allowDuplicates\":false,\"orderSensitive\":false}'::jsonb, now()) on conflict (id) do nothing;"
```

После этого в `/admin/draws` можно создать розыгрыш, указав:

```text
combinationSchemaId: 11111111-1111-1111-1111-111111111111
```

Для полного запуска draw execution backend также требует winning rules. После создания розыгрыша скопируйте его `drawId` и добавьте prize + winning rule:

```bash
docker compose --env-file .env.example exec postgres psql -U lottery -d lottery -c "insert into prizes (id, type, name, amount, currency) values ('22222222-2222-2222-2222-222222222222', 'MONEY', 'Main prize', 1000.00, 'RUB') on conflict (id) do nothing;"
```

```bash
docker compose --env-file .env.example exec postgres psql -U lottery -d lottery -c "insert into winning_rules (id, draw_id, match_percent_from, match_percent_to, prize_id, priority) values (gen_random_uuid(), '<DRAW_ID>', 100.00, 100.00, '22222222-2222-2222-2222-222222222222', 1);"
```

Замените `<DRAW_ID>` на UUID созданного розыгрыша.

### Создание И Оплата Билета

Клиентский сценарий:

1. Зарегистрируйтесь или войдите как клиент.
2. Откройте `/draws`.
3. Откройте нужный розыгрыш.
4. Создайте билет, указав комбинацию, например:

```text
12, 18, 27, 34
```

5. Откройте ticket details.
6. Создайте invoice.
7. Для локального mock provider обработка платежа выполняется backend outbox worker-ом.

Участие в розыгрыше разрешено только билетам со статусом `PAID`, у которых подтвержден provider payment.

### Запуск Розыгрыша

Розыгрыш можно запустить только в статусе `SALES_CLOSED`.

Lifecycle endpoints доступны через API:

- `POST /api/v1/draws/{drawId}/activate`
- `POST /api/v1/draws/{drawId}/pause`
- `POST /api/v1/draws/{drawId}/postpone`
- `POST /api/v1/draws/{drawId}/close-sales`
- `POST /api/v1/draws/{drawId}/run`
- `POST /api/v1/draws/{drawId}/cancel`
- `POST /api/v1/draws/{drawId}/archive`

Удобнее всего вызывать их через `/docs` под admin-пользователем.

## Полезные Команды

Остановить стек:

```bash
docker compose --env-file .env.example down
```

Остановить стек и удалить volume PostgreSQL:

```bash
docker compose --env-file .env.example down -v
```

Посмотреть логи backend:

```bash
docker compose --env-file .env.example logs -f lottery-backend
```

Посмотреть логи frontend:

```bash
docker compose --env-file .env.example logs -f lottery-frontend
```

Пересобрать и перезапустить:

```bash
LOTTERY_HOST_HTTP_PORT=8090 docker compose --env-file .env.example up -d --build
```

## Разработка Без Docker

Backend tests:

```bash
mvn -f backend/pom.xml test
```

Backend package:

```bash
mvn -f backend/pom.xml package
```

Frontend dependencies:

```bash
pnpm --dir frontend install
```

Frontend dev server:

```bash
pnpm --dir frontend dev
```

Frontend production build:

```bash
pnpm --dir frontend build:prod
```

Regenerate frontend API client:

```bash
pnpm --dir frontend api:generate
```

Storybook:

```bash
pnpm --dir frontend storybook
```

## Проверки Перед Завершением Работы

Рекомендуемый минимум:

```bash
mvn -f backend/pom.xml test
pnpm --dir frontend api:generate
pnpm --dir frontend build:prod
```

Для UI-компонентов:

```bash
pnpm --dir frontend build-storybook
```

Для Docker smoke:

```bash
LOTTERY_HOST_HTTP_PORT=8090 docker compose --env-file .env.example up -d --build
curl http://127.0.0.1:8090/health
curl http://127.0.0.1:8090/api/v1/home-page
```

## Переменные Окружения

Для локального Docker-запуска используется `.env.example`.

Ключевые переменные:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `LOTTERY_DB_MIGRATIONS_ENABLED`
- `LOTTERY_BCRYPT_COST`
- `LOTTERY_ACCESS_TOKEN_TTL_SECONDS`
- `LOTTERY_ACCESS_TOKEN_SECRET`
- `LOTTERY_MOCK_PAYMENT_WEBHOOK_SECRET`
- `LOTTERY_CORS_ALLOWED_ORIGINS`
- `LOTTERY_FRONTEND_BASE_URL`
- `LOTTERY_BACKEND_BASE_URL`
- `VITE_API_BASE_URL`
- `LOTTERY_HOST_HTTP_PORT`

Для production нельзя использовать `CHANGE_ME` значения из `.env.example`. Создайте отдельный `.env` на сервере и заполните реальные секреты.

## Текущие Особенности И Ограничения

- Первый admin пользователь не seed-ится миграциями: зарегистрируйте пользователя с логином `owner`, пока в системе еще нет пользователя с ролью `ADMIN`, и backend автоматически назначит ему роль `ADMIN`.
- UI для combination schemas, prizes и winning rules еще не выделен в отдельные админ-разделы; для полного сценария розыгрыша эти данные можно добавить через SQL или API/DB tooling.
- Frontend guards не заменяют backend RBAC: все реальные проверки прав выполняет backend.
- `/docs` и `/api/v1/openapi.yaml` доступны только администратору.
- Docker Compose публикует frontend на `127.0.0.1`, что подходит для локального запуска и production-схемы с reverse proxy.
