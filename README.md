# Lottery System

Lottery System - это fullstack-приложение для управления онлайн-лотереей. 
Система включает клиентскую часть, административную панель, backend API, PostgreSQL и 
Docker-инфраструктуру для локального запуска.

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

## Локальный запуск в Docker

### 1. Требования

Необходимое ПО и инструменты:

- Docker Desktop или Docker Engine;
- Docker Compose plugin;
- `curl` для smoke-проверок.

### 2. Запустите сборку образов и запуск сервисов

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

### 3. Проверьте статус

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

### 4. Откройте приложение

Откройте в браузере:

```text
http://127.0.0.1:8090
```

Вы увидите главную публичную страницу системы, которая загружается из backend-настроек. 
На странице доступен переключатель темы Light/Dark, функционал администратора и другие возможности.

## Первый вход и пользователь-администратор

Миграции создают роли и разрешения, но не создают пользователя-администратора. 
Первый администратор создается автоматически, если самый первый зарегистрированный пользователь имеет логин `owner`.

1. Откройте приложение.
2. Перейдите на `/register`.
3. Зарегистрируйте пользователя, например:

```text
email: owner@example.com
login: owner
password: *****
```

4. Войдите под этим пользователем.

После входа будут доступны:

- `/admin` - административные разделы.
- `/docs` - документация api.

Важно: автоматическое назначение `ADMIN` работает только для самого первого пользователя 
в пустой базе и только при логине `owner`. Все остальные регистрации получают роль `CLIENT`.

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

## Полезные системные команды

Остановить сервисы:

```bash
docker compose --env-file .env.example down
```

Остановить сервисы и удалить volume PostgreSQL:

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

Пересобрать и перезапустить сервисы:

```bash
LOTTERY_HOST_HTTP_PORT=8090 docker compose --env-file .env.example up -d --build
```

## Разработка без Docker

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

## Проверки работоспособности приложений

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

Для production нельзя использовать `CHANGE_ME` значения из `.env.example`. 
Создайте отдельный `.env` на сервере и заполните реальные секреты.

## Текущие Особенности И Ограничения

- Первый admin пользователь не seed-ится миграциями: зарегистрируйте первым пользователя с логином `owner`, и backend автоматически назначит ему роль `ADMIN`.
- UI для combination schemas, prizes и winning rules еще не выделен в отдельные админ-разделы; для полного сценария розыгрыша эти данные можно добавить через SQL или API/DB tooling.
- Frontend guards не заменяют backend RBAC: все реальные проверки прав выполняет backend.
- `/docs` и `/api/v1/openapi.yaml` доступны только администратору.
- Docker Compose публикует frontend на `127.0.0.1`, что подходит для локального запуска и production-схемы с reverse proxy.

Ниже инструкция для **Сценария 1: базовая лотерея**. 
Важно: в текущей версии проекта часть действий есть в UI, 
но часть полного lifecycle пока удобнее выполнять через `curl` и SQL, 
потому что в админке нет отдельных экранов для `combination_schemas`, `prizes`, `winning_rules`, 
а также нет кнопок `Activate` / `Close sales`.

# Сценарий 1. Базовая лотерея

Цель сценария:

```text
→ создание тиража
→ получение списка активных тиражей
→ создание билета
→ оплата билета через mock payment
→ закрытие продаж
→ генерация выигрышной комбинации
→ проверка результата билета
→ отображение статуса билета WIN или LOSE
```

---

## 1. Запустить проект

Из корня проекта:

```bash
LOTTERY_HOST_HTTP_PORT=8090 docker compose --env-file .env.example up -d --build
```

Проверить, что контейнеры поднялись:

```bash
docker compose --env-file .env.example ps
```

Проверить frontend:

```bash
curl http://127.0.0.1:8090/health
```

Проверить backend API:

```bash
curl http://127.0.0.1:8090/api/v1/home-page
```

Дальше будем использовать базовый адрес:

```bash
export BASE="http://127.0.0.1:8090/api/v1"
```

---

## 2. Создать первого администратора

Первый пользователь с логином `owner` автоматически получает роль `ADMIN`.

```bash
curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@example.com",
    "login": "owner",
    "password": "admin-password"
  }' | jq
```

Залогиниться под админом:

```bash
ADMIN_LOGIN_RESPONSE=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "loginOrEmail": "owner",
    "password": "admin-password"
  }')

export ADMIN_TOKEN=$(echo "$ADMIN_LOGIN_RESPONSE" | jq -r '.accessToken')
export ADMIN_USER_ID=$(echo "$ADMIN_LOGIN_RESPONSE" | jq -r '.user.id')

echo "$ADMIN_TOKEN"
echo "$ADMIN_USER_ID"
```

---

## 3. Создать обычного клиента

```bash
curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "client@example.com",
    "login": "client",
    "password": "client-password"
  }' | jq
```

Залогиниться под клиентом:

```bash
CLIENT_LOGIN_RESPONSE=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "loginOrEmail": "client",
    "password": "client-password"
  }')

export CLIENT_TOKEN=$(echo "$CLIENT_LOGIN_RESPONSE" | jq -r '.accessToken')
export CLIENT_USER_ID=$(echo "$CLIENT_LOGIN_RESPONSE" | jq -r '.user.id')

echo "$CLIENT_TOKEN"
echo "$CLIENT_USER_ID"
```

---

## 4. Подготовить схему комбинации

В UI пока нет отдельного экрана для создания схемы комбинации, поэтому добавим её напрямую в БД.

Для демонстрации создадим простую схему из двух чисел:

```text
первое число всегда 7
второе число 1 или 2
```

Это нужно, чтобы гарантированно получить один билет `WIN`, а второй `LOSE`.

```bash
export COMBINATION_SCHEMA_ID="11111111-1111-1111-1111-111111111111"

docker compose --env-file .env.example exec -T postgres psql -U lottery -d lottery <<SQL
insert into combination_schemas (
  id,
  name,
  schema_json,
  created_at
)
values (
  '$COMBINATION_SCHEMA_ID',
  'Demo schema: 7 + 1..2',
  '{
    "positions": [
      {
        "type": "NUMBER",
        "min": 7,
        "max": 7
      },
      {
        "type": "NUMBER",
        "min": 1,
        "max": 2
      }
    ],
    "allowDuplicates": false,
    "orderSensitive": true
  }'::jsonb,
  now()
)
on conflict (id) do nothing;
SQL
```

---

## 5. Создать тираж

Создадим тираж через API от имени администратора.

Сначала подготовим даты:

```bash
export SALES_START_AT=$(python3 - <<'PY'
from datetime import datetime, timezone, timedelta
print((datetime.now(timezone.utc) - timedelta(minutes=5)).isoformat().replace("+00:00", "Z"))
PY
)

export SALES_END_AT=$(python3 - <<'PY'
from datetime import datetime, timezone, timedelta
print((datetime.now(timezone.utc) + timedelta(hours=1)).isoformat().replace("+00:00", "Z"))
PY
)

export DRAW_AT=$(python3 - <<'PY'
from datetime import datetime, timezone, timedelta
print((datetime.now(timezone.utc) + timedelta(hours=2)).isoformat().replace("+00:00", "Z"))
PY
)

echo "$SALES_START_AT"
echo "$SALES_END_AT"
echo "$DRAW_AT"
```

Создать тираж:

```bash
DRAW_RESPONSE=$(curl -s -X POST "$BASE/draws" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{
    \"title\": \"Demo basic lottery\",
    \"description\": \"Basic lifecycle demo draw\",
    \"combinationSchemaId\": \"$COMBINATION_SCHEMA_ID\",
    \"salesStartAt\": \"$SALES_START_AT\",
    \"salesEndAt\": \"$SALES_END_AT\",
    \"drawAt\": \"$DRAW_AT\",
    \"maxTickets\": 100,
    \"test\": false
  }")

echo "$DRAW_RESPONSE" | jq

export DRAW_ID=$(echo "$DRAW_RESPONSE" | jq -r '.id')
echo "$DRAW_ID"
```

После создания обычный тираж получает статус:

```text
DRAFT
```

Для продажи билетов его нужно перевести в `ACTIVE`.

---

## 6. Активировать тираж

```bash
curl -s -X POST "$BASE/draws/$DRAW_ID/activate" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

Проверить статус:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/draws/$DRAW_ID" | jq '{id, title, status, salesStartAt, salesEndAt, drawAt}'
```

Ожидаемый статус:

```text
ACTIVE
```

---

## 7. Добавить приз и правило выигрыша

Для запуска розыгрыша backend требует наличие `winning_rules`.

В UI этого пока нет, поэтому добавляем через SQL.

Правило будет таким:

```text
если совпадение 100% → билет WIN
иначе → билет LOSE
```

```bash
export PRIZE_ID="22222222-2222-2222-2222-222222222222"

docker compose --env-file .env.example exec -T postgres psql -U lottery -d lottery <<SQL
insert into prizes (
  id,
  type,
  name,
  amount,
  currency
)
values (
  '$PRIZE_ID',
  'MONEY',
  'Demo main prize',
  1000.00,
  'RUB'
)
on conflict (id) do nothing;

delete from winning_rules
where draw_id = '$DRAW_ID';

insert into winning_rules (
  id,
  draw_id,
  match_percent_from,
  match_percent_to,
  prize_id,
  priority
)
values (
  gen_random_uuid(),
  '$DRAW_ID',
  100.00,
  100.00,
  '$PRIZE_ID',
  1
);
SQL
```

---

## 8. Получить список активных тиражей

Через API от имени клиента:

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/draws?limit=20&offset=0" \
  | jq '.items[] | select(.status == "ACTIVE") | {
      id,
      title,
      status,
      salesStartAt,
      salesEndAt,
      drawAt
    }'
```

Важно: текущий endpoint `/draws` возвращает все неудалённые тиражи, а не только активные. Поэтому для активных нужно смотреть `status == "ACTIVE"`.

Через UI:

```text
http://127.0.0.1:8090/draws
```

В списке нужно выбрать тираж со статусом `ACTIVE`.

---

## 9. Создать билеты

Для демонстрации создадим два билета:

```text
билет 1: 7, 1
билет 2: 7, 2
```

Так как выигрышная комбинация будет либо `7, 1`, либо `7, 2`, один билет станет `WIN`, а второй `LOSE`.

### Создать первый билет

```bash
TICKET_1_RESPONSE=$(curl -s -X POST "$BASE/tickets" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -d "{
    \"userId\": \"$CLIENT_USER_ID\",
    \"drawId\": \"$DRAW_ID\",
    \"combinationValues\": [\"7\", \"1\"],
    \"priceAmount\": \"100.00\",
    \"priceCurrency\": \"RUB\",
    \"test\": false
  }")

echo "$TICKET_1_RESPONSE" | jq

export TICKET_1_ID=$(echo "$TICKET_1_RESPONSE" | jq -r '.id')
echo "$TICKET_1_ID"
```

### Создать второй билет

```bash
TICKET_2_RESPONSE=$(curl -s -X POST "$BASE/tickets" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -d "{
    \"userId\": \"$CLIENT_USER_ID\",
    \"drawId\": \"$DRAW_ID\",
    \"combinationValues\": [\"7\", \"2\"],
    \"priceAmount\": \"100.00\",
    \"priceCurrency\": \"RUB\",
    \"test\": false
  }")

echo "$TICKET_2_RESPONSE" | jq

export TICKET_2_ID=$(echo "$TICKET_2_RESPONSE" | jq -r '.id')
echo "$TICKET_2_ID"
```

После создания билеты должны иметь статус:

```text
CREATED
```

Проверить:

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets?userId=$CLIENT_USER_ID&limit=50&offset=0" \
  | jq '.items[] | select(.drawId == env.DRAW_ID) | {
      id,
      status,
      combinationValues,
      priceAmount,
      priceCurrency
    }'
```

---

## 10. Создать invoice для оплаты билетов

В проекте розыгрыш учитывает только билеты со статусом `PAID`. Поэтому перед запуском тиража нужно создать invoice и подтвердить оплату mock webhook’ом.

### Создать invoice для первого билета

```bash
INVOICE_1_RESPONSE=$(curl -s -X POST "$BASE/tickets/$TICKET_1_ID/invoice" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -d "{
    \"providerCode\": \"mock\",
    \"idempotencyKey\": \"invoice-$TICKET_1_ID-$(date +%s)\"
  }")

echo "$INVOICE_1_RESPONSE" | jq
```

### Создать invoice для второго билета

```bash
INVOICE_2_RESPONSE=$(curl -s -X POST "$BASE/tickets/$TICKET_2_ID/invoice" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -d "{
    \"providerCode\": \"mock\",
    \"idempotencyKey\": \"invoice-$TICKET_2_ID-$(date +%s)\"
  }")

echo "$INVOICE_2_RESPONSE" | jq
```

После создания invoice билет переходит в статус:

```text
PAYMENT_PENDING
```

---

## 11. Дождаться обработки payment outbox

Backend worker обрабатывает создание invoice у mock provider примерно раз в 10 секунд.

Подождать:

```bash
sleep 12
```

Проверить invoice первого билета:

```bash
INVOICE_1_CURRENT=$(curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_1_ID/invoice")

echo "$INVOICE_1_CURRENT" | jq
```

Проверить invoice второго билета:

```bash
INVOICE_2_CURRENT=$(curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_2_ID/invoice")

echo "$INVOICE_2_CURRENT" | jq
```

Нужно, чтобы появились поля:

```text
externalInvoiceId
paymentUrl
```

И статус invoice стал:

```text
PENDING
```

Сохраним external invoice id:

```bash
export EXTERNAL_INVOICE_1_ID=$(echo "$INVOICE_1_CURRENT" | jq -r '.externalInvoiceId')
export EXTERNAL_INVOICE_2_ID=$(echo "$INVOICE_2_CURRENT" | jq -r '.externalInvoiceId')

echo "$EXTERNAL_INVOICE_1_ID"
echo "$EXTERNAL_INVOICE_2_ID"
```

---

## 12. Подтвердить оплату через mock webhook

В реальной системе оплату подтвердил бы внешний платёжный провайдер. В этом проекте используется mock provider, поэтому вручную отправляем webhook `PAYMENT_SUCCEEDED`.

Сначала взять secret из `.env.example`:

```bash
export WEBHOOK_SECRET=$(grep '^LOTTERY_MOCK_PAYMENT_WEBHOOK_SECRET=' .env.example | cut -d= -f2-)
echo "$WEBHOOK_SECRET"
```

Создадим helper-функцию:

```bash
send_payment_succeeded_webhook() {
  local external_invoice_id="$1"
  local event_id="evt-${external_invoice_id}-$(date +%s)-$RANDOM"

  local payload="{\"eventId\":\"${event_id}\",\"eventType\":\"PAYMENT_SUCCEEDED\",\"externalInvoiceId\":\"${external_invoice_id}\"}"

  local signature
  signature=$(printf '%s' "$payload" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" | awk '{print $NF}')

  curl -s -X POST "$BASE/payment-providers/mock/webhook" \
    -H "Content-Type: application/json" \
    -H "X-Mock-Signature: $signature" \
    -d "$payload" | jq
}
```

Отправить webhook для первого билета:

```bash
send_payment_succeeded_webhook "$EXTERNAL_INVOICE_1_ID"
```

Отправить webhook для второго билета:

```bash
send_payment_succeeded_webhook "$EXTERNAL_INVOICE_2_ID"
```

Проверить, что билеты стали `PAID`:

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_1_ID" | jq '{id, status, combinationValues}'

curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_2_ID" | jq '{id, status, combinationValues}'
```

Ожидаемый статус обоих билетов:

```text
PAID
```

---

## 13. Закрыть продажи

Перед запуском розыгрыша тираж должен быть в статусе:

```text
SALES_CLOSED
```

Закрыть продажи:

```bash
curl -s -X POST "$BASE/draws/$DRAW_ID/close-sales" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

Проверить статус:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/draws/$DRAW_ID" | jq '{id, title, status}'
```

Ожидаемый статус:

```text
SALES_CLOSED
```

После этого в админке кнопка `Run` должна стать доступной.

---

## 14. Запустить розыгрыш и сгенерировать выигрышную комбинацию

Запуск розыгрыша:

```bash
RUN_RESPONSE=$(curl -s -X POST "$BASE/draws/$DRAW_ID/run" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "$RUN_RESPONSE" | jq
```

Backend автоматически:

```text
переводит тираж в DRAWING
генерирует выигрышную комбинацию
создаёт draw_result
проверяет оплаченные билеты
проставляет билетам WIN или LOSE
переводит тираж в COMPLETED
```

В ответе будет примерно такая структура:

```json
{
  "drawId": "...",
  "drawResultId": "...",
  "winningCombinationValues": ["7", "1"],
  "processedTickets": 2,
  "winningTickets": 1,
  "losingTickets": 1,
  "completedAt": "..."
}
```

---

## 15. Получить результат тиража

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/draws/$DRAW_ID/result" | jq
```

В ответе будет выигрышная комбинация:

```json
{
  "drawId": "...",
  "winningCombinationValues": ["7", "1"],
  "algorithmVersion": "json-schema-secure-random-v1",
  "randomProvider": "SecureRandom"
}
```

---

## 16. Проверить результат билетов

Проверить первый билет:

```bash
curl -s -X POST "$BASE/tickets/$TICKET_1_ID/check" \
  -H "Authorization: Bearer $CLIENT_TOKEN" | jq
```

Проверить второй билет:

```bash
curl -s -X POST "$BASE/tickets/$TICKET_2_ID/check" \
  -H "Authorization: Bearer $CLIENT_TOKEN" | jq
```

После запуска розыгрыша один билет должен иметь статус:

```text
WIN
```

Второй билет должен иметь статус:

```text
LOSE
```

---

## 17. Отобразить финальные статусы билетов

Через API:

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets?userId=$CLIENT_USER_ID&limit=50&offset=0" \
  | jq '.items[] | select(.drawId == env.DRAW_ID) | {
      id,
      drawId,
      status,
      combinationValues,
      matchPercent,
      prizeId,
      participatedAt,
      checkedAt
    }'
```

Ожидаемый результат:

```json
[{
  "status": "WIN",
  "combinationValues": ["7", "1"],
  "matchPercent": 100.00,
  "prizeId": "22222222-2222-2222-2222-222222222222"
},
{
  "status": "LOSE",
  "combinationValues": ["7", "2"],
  "matchPercent": 50.00,
  "prizeId": null
}]
```

Конкретно `WIN` может быть либо билет `7,1`, либо билет `7,2`, потому что выигрышная комбинация генерируется случайно. Но при этой демо-схеме один билет гарантированно будет `WIN`, а второй `LOSE`.

---

# Как пройти этот же сценарий через UI

## Администратор

Открыть:

```text
http://127.0.0.1:8090/register
```

Создать первого пользователя:

```text
email: owner@example.com
login: owner
password: admin-password
```

Войти:

```text
http://127.0.0.1:8090/login
```

Перейти в админку:

```text
http://127.0.0.1:8090/admin/draws
```

Создать тираж, указав:

```text
title: Demo basic lottery
combinationSchemaId: 11111111-1111-1111-1111-111111111111
salesStartAt: текущее время или раньше
salesEndAt: время в будущем
drawAt: время после salesEndAt
maxTickets: 100
test: false
```

После создания тираж будет в статусе `DRAFT`.

Так как в UI пока нет кнопки `Activate`, активировать его нужно через API:

```bash
curl -s -X POST "$BASE/draws/$DRAW_ID/activate" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

---

## Клиент

Открыть список тиражей:

```text
http://127.0.0.1:8090/draws
```

Выбрать тираж со статусом:

```text
ACTIVE
```

Нажать:

```text
Details → Create ticket
```

На странице аккаунта заполнить:

```text
Draw id: UUID активного тиража
Combination: 7, 1
Price: 100.00
Currency: RUB
```

После создания билета открыть его детали и нажать:

```text
Create invoice
```

Но фактическое подтверждение оплаты в текущем проекте всё равно нужно сделать через mock webhook из шага 12.

---

## Завершение розыгрыша

Когда билеты оплачены:

```bash
curl -s -X POST "$BASE/draws/$DRAW_ID/close-sales" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

После этого в UI админки кнопка `Run` станет доступной.

Можно нажать `Run` в админке или выполнить:

```bash
curl -s -X POST "$BASE/draws/$DRAW_ID/run" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

После запуска розыгрыша клиент может открыть:

```text
/account
/account/tickets/{ticketId}
```

И нажать:

```text
Check result
```

В карточке билета будет отображён статус:

```text
WIN
```

или:

```text
LOSE
```

---

# Краткая схема статусов

## Тираж

```text
DRAFT
  ↓ activate
ACTIVE
  ↓ close-sales
SALES_CLOSED
  ↓ run
DRAWING
  ↓ автоматически после run
COMPLETED
```

## Билет

```text
CREATED
  ↓ create invoice
PAYMENT_PENDING
  ↓ PAYMENT_SUCCEEDED webhook
PAID
  ↓ run draw
WIN / LOSE
```

---

# Частые ошибки

## `Draw is not available for ticket creation`

Причина: тираж не готов к продаже билетов.

Проверь:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/draws/$DRAW_ID" | jq '{status, salesStartAt, salesEndAt}'
```

Для создания билета нужно:

```text
status = ACTIVE
salesStartAt <= текущее время
salesEndAt >= текущее время
```

---

## `Draw must be in SALES_CLOSED status`

Причина: попытка нажать `Run`, когда тираж ещё `ACTIVE`.

Нужно выполнить:

```bash
curl -s -X POST "$BASE/draws/$DRAW_ID/close-sales" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

---

## `Draw cannot be run without winning rules`

Причина: для тиража не добавлены `winning_rules`.

Нужно выполнить шаг 7 и добавить приз + правило выигрыша.

---

## Билеты не стали `WIN` или `LOSE`

Скорее всего, они не были `PAID`.

Проверь:

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets?userId=$CLIENT_USER_ID&limit=50&offset=0" \
  | jq '.items[] | select(.drawId == env.DRAW_ID) | {
      id,
      status,
      combinationValues
    }'
```

В розыгрыше участвуют только билеты со статусом:

```text
PAID
```
