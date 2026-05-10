# Lottery System

Lottery System - это fullstack-приложение для управления онлайн-лотереей. 
Система включает клиентскую часть, административную панель, backend API, PostgreSQL и 
Docker-инфраструктуру для локального запуска.

## Что реализовано

Основные возможности системы:

- регистрация и вход пользователей;
- RBAC: роли `ADMIN`, `MANAGER`, `CLIENT` и набор permission-кодов;
- клиентские сценарии просмотра розыгрышей, покупки билетов, просмотра билетов, invoice/payment статусов и результатов;
- управление пользователями, ролями и permissions в админ-панели;
- управление розыгрышами и назначение менеджера;
- создание билетов и bulk-создание билетов через API;
- invoice/payment/refund/webhook flows через mock payment provider;
- отдельная генерация выигрышной комбинации для тиража;
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

## Структура проекта

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

## Как пользоваться системой

### Главная страница и темы

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

### Регистрация и вход

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
- `Draws` - создание и управление розыгрышами, назначение менеджера, генерация выигрышной комбинации и запуск розыгрыша;
- `Reports` - отчеты по розыгрышам и билетам;
- `Audit` - аудит действий;
- `Settings` - настройки Home page;
- `UI Themes` - управление темами;
- `UI Templates` - управление шаблонами.

### API документация

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

## Переменные окружения

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

## Текущие особенности и ограничения

- Первый admin пользователь не seed-ится миграциями: зарегистрируйте первым пользователя с логином `owner`, и backend автоматически назначит ему роль `ADMIN`.
- UI для combination schemas, prizes и winning rules еще не выделен в отдельные админ-разделы; для полного сценария розыгрыша эти данные можно добавить через SQL или API/DB tooling.
- Генерация выигрышной комбинации доступна в админке в разделе `Draws` кнопкой `Generate combination`, когда тираж находится в статусе `SALES_CLOSED`. После генерации тираж переходит в `DRAWING`, а комбинация сохраняется в `draw_results` и повторно не генерируется.
- Frontend guards не заменяют backend RBAC: все реальные проверки прав выполняет backend.
- `/docs` и `/api/v1/openapi.yaml` доступны только администратору.
- Docker Compose публикует frontend на `127.0.0.1`, что подходит для локального запуска и production-схемы с reverse proxy.

Ниже инструкция для **Сценария 1: базовая лотерея**. 
Важно: в текущей версии проекта часть действий есть в UI, 
но часть полного lifecycle пока удобнее выполнять через `curl` и SQL, 
потому что в админке нет отдельных экранов для `combination_schemas`, `prizes`, `winning_rules`, 
а также нет кнопок `Activate` / `Close sales`. Генерация выигрышной комбинации и запуск розыгрыша доступны в админке.

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
Backend уже умеет использовать эту схему при генерации выигрышной комбинации:

- `positions` описывает количество и тип позиций в комбинации;
- `allowDuplicates: false` запрещает повтор значений в одной комбинации;
- `orderSensitive: true` означает, что совпадение считается по позициям.

Для демонстрации создадим простую схему из двух чисел:

```text
первое число всегда 7
второе число 1 или 2
```

С `allowDuplicates: false` выигрышная комбинация всегда будет либо `7,1`, либо `7,2`.
Дальше в сценарии мы создадим два билета с этими комбинациями, поэтому один билет гарантированно станет `WIN`, а второй `LOSE`.

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

После этого в админке в разделе `Draws` становятся доступны действия:

```text
Generate combination
Run
```

---

## 14. Сгенерировать выигрышную комбинацию

Вариант через админку:

```text
/admin/draws → Generate combination
```

Вариант через API:

```bash
DRAW_RESULT_RESPONSE=$(curl -s -X POST "$BASE/draws/$DRAW_ID/result" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "$DRAW_RESULT_RESPONSE" | jq
```

Backend:

```text
проверяет право draw.run
проверяет, что тираж в статусе SALES_CLOSED
генерирует выигрышную комбинацию по CombinationSchema
сохраняет immutable draw_result
сохраняет algorithmVersion, randomProvider, proofHash и request/correlation ids
переводит тираж в DRAWING
```

Пример ответа:

```json
{
  "drawId": "...",
  "winningCombinationValues": ["7", "1"],
  "algorithmVersion": "json-schema-secure-random-v1",
  "randomProvider": "SecureRandom",
  "proofHash": "..."
}
```

Повторная генерация для того же тиража вернет `409`, потому что результат уже сохранен.

---

## 15. Запустить розыгрыш

Вариант через админку:

```text
/admin/draws → Run
```

Вариант через API:

```bash
RUN_RESPONSE=$(curl -s -X POST "$BASE/draws/$DRAW_ID/run" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "$RUN_RESPONSE" | jq
```

Backend автоматически:

```text
использует уже сохраненную выигрышную комбинацию
проверяет оплаченные билеты
проставляет билетам WIN или LOSE
переводит тираж в COMPLETED
```

Если вызвать `Run` сразу для тиража в статусе `SALES_CLOSED`, backend сохранит обратную совместимость:
он сам сгенерирует комбинацию, создаст `draw_result` и завершит розыгрыш одним действием.

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

## 16. Получить результат тиража

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

## 17. Проверить результат билетов

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

## 18. Отобразить финальные статусы билетов

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

После этого в UI админки станут доступны кнопки `Generate combination` и `Run`.

Рекомендуемый UI-сценарий:

```text
/admin/draws → Generate combination → Run
```

Можно также выполнить через API двумя шагами:

```bash
curl -s -X POST "$BASE/draws/$DRAW_ID/result" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

```bash
curl -s -X POST "$BASE/draws/$DRAW_ID/run" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

Если нажать `Run` сразу для `SALES_CLOSED`, backend сам сгенерирует комбинацию и завершит розыгрыш одним запросом:

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
  ↓ generate combination
DRAWING
  ↓ run
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

## `Draw must be in SALES_CLOSED or DRAWING status`

Причина: попытка нажать `Generate combination` или `Run`, когда тираж ещё `ACTIVE`.

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

## `Draw winning combination already exists`

Причина: выигрышная комбинация для тиража уже была сгенерирована.

Что делать:

```text
не нажимать Generate combination повторно
перейти к Run для завершения розыгрыша
посмотреть сохраненный результат через GET /draws/{drawId}/result
```

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
---
Ниже инструкция для **Сценария 2. Лотерея с оплатой**.

Повторяющиеся этапы из сценария 1 я не дублирую полностью, а указываю, какой шаг нужно выполнить.

# Сценарий 2. Лотерея с оплатой

Цель сценария:

```text
создание тиража
→ создание билета
→ создание invoice
→ mock-оплата
→ webhook успешной оплаты
→ привязка оплаты к билету
→ webhook неуспешной оплаты
→ завершение тиража
→ проверка результата оплаченного билета
```

---

## 0. Что должно быть подготовлено ранее (во время проверки сценаря 1)

Этот этап уже описан в **Сценарии 1**, поэтому не дублируется.

Нужно выполнить:

```text
Сценарий 1, шаги 1–7:
1. Запустить проект.
2. Создать администратора.
3. Создать клиента.
4. Подготовить combination_schema.
5. Создать тираж.
6. Активировать тираж.
7. Добавить приз и winning_rule.
```

После этих шагов должны быть переменные:

```bash
export BASE="http://127.0.0.1:8090/api/v1"
export ADMIN_TOKEN="<admin_token>"
export CLIENT_TOKEN="<client_token>"
export CLIENT_USER_ID="<client_user_id>"
export DRAW_ID="<draw_id>"
```

Тираж должен быть в статусе:

```text
ACTIVE
```

Проверить можно так:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/draws/$DRAW_ID" | jq '{id, title, status, salesStartAt, salesEndAt}'
```

---

# Часть 1. Создание билетов

Создание билета уже было описано в **Сценарии 1, шаг 9**, но для сценария оплаты создадим два билета:

```text
TICKET_SUCCESS_ID — билет с успешной оплатой
TICKET_FAILED_ID  — билет с неуспешной оплатой
```

## 1.1. Создать билет для успешной оплаты

```bash
TICKET_SUCCESS_RESPONSE=$(curl -s -X POST "$BASE/tickets" \
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

echo "$TICKET_SUCCESS_RESPONSE" | jq

export TICKET_SUCCESS_ID=$(echo "$TICKET_SUCCESS_RESPONSE" | jq -r '.id')
echo "$TICKET_SUCCESS_ID"
```

Ожидаемый статус билета:

```text
CREATED
```

---

## 1.2. Создать билет для неуспешной оплаты

```bash
TICKET_FAILED_RESPONSE=$(curl -s -X POST "$BASE/tickets" \
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

echo "$TICKET_FAILED_RESPONSE" | jq

export TICKET_FAILED_ID=$(echo "$TICKET_FAILED_RESPONSE" | jq -r '.id')
echo "$TICKET_FAILED_ID"
```

Ожидаемый статус билета:

```text
CREATED
```

---

# Часть 2. Создание invoice для оплаты

В текущем проекте оплата работает через сущности:

```text
ticket
  ↓
invoice
  ↓
payment
  ↓
mock payment webhook
```

То есть успешная оплата не отправляется напрямую в билет. 
Она приходит через webhook по `externalInvoiceId`, backend находит invoice, 
по invoice находит ticket и меняет статус билета.

---

## 2.1. Создать invoice для билета с успешной оплатой

```bash
INVOICE_SUCCESS_RESPONSE=$(curl -s -X POST "$BASE/tickets/$TICKET_SUCCESS_ID/invoice" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -d "{
    \"providerCode\": \"mock\",
    \"idempotencyKey\": \"invoice-success-$TICKET_SUCCESS_ID-$(date +%s)\"
  }")

echo "$INVOICE_SUCCESS_RESPONSE" | jq

export INVOICE_SUCCESS_ID=$(echo "$INVOICE_SUCCESS_RESPONSE" | jq -r '.id')
echo "$INVOICE_SUCCESS_ID"
```

После создания invoice билет должен перейти в статус:

```text
PAYMENT_PENDING
```

Проверить билет:

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_SUCCESS_ID" | jq '{id, status, priceAmount, priceCurrency}'
```

---

## 2.2. Создать invoice для билета с неуспешной оплатой

```bash
INVOICE_FAILED_RESPONSE=$(curl -s -X POST "$BASE/tickets/$TICKET_FAILED_ID/invoice" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -d "{
    \"providerCode\": \"mock\",
    \"idempotencyKey\": \"invoice-failed-$TICKET_FAILED_ID-$(date +%s)\"
  }")

echo "$INVOICE_FAILED_RESPONSE" | jq

export INVOICE_FAILED_ID=$(echo "$INVOICE_FAILED_RESPONSE" | jq -r '.id')
echo "$INVOICE_FAILED_ID"
```

После создания invoice второй билет тоже должен стать:

```text
PAYMENT_PENDING
```

---

# Часть 3. Дождаться mock invoice от payment provider

После создания invoice backend кладёт задачу в payment outbox. Worker должен обратиться к mock provider и заполнить:

```text
externalInvoiceId
externalPaymentId
paymentUrl
```

Подождать:

```bash
sleep 12
```

Получить invoice для успешной оплаты:

```bash
INVOICE_SUCCESS_CURRENT=$(curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_SUCCESS_ID/invoice")

echo "$INVOICE_SUCCESS_CURRENT" | jq

export EXTERNAL_INVOICE_SUCCESS_ID=$(echo "$INVOICE_SUCCESS_CURRENT" | jq -r '.externalInvoiceId')
echo "$EXTERNAL_INVOICE_SUCCESS_ID"
```

Получить invoice для неуспешной оплаты:

```bash
INVOICE_FAILED_CURRENT=$(curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_FAILED_ID/invoice")

echo "$INVOICE_FAILED_CURRENT" | jq

export EXTERNAL_INVOICE_FAILED_ID=$(echo "$INVOICE_FAILED_CURRENT" | jq -r '.externalInvoiceId')
echo "$EXTERNAL_INVOICE_FAILED_ID"
```

Ожидаемо у invoice должны появиться поля:

```text
externalInvoiceId
paymentUrl
```

Пример:

```json
{
  "status": "PENDING",
  "externalInvoiceId": "mock_inv_...",
  "paymentUrl": "https://mock-payments.local/invoices/mock_inv_..."
}
```

---

# Часть 4. Подготовить функцию для mock webhook

Mock provider требует HMAC SHA-256 подпись в заголовке:

```text
X-Mock-Signature
```

Секрет берём из `.env.example`:

```bash
export WEBHOOK_SECRET=$(grep '^LOTTERY_MOCK_PAYMENT_WEBHOOK_SECRET=' .env.example | cut -d= -f2-)
echo "$WEBHOOK_SECRET"
```

Создадим универсальную функцию для отправки webhook:

```bash
send_mock_payment_webhook() {
  local external_invoice_id="$1"
  local event_type="$2"
  local event_id="evt-${event_type}-${external_invoice_id}-$(date +%s)-$RANDOM"

  local payload="{\"eventId\":\"${event_id}\",\"eventType\":\"${event_type}\",\"externalInvoiceId\":\"${external_invoice_id}\"}"

  local signature
  signature=$(printf '%s' "$payload" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" | awk '{print $NF}')

  curl -s -X POST "$BASE/payment-providers/mock/webhook" \
    -H "Content-Type: application/json" \
    -H "X-Mock-Signature: $signature" \
    -d "$payload" | jq
}
```

Эта функция будет использоваться для событий:

```text
PAYMENT_SUCCEEDED
PAYMENT_FAILED
```

---

# Часть 5. Обработка успешной оплаты

## 5.1. Отправить webhook успешной оплаты

```bash
send_mock_payment_webhook "$EXTERNAL_INVOICE_SUCCESS_ID" "PAYMENT_SUCCEEDED"
```

Ожидаемый ответ:

```json
{
  "processed": true,
  "duplicate": false,
  "status": "PAYMENT_SUCCEEDED"
}
```

---

## 5.2. Проверить, что успешная оплата привязалась к билету

После успешного webhook backend должен обновить связанные сущности:

```text
invoice.status = PAID
payment.status = CAPTURED
ticket.status = PAID
```

Проверить билет:

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_SUCCESS_ID" | jq '{
    id,
    drawId,
    status,
    combinationValues,
    priceAmount,
    priceCurrency
  }'
```

Ожидаемый статус:

```text
PAID
```

Проверить invoice:

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_SUCCESS_ID/invoice" | jq '{
    id,
    ticketId,
    providerCode,
    status,
    externalInvoiceId,
    paymentUrl,
    paidAt
  }'
```

Ожидаемый статус invoice:

```text
PAID
```

---

# Часть 6. Обработка неуспешной оплаты

## 6.1. Отправить webhook неуспешной оплаты

```bash
send_mock_payment_webhook "$EXTERNAL_INVOICE_FAILED_ID" "PAYMENT_FAILED"
```

Ожидаемый ответ:

```json
{
  "processed": true,
  "duplicate": false,
  "status": "PAYMENT_FAILED"
}
```

---

## 6.2. Проверить статус билета после неуспешной оплаты

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_FAILED_ID" | jq '{
    id,
    drawId,
    status,
    combinationValues,
    priceAmount,
    priceCurrency
  }'
```

Ожидаемый статус билета:

```text
PAYMENT_FAILED
```

Проверить invoice:

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_FAILED_ID/invoice" | jq '{
    id,
    ticketId,
    providerCode,
    status,
    externalInvoiceId,
    paymentUrl,
    paidAt
  }'
```

Ожидаемый статус invoice:

```text
FAILED
```

---

# Часть 7. Что происходит с билетом после неуспешной оплаты

Билет со статусом:

```text
PAYMENT_FAILED
```

не участвует в розыгрыше.

В розыгрыш попадают только билеты, у которых:

```text
ticket.status = PAID
invoice.status = PAID
payment.status = CAPTURED
```

Поэтому после завершения тиража:

```text
TICKET_SUCCESS_ID участвует в розыгрыше
TICKET_FAILED_ID не участвует в розыгрыше
```

---

# Часть 8. Повторная попытка оплаты после неуспешной оплаты

Этот шаг нужен, если нужно проверить, что после `PAYMENT_FAILED` билет можно оплатить повторно.

Backend разрешает создать новый invoice для билета в статусе:

```text
CREATED
PAYMENT_FAILED
```

Создать новый invoice для ранее неуспешного билета:

```bash
INVOICE_RETRY_RESPONSE=$(curl -s -X POST "$BASE/tickets/$TICKET_FAILED_ID/invoice" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -d "{
    \"providerCode\": \"mock\",
    \"idempotencyKey\": \"invoice-retry-$TICKET_FAILED_ID-$(date +%s)\"
  }")

echo "$INVOICE_RETRY_RESPONSE" | jq

export INVOICE_RETRY_ID=$(echo "$INVOICE_RETRY_RESPONSE" | jq -r '.id')
echo "$INVOICE_RETRY_ID"
```

Подождать, пока outbox создаст mock invoice:

```bash
sleep 12
```

Получить новый invoice:

```bash
INVOICE_RETRY_CURRENT=$(curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_FAILED_ID/invoice")

echo "$INVOICE_RETRY_CURRENT" | jq

export EXTERNAL_INVOICE_RETRY_ID=$(echo "$INVOICE_RETRY_CURRENT" | jq -r '.externalInvoiceId')
echo "$EXTERNAL_INVOICE_RETRY_ID"
```

Теперь отправить успешную оплату по повторному invoice:

```bash
send_mock_payment_webhook "$EXTERNAL_INVOICE_RETRY_ID" "PAYMENT_SUCCEEDED"
```

Проверить билет:

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_FAILED_ID" | jq '{
    id,
    status,
    combinationValues
  }'
```

Ожидаемый статус:

```text
PAID
```

После этого билет тоже будет участвовать в розыгрыше.

---

# Часть 9. Закрыть продажи

Этот этап уже описан в **Сценарии 1, шаг 13**, поэтому не дублируется подробно.

Выполнить:

```bash
curl -s -X POST "$BASE/draws/$DRAW_ID/close-sales" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

Проверить:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/draws/$DRAW_ID" | jq '{id, title, status}'
```

Ожидаемый статус тиража:

```text
SALES_CLOSED
```

---

# Часть 10. Запустить розыгрыш

Этот этап уже описан в **Сценарии 1, шаг 14**, поэтому не дублируется подробно.

Выполнить:

```bash
RUN_RESPONSE=$(curl -s -X POST "$BASE/draws/$DRAW_ID/run" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "$RUN_RESPONSE" | jq
```

В ответе будет:

```json
{
  "drawId": "...",
  "drawResultId": "...",
  "winningCombinationValues": ["7", "1"],
  "processedTickets": 1,
  "winningTickets": 1,
  "losingTickets": 0,
  "completedAt": "..."
}
```

Количество `processedTickets` зависит от того, сколько билетов было успешно оплачено.

Если не делана повторная оплата для `TICKET_FAILED_ID`, то участвует только один билет:

```text
TICKET_SUCCESS_ID
```

Если сделана повторная оплата из части 8, участвуют оба билета:

```text
TICKET_SUCCESS_ID
TICKET_FAILED_ID
```

---

# Часть 11. Проверить результат оплаченного билета

Этот этап уже частично описан в **Сценарии 1, шаги 15–17**, 
но для сценария оплаты важно разделить оплаченный и неоплаченный билет.

## 11.1. Проверить результат успешно оплаченного билета

```bash
curl -s -X POST "$BASE/tickets/$TICKET_SUCCESS_ID/check" \
  -H "Authorization: Bearer $CLIENT_TOKEN" | jq
```

Ожидаемый результат:

```text
WIN
```

или:

```text
LOSE
```

Пример ответа:

```json
{
  "id": "...",
  "status": "WIN",
  "combinationValues": ["7", "1"],
  "matchPercent": 100.00,
  "prizeId": "..."
}
```

---

## 11.2. Проверить результат билета с неуспешной оплатой

Если **не делана повторная успешная оплата** из части 8, то этот билет остался:

```text
PAYMENT_FAILED
```

Попробовать проверить результат:

```bash
curl -s -X POST "$BASE/tickets/$TICKET_FAILED_ID/check" \
  -H "Authorization: Bearer $CLIENT_TOKEN" | jq
```

Ожидаемо backend не должен вернуть `WIN` или `LOSE`, потому что билет не участвовал в розыгрыше.

Вероятная ошибка:

```text
TICKET_RESULT_NOT_READY
Ticket has not been checked in draw yet
```

Это корректное поведение.

---

# Часть 12. Финальная проверка статусов билетов

Получить список билетов клиента:

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets?userId=$CLIENT_USER_ID&limit=50&offset=0" \
  | jq '.items[] | select(.drawId == env.DRAW_ID) | {
      id,
      status,
      combinationValues,
      matchPercent,
      prizeId,
      participatedAt,
      checkedAt
    }'
```

## Вариант A. Без повторной оплаты failed-билета

Ожидаемо:

```text
TICKET_SUCCESS_ID → WIN или LOSE
TICKET_FAILED_ID  → PAYMENT_FAILED
```

## Вариант B. С повторной успешной оплатой failed-билета

Ожидаемо:

```text
TICKET_SUCCESS_ID → WIN или LOSE
TICKET_FAILED_ID  → WIN или LOSE
```

---

# Краткая схема payment lifecycle

## Успешная оплата

```text
ticket.CREATED
  ↓ create invoice
ticket.PAYMENT_PENDING

invoice.CREATED
payment.INITIATED

  ↓ payment outbox worker

invoice.PENDING
payment.INITIATED
externalInvoiceId появляется

  ↓ webhook PAYMENT_SUCCEEDED

invoice.PAID
payment.CAPTURED
ticket.PAID

  ↓ run draw

ticket.WIN / ticket.LOSE
```

---

## Неуспешная оплата

```text
ticket.CREATED
  ↓ create invoice
ticket.PAYMENT_PENDING

invoice.CREATED
payment.INITIATED

  ↓ payment outbox worker

invoice.PENDING
externalInvoiceId появляется

  ↓ webhook PAYMENT_FAILED

invoice.FAILED
payment.FAILED
ticket.PAYMENT_FAILED
```

Такой билет не участвует в розыгрыше, пока по нему не будет создан новый invoice и не придёт успешный `PAYMENT_SUCCEEDED`.

---

# Важные ошибки в сценарии оплаты

## `Ticket already has an active invoice`

Причина: у билета уже есть активный invoice в статусе:

```text
CREATED
PENDING
```

Нельзя создать новый invoice, пока текущий активен.

---

## `Ticket is not available for invoice creation`

Причина: invoice можно создать только для билета в статусе:

```text
CREATED
PAYMENT_FAILED
```

Нельзя создать invoice для билета в статусе:

```text
PAID
WIN
LOSE
CANCELLED
REFUNDED
```

---

## `SIGNATURE_INVALID`

Причина: неверная HMAC-подпись mock webhook.

Проверить:

```bash
echo "$WEBHOOK_SECRET"
```

И убедиться, что подпись считается строго по тому же JSON payload, который отправляется в `curl`.

---

## `Invoice not found`

Причина: webhook отправлен с неправильным `externalInvoiceId`.

Нужно использовать именно:

```bash
echo "$EXTERNAL_INVOICE_SUCCESS_ID"
```

или:

```bash
echo "$EXTERNAL_INVOICE_FAILED_ID"
```

А не внутренний `invoice.id`.

---

## Билет не попал в розыгрыш

Проверить, что перед запуском draw билет был:

```text
ticket.status = PAID
```

Проверить:

```bash
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_SUCCESS_ID" | jq '{id, status}'
```

Для участия в розыгрыше одного статуса билета недостаточно: backend также проверяет, что связанная оплата подтверждена provider’ом:

```text
invoice.status = PAID
payment.status = CAPTURED
```

Ниже инструкция для **Сценария 3. Лотерея с историей и аналитикой**.

Повторяющиеся этапы из сценариев 1–2 я не дублирую полностью, а указываю, где они уже были описаны.

# Сценарий 3. Лотерея с историей и аналитикой

Цель сценария:

```text id="2i38h6"
→ создание тиража
→ создание билета
→ определение результата
→ просмотр истории завершённых тиражей
→ просмотр истории билетов пользователя
→ получение простого отчёта по тиражам или билетам в JSON/CSV
```

---

## 0. Предварительные условия

Этот этап уже описан в **Сценарии 1, шаги 1–7**, поэтому не дублируется.

Нужно подготовить:

```text id="8lh0c6"
1. Запущенный проект.
2. Admin-пользователь.
3. Client-пользователь.
4. Combination schema.
5. Приз.
6. Winning rule.
7. Активный тираж.
```

Также должны быть переменные:

```bash id="398vkr"
export BASE="http://127.0.0.1:8090/api/v1"
export ADMIN_TOKEN="<admin_token>"
export CLIENT_TOKEN="<client_token>"
export CLIENT_USER_ID="<client_user_id>"
export DRAW_ID="<draw_id>"
```

Проверить текущий тираж:

```bash id="uv4lu5"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/draws/$DRAW_ID" | jq '{
    id,
    title,
    status,
    salesStartAt,
    salesEndAt,
    drawAt
  }'
```

Для продолжения статус должен быть:

```text id="f9h25q"
ACTIVE
```

---

# Часть 1. Создание тиража

## Через curl

Создание тиража уже описано в **Сценарии 1, шаг 5**, поэтому здесь не дублируется полный текст.

Кратко endpoint такой:

```text id="guzbq7"
POST /api/v1/draws
```

После создания обычный тираж получает статус:

```text id="dxwb7o"
DRAFT
```

Затем его нужно активировать:

```text id="evcj5x"
POST /api/v1/draws/{drawId}/activate
```

---

## Через пользовательский интерфейс

Открыть админку:

```text id="35a8ka"
http://127.0.0.1:8090/admin/draws
```

Далее:

```text id="9dm6qi"
1. Нажать форму создания тиража.
2. Заполнить title, description, combinationSchemaId, salesStartAt, salesEndAt, drawAt, maxTickets.
3. Создать тираж.
```

Важно: через UI тираж создаётся, но в текущей версии интерфейса нет отдельной кнопки `Activate`. 
Поэтому перевод из `DRAFT` в `ACTIVE` нужно выполнить через curl:

```bash id="q3jo4e"
curl -s -X POST "$BASE/draws/$DRAW_ID/activate" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

---

# Часть 2. Создание билета

Этот этап уже описан в **Сценарии 1, шаг 9**, поэтому не дублируется.

## Через curl

```bash id="r1i5e9"
TICKET_RESPONSE=$(curl -s -X POST "$BASE/tickets" \
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

echo "$TICKET_RESPONSE" | jq

export TICKET_ID=$(echo "$TICKET_RESPONSE" | jq -r '.id')
echo "$TICKET_ID"
```

Ожидаемый статус после создания:

```text id="9x9sij"
CREATED
```

---

## Через пользовательский интерфейс

Открыть список тиражей:

```text id="7z9p0i"
http://127.0.0.1:8090/draws
```

Далее:

```text id="b85kd6"
1. Открыть активный тираж.
2. Нажать Create ticket.
3. Заполнить комбинацию.
4. Создать билет.
```

Также можно открыть аккаунт напрямую:

```text id="5nwv92"
http://127.0.0.1:8090/account
```

И создать билет в блоке:

```text id="a6ylc9"
Create ticket
```

---

# Часть 3. Оплата билета

Для участия в розыгрыше билет должен быть оплачен. 
Этот этап уже подробно описан в **Сценарии 2**, поэтому здесь только краткая схема.

Нужно выполнить:

```text id="4m5dgu"
1. Создать invoice для билета.
2. Дождаться, пока payment outbox создаст mock invoice.
3. Отправить mock webhook PAYMENT_SUCCEEDED.
4. Проверить, что билет стал PAID.
```

## Краткие команды

Создать invoice:

```bash id="hth2bj"
INVOICE_RESPONSE=$(curl -s -X POST "$BASE/tickets/$TICKET_ID/invoice" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -d "{
    \"providerCode\": \"mock\",
    \"idempotencyKey\": \"invoice-$TICKET_ID-$(date +%s)\"
  }")

echo "$INVOICE_RESPONSE" | jq
```

Подождать обработку outbox:

```bash id="sfspve"
sleep 12
```

Получить invoice:

```bash id="w6xvf0"
INVOICE_CURRENT=$(curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_ID/invoice")

echo "$INVOICE_CURRENT" | jq

export EXTERNAL_INVOICE_ID=$(echo "$INVOICE_CURRENT" | jq -r '.externalInvoiceId')
echo "$EXTERNAL_INVOICE_ID"
```

Подготовить secret:

```bash id="0zqqby"
export WEBHOOK_SECRET=$(grep '^LOTTERY_MOCK_PAYMENT_WEBHOOK_SECRET=' .env.example | cut -d= -f2-)
echo "$WEBHOOK_SECRET"
```

Отправить успешный webhook:

```bash id="t16p2v"
EVENT_ID="evt-success-$EXTERNAL_INVOICE_ID-$(date +%s)-$RANDOM"
PAYLOAD="{\"eventId\":\"$EVENT_ID\",\"eventType\":\"PAYMENT_SUCCEEDED\",\"externalInvoiceId\":\"$EXTERNAL_INVOICE_ID\"}"
SIGNATURE=$(printf '%s' "$PAYLOAD" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" | awk '{print $NF}')

curl -s -X POST "$BASE/payment-providers/mock/webhook" \
  -H "Content-Type: application/json" \
  -H "X-Mock-Signature: $SIGNATURE" \
  -d "$PAYLOAD" | jq
```

Проверить билет:

```bash id="56um18"
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_ID" | jq '{
    id,
    status,
    combinationValues,
    priceAmount,
    priceCurrency
  }'
```

Ожидаемый статус:

```text id="i2gjg2"
PAID
```

---

# Часть 4. Определение результата

Определение результата уже было описано в **Сценарии 1, шаги 13–16**, поэтому здесь кратко.

Чтобы определить результат, нужно:

```text id="u05wfq"
1. Закрыть продажи.
2. Запустить розыгрыш.
3. Получить результат тиража.
4. Проверить результат билета.
```

---

## 4.1. Закрыть продажи

```bash id="wlfxee"
curl -s -X POST "$BASE/draws/$DRAW_ID/close-sales" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

Ожидаемый статус тиража:

```text id="8or8pc"
SALES_CLOSED
```

Проверить:

```bash id="7hfgvy"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/draws/$DRAW_ID" | jq '{id, title, status}'
```

---

## 4.2. Сгенерировать выигрышную комбинацию

Через UI:

```text
/admin/draws → Generate combination
```

Через API:

```bash id="generate-result"
DRAW_RESULT_RESPONSE=$(curl -s -X POST "$BASE/draws/$DRAW_ID/result" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "$DRAW_RESULT_RESPONSE" | jq
```

После этого backend:

```text id="generate-result-steps"
1. Генерирует выигрышную комбинацию.
2. Создаёт immutable draw_result.
3. Сохраняет proofHash, randomProvider, algorithmVersion.
4. Переводит тираж в DRAWING.
```

Проверить статус тиража:

```bash id="check-drawing"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/draws/$DRAW_ID" | jq '{id, title, status}'
```

Ожидаемый статус:

```text id="drawing-status"
DRAWING
```

---

## 4.3. Запустить розыгрыш

```bash id="mxxl3j"
RUN_RESPONSE=$(curl -s -X POST "$BASE/draws/$DRAW_ID/run" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "$RUN_RESPONSE" | jq
```

После этого backend:

```text id="v3owyo"
1. Использует уже сохраненный draw_result.
2. Обрабатывает оплаченные билеты.
3. Проставляет билетам WIN или LOSE.
4. Переводит тираж в COMPLETED.
```

Проверить статус тиража:

```bash id="mi2jz5"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/draws/$DRAW_ID" | jq '{id, title, status}'
```

Ожидаемый статус:

```text id="hs3n7m"
COMPLETED
```

---

## 4.4. Получить результат тиража через curl

```bash id="pjzek4"
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/draws/$DRAW_ID/result" | jq
```

Пример ответа:

```json id="bmpo8p"
{
  "id": "7716fc8c-...",
  "drawId": "8f3be9e5-...",
  "winningCombinationValues": ["7", "1"],
  "algorithmVersion": "json-schema-secure-random-v1",
  "randomProvider": "SecureRandom",
  "proofHash": "...",
  "generatedAt": "2026-05-10T..."
}
```

---

## 4.5. Получить результат тиража через UI

Открыть страницу тиража:

```text id="22d3v7"
http://127.0.0.1:8090/draws/<DRAW_ID>
```

На странице будет блок:

```text id="0rnsct"
Draw result
```

Если розыгрыш завершён, там отображаются:

```text id="t20qrz"
Winning combination
Generated at
Algorithm
Random provider
Proof hash
Request ID
```

Если розыгрыш ещё не завершён, будет сообщение:

```text id="k0jzy0"
Result is not published yet.
```

---

## 4.6. Проверить результат билета через curl

```bash id="zxd4p9"
curl -s -X POST "$BASE/tickets/$TICKET_ID/check" \
  -H "Authorization: Bearer $CLIENT_TOKEN" | jq
```

Ожидаемый статус билета:

```text id="i57ufk"
WIN
```

или:

```text id="4i3awi"
LOSE
```

Проверить билет после определения результата:

```bash id="d72mhz"
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_ID" | jq '{
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

---

## 4.7. Проверить результат билета через UI

Открыть страницу билета:

```text id="ak6b8j"
http://127.0.0.1:8090/account/tickets/<TICKET_ID>
```

Нажать:

```text id="7ypv77"
Check result
```

В карточке билета будут отображаться:

```text id="ueao7u"
Status
Draw
Combination
Price
Created
Participated
Checked
Match percent
Prize
```

После завершения розыгрыша статус должен стать:

```text id="5ejslz"
WIN
```

или:

```text id="66bptf"
LOSE
```

---

# Часть 5. История завершённых тиражей

В текущем проекте есть два способа посмотреть завершённые тиражи:

```text id="dxat78"
1. Публичный список тиражей /draws — показывает список тиражей.
2. Админский отчёт /admin/reports — позволяет фильтровать тиражи по статусу COMPLETED.
```

---

## 5.1. История завершённых тиражей через UI

### Вариант 1. Через страницу тиражей

Открыть:

```text id="w5lsrb"
http://127.0.0.1:8090/draws
```

В списке найти тиражи со статусом:

```text id="owd1jx"
COMPLETED
```

Открыть конкретный завершённый тираж:

```text id="tmlmx1"
http://127.0.0.1:8090/draws/<DRAW_ID>
```

На странице завершённого тиража будет виден результат в блоке:

```text id="qw8k6p"
Draw result
```

---

### Вариант 2. Через админский отчёт

Открыть:

```text id="sp29g9"
http://127.0.0.1:8090/admin/reports
```

В блоке:

```text id="p63z3j"
Draw report filters
```

выставить:

```text id="ntry8p"
Status: Completed
Page size: 50
```

Нажать:

```text id="j18etk"
Apply
```

В таблице `Draw report` будут отображены завершённые тиражи.

Важно: для доступа нужен admin-пользователь или право:

```text id="k29a92"
report.draw.export
```

---

## 5.2. История завершённых тиражей через curl

### Получить все тиражи и отфильтровать COMPLETED локально

```bash id="187h71"
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/draws?limit=100&offset=0" \
  | jq '.items[] | select(.status == "COMPLETED") | {
      id,
      title,
      status,
      salesStartAt,
      salesEndAt,
      drawAt,
      createdAt
    }'
```

---

### Получить завершённые тиражи через reports API

```bash id="zdq98c"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/draws?status=COMPLETED&limit=100&offset=0" | jq
```

Ответ будет в формате:

```json id="gi2abj"
{
  "items": [
    {
      "id": "...",
      "title": "Demo basic lottery",
      "status": "COMPLETED",
      "salesStartAt": "...",
      "salesEndAt": "...",
      "drawAt": "...",
      "createdAt": "..."
    }
  ],
  "total": 1,
  "limit": 100,
  "offset": 0,
  "hasMore": false
}
```

---

### Получить результат конкретного завершённого тиража

```bash id="1un9zh"
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/draws/$DRAW_ID/result" | jq
```

---

# Часть 6. История билетов пользователя

История билетов доступна клиенту на странице аккаунта и через API.

---

## 6.1. История билетов пользователя через UI

Открыть:

```text id="8ccwf0"
http://127.0.0.1:8090/account
```

На странице есть несколько блоков:

```text id="pzqjki"
Account
Create ticket
Ticket list
Result history
```

В `Ticket list` отображаются билеты пользователя.

В `Result history` отображаются билеты, у которых уже есть результат:

```text id="9m4t6e"
WIN
LOSE
CHECKED
PARTICIPATED
NOT_PARTICIPATED
```

Для конкретного билета можно открыть:

```text id="bdrqd6"
http://127.0.0.1:8090/account/tickets/<TICKET_ID>
```

Там отображаются:

```text id="x0t4rl"
статус билета
комбинация
цена
дата создания
дата участия
дата проверки
процент совпадения
приз
```

---

## 6.2. История билетов пользователя через curl

Получить все билеты пользователя:

```bash id="dykvyd"
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets?userId=$CLIENT_USER_ID&limit=100&offset=0" | jq
```

Вывести в компактном виде:

```bash id="ji4yfk"
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets?userId=$CLIENT_USER_ID&limit=100&offset=0" \
  | jq '.items[] | {
      id,
      drawId,
      status,
      combinationValues,
      priceAmount,
      priceCurrency,
      matchPercent,
      prizeId,
      participatedAt,
      checkedAt,
      createdAt
    }'
```

---

## 6.3. Посмотреть только билеты с результатом WIN или LOSE

```bash id="gw8mlr"
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets?userId=$CLIENT_USER_ID&limit=100&offset=0" \
  | jq '.items[] | select(.status == "WIN" or .status == "LOSE") | {
      id,
      drawId,
      status,
      combinationValues,
      matchPercent,
      prizeId,
      checkedAt
    }'
```

---

## 6.4. Посмотреть конкретный билет

```bash id="n6wvlv"
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_ID" | jq
```

---

# Часть 7. Простой отчёт по тиражам в JSON

Отчёты доступны через:

```text id="fbpsd1"
GET /api/v1/reports/draws
GET /api/v1/reports/tickets
GET /api/v1/reports/draws/export
GET /api/v1/reports/tickets/export
```

Для отчётов нужен admin-пользователь или соответствующие права:

```text id="jv7r6c"
report.draw.export
report.ticket.export
```

---

## 7.1. JSON-отчёт по завершённым тиражам через UI

Открыть:

```text id="peftc6"
http://127.0.0.1:8090/admin/reports
```

В блоке `Draw report filters` указать:

```text id="supv1k"
Status: Completed
Page size: 50
```

Нажать:

```text id="g7jv6s"
JSON
```

Браузер скачает JSON-отчёт.

---

## 7.2. JSON-отчёт по завершённым тиражам через curl

```bash id="5hs2z1"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/draws?status=COMPLETED&limit=100&offset=0" \
  | jq
```

Сохранить в файл:

```bash id="k0p69m"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/draws?status=COMPLETED&limit=100&offset=0" \
  | jq > draw-report-completed.json
```

---

## 7.3. JSON export endpoint

```bash id="c1or7p"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/draws/export?format=json&status=COMPLETED&limit=100&offset=0" \
  | jq > draw-report-completed-export.json
```

---

# Часть 8. Простой отчёт по тиражам в CSV

## Через UI

Открыть:

```text id="zfxr18"
http://127.0.0.1:8090/admin/reports
```

В блоке `Draw report filters` указать:

```text id="xqaemg"
Status: Completed
Page size: 50
```

Нажать:

```text id="xsr96p"
CSV
```

Браузер скачает файл:

```text id="wumvlb"
draw-report.csv
```

---

## Через curl

```bash id="cd0nyk"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/draws/export?format=csv&status=COMPLETED&limit=100&offset=0" \
  -o draw-report-completed.csv
```

Проверить файл:

```bash id="io8hip"
cat draw-report-completed.csv
```

CSV содержит поля:

```text id="nrqs03"
id,title,status,managerId,combinationSchemaId,salesStartAt,salesEndAt,drawAt,test,createdAt,version
```

---

# Часть 9. Простой отчёт по билетам в JSON

## 9.1. JSON-отчёт по билетам конкретного пользователя через UI

Открыть:

```text id="xmm0u9"
http://127.0.0.1:8090/admin/reports
```

В блоке `Ticket report filters` указать:

```text id="j1ytgj"
User ID: <CLIENT_USER_ID>
Status: Any status или Win/Lose
Page size: 50
```

Нажать:

```text id="pcgj80"
Apply
```

Для скачивания нажать:

```text id="5i64tc"
JSON
```

---

## 9.2. JSON-отчёт по билетам конкретного пользователя через curl

```bash id="q61xwx"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/tickets?userId=$CLIENT_USER_ID&limit=100&offset=0" \
  | jq
```

Сохранить:

```bash id="i3mi9h"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/tickets?userId=$CLIENT_USER_ID&limit=100&offset=0" \
  | jq > ticket-report-user.json
```

---

## 9.3. JSON-отчёт только по выигравшим билетам

```bash id="rxqn52"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/tickets?userId=$CLIENT_USER_ID&status=WIN&limit=100&offset=0" \
  | jq > ticket-report-user-win.json
```

---

## 9.4. JSON-отчёт только по проигравшим билетам

```bash id="wwwhh6"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/tickets?userId=$CLIENT_USER_ID&status=LOSE&limit=100&offset=0" \
  | jq > ticket-report-user-lose.json
```

---

# Часть 10. Простой отчёт по билетам в CSV

## Через UI

Открыть:

```text id="0ecqfc"
http://127.0.0.1:8090/admin/reports
```

В блоке `Ticket report filters` указать:

```text id="i67xyk"
User ID: <CLIENT_USER_ID>
Status: Any status / Win / Lose / Paid
Page size: 50
```

Нажать:

```text id="0hk5xt"
CSV
```

Браузер скачает файл:

```text id="c1ciek"
ticket-report.csv
```

---

## Через curl

CSV-отчёт по всем билетам пользователя:

```bash id="ldvjg2"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/tickets/export?format=csv&userId=$CLIENT_USER_ID&limit=100&offset=0" \
  -o ticket-report-user.csv
```

CSV-отчёт только по билетам конкретного тиража:

```bash id="l2godo"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/tickets/export?format=csv&drawId=$DRAW_ID&limit=100&offset=0" \
  -o ticket-report-draw.csv
```

CSV-отчёт только по выигравшим билетам:

```bash id="9gks03"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/tickets/export?format=csv&status=WIN&limit=100&offset=0" \
  -o ticket-report-win.csv
```

Проверить файл:

```bash id="iuzmc4"
cat ticket-report-user.csv
```

CSV содержит поля:

```text id="ock9xy"
id,userId,drawId,status,priceAmount,priceCurrency,test,createdAt,version
```

---

# Часть 11. Получить аналитику через jq без отдельного backend-отчёта

В проекте уже есть отчёты списком, но нет отдельного аналитического endpoint’а вида `/analytics/summary`. 
Поэтому простой аналитический результат можно получить через `jq` поверх JSON-отчёта.

---

## 11.1. Количество билетов по статусам

```bash id="o03j3z"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/tickets?drawId=$DRAW_ID&limit=250&offset=0" \
  | jq '
      .items
      | group_by(.status)
      | map({
          status: .[0].status,
          count: length
        })
    '
```

Пример результата:

```json id="u4rcoo"
[
  {
    "status": "WIN",
    "count": 1
  },
  {
    "status": "LOSE",
    "count": 3
  }
]
```

---

## 11.2. Количество WIN / LOSE по конкретному тиражу

```bash id="3zqiba"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/tickets?drawId=$DRAW_ID&limit=250&offset=0" \
  | jq '
      {
        drawId: env.DRAW_ID,
        totalTickets: (.items | length),
        winTickets: (.items | map(select(.status == "WIN")) | length),
        loseTickets: (.items | map(select(.status == "LOSE")) | length),
        paidTickets: (.items | map(select(.status == "PAID")) | length)
      }
    '
```

---

## 11.3. Простая аналитика по завершённым тиражам

```bash id="76u2rg"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/draws?status=COMPLETED&limit=250&offset=0" \
  | jq '
      {
        completedDraws: (.items | length),
        draws: [.items[] | {
          id,
          title,
          status,
          salesEndAt,
          drawAt,
          createdAt
        }]
      }
    '
```

---

# Часть 12. Полная проверка сценария одним набором команд

Этот блок можно использовать после того, как тираж создан, билет создан, билет оплачен и розыгрыш запущен.

```bash id="6j7n6c"
echo "1. Draw status"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/draws/$DRAW_ID" | jq '{id, title, status}'

echo "2. Draw result"
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/draws/$DRAW_ID/result" | jq

echo "3. Ticket result"
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets/$TICKET_ID" | jq '{
    id,
    drawId,
    status,
    combinationValues,
    matchPercent,
    prizeId,
    participatedAt,
    checkedAt
  }'

echo "4. Completed draws history"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/draws?status=COMPLETED&limit=100&offset=0" \
  | jq '.items[] | {id, title, status, drawAt, createdAt}'

echo "5. User ticket history"
curl -s -H "Authorization: Bearer $CLIENT_TOKEN" \
  "$BASE/tickets?userId=$CLIENT_USER_ID&limit=100&offset=0" \
  | jq '.items[] | {
      id,
      drawId,
      status,
      combinationValues,
      matchPercent,
      prizeId,
      checkedAt
    }'

echo "6. Ticket analytics by status"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/reports/tickets?drawId=$DRAW_ID&limit=250&offset=0" \
  | jq '.items | group_by(.status) | map({status: .[0].status, count: length})'
```

---

# Краткая схема сценария 3

```text id="n78wp8"
1. Создать тираж
   Уже описано в сценарии 1.

2. Активировать тираж
   Уже описано в сценарии 1.

3. Создать билет
   Уже описано в сценарии 1.

4. Оплатить билет
   Уже описано в сценарии 2.

5. Закрыть продажи
   Уже описано в сценарии 1.

6. Запустить розыгрыш
   Уже описано в сценарии 1.

7. Проверить результат билета
   Через curl: POST /tickets/{ticketId}/check
   Через UI: /account/tickets/{ticketId} → Check result

8. Посмотреть историю завершённых тиражей
   Через UI: /draws или /admin/reports
   Через curl: GET /reports/draws?status=COMPLETED

9. Посмотреть историю билетов пользователя
   Через UI: /account
   Через curl: GET /tickets?userId={userId}

10. Получить отчёт
   JSON: /reports/draws или /reports/tickets
   CSV: /reports/draws/export?format=csv или /reports/tickets/export?format=csv
```

---

# Важные замечания

1. **История завершённых тиражей** в пользовательском интерфейсе отображается через общий список `/draws`, 
а более точная фильтрация по `COMPLETED` есть в админском разделе `/admin/reports`.

2. **История билетов пользователя** доступна клиенту через `/account`.

3. **Отчёты JSON/CSV** доступны через админский раздел и требуют прав `report.draw.export` или `report.ticket.export`.

4. В `draw report` параметр `userId` фактически используется как фильтр по менеджеру тиража, то есть по `managerId`.

5. В `reports` фильтры `dateFrom` и `dateTo` работают по дате создания записи, 
то есть по `createdAt`, а не по дате завершения тиража.
