# Lottery Frontend

Frontend lottery system is a Vue 3 application for client lottery workflows, administrative management, API documentation viewing and configurable Home page presentation.

## What The Frontend Does

The frontend provides user interfaces for the main lottery system workflows:

- Public Home page rendered from backend-managed UI template.
- Light and dark theme selection with the selected theme saved in the browser.
- User registration and password login.
- Authenticated draw list and draw details pages.
- Ticket purchase flow with combination input.
- Account page with client tickets.
- Ticket details page with invoice status, payment actions and result history.
- API documentation page based on Swagger UI, available only for system administrators.
- Forbidden page for access-denied scenarios.
- Administrative shell with role-aware navigation.

Administrative screens include:

- Dashboard entry page.
- User management.
- Role management and user role assignment.
- Permission management and role permission assignment.
- Draw management and manager assignment.
- Draw and ticket reports.
- Audit log inspection.
- Home page settings.
- UI theme management.
- UI template management.

## Architecture

The frontend is organized close to Feature-Sliced Design:

- `src/app`: application-level router and route guards.
- `src/pages`: route-level pages.
- `src/features`: feature modules with API adapters, stores and UI components.
- `src/widgets`: larger reusable page widgets, such as admin sidebar.
- `src/shared`: generated API client, common utilities and shared UI components.

Feature modules usually follow this structure:

- `api`: typed wrappers around generated OpenAPI SDK methods.
- `model`: Pinia stores and feature state.
- `ui`: reusable feature components.

The frontend does not contain backend business rules. Permissions and role checks are used for UX routing/navigation, while the backend remains the source of truth for authorization.

## Main Pages

Public and client pages:

- `/`: configurable Home page.
- `/login`: password login.
- `/register`: user registration.
- `/draws`: available draws.
- `/draws/:drawId`: draw details and result data.
- `/account`: client account and tickets.
- `/account/tickets/:ticketId`: ticket details, invoice state and result history.
- `/forbidden`: access denied page.

Admin pages:

- `/admin`
- `/admin/users`
- `/admin/roles`
- `/admin/permissions`
- `/admin/draws`
- `/admin/reports`
- `/admin/audit-logs`
- `/admin/settings`
- `/admin/ui-themes`
- `/admin/ui-templates`

API documentation:

- `/docs`: Swagger UI page for the backend OpenAPI contract. The route requires authentication and `ADMIN` role.

## Authentication And Authorization

The frontend stores the authenticated session in the auth store and attaches bearer tokens to backend requests through the shared API client configuration.

Implemented UX checks:

- Public-only redirects for login/register when already authenticated.
- Auth-required route guard.
- Admin-only route guard for `/docs`.
- Permission-based admin route access.
- Role-aware admin navigation filtering.

Security note: frontend guards improve user experience, but all real authorization decisions are enforced by the backend.

## Home Page, Themes And Templates

The Home page is configured by backend settings:

- `GET /api/v1/home-page` returns active template, default theme and available themes.
- `HomeLayoutRenderer` renders template regions:
  - header;
  - banner;
  - sidebar;
  - main;
  - footer.
- `ThemeSwitcher` allows switching between available themes.
- `theme.store.ts` saves selected theme id in `localStorage` under `lottery.theme.id`.
- Theme colors are applied through CSS variables on `document.documentElement`.

Admin users can manage:

- UI themes on `/admin/ui-themes`.
- Home page templates on `/admin/ui-templates`.
- Active Home template and default theme on `/admin/settings`.

## API Client

The frontend uses an OpenAPI-generated TypeScript client.

Generated files are located in:

```text
src/shared/api/generated
```

The source contract is:

```text
../backend/src/main/resources/openapi/openapi.yaml
```

Regenerate the client after backend API changes:

```bash
pnpm api:generate
```

Feature API modules wrap generated SDK calls so pages and stores do not depend directly on raw generated functions.

## Shared UI

Reusable UI primitives live in `src/shared/ui`, including:

- `BaseButton`
- `BaseCard`
- `BaseInput`
- `BaseSelect`
- `BaseTable`
- `BaseModal`
- `AppErrorMessage`
- `AppLoader`

Several feature components and shared components have Storybook stories for isolated review.

## Technologies

Core stack:

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Axios

API and docs:

- `@hey-api/openapi-ts`
- Swagger UI via `swagger-ui-dist`

UI development and verification:

- Storybook
- Vue TSC
- ESLint
- Prettier

Production delivery:

- Docker multi-stage frontend build.
- Nginx runtime image.
- Nginx SPA fallback.
- Nginx proxy for `/api/*` requests to backend.

Package manager:

- pnpm

## Local Development

Install dependencies:

```bash
pnpm install
```

Start Vite dev server:

```bash
pnpm dev
```

Build production bundle:

```bash
pnpm build:prod
```

Preview production build locally:

```bash
pnpm preview
```

Regenerate API client:

```bash
pnpm api:generate
```

Run Storybook:

```bash
pnpm storybook
```

Build Storybook:

```bash
pnpm build-storybook
```

## Docker

The production frontend image is built from `frontend/Dockerfile`.

The runtime container uses Nginx and `frontend/nginx/default.conf`:

- serves built static assets;
- supports SPA routing fallback to `index.html`;
- proxies API requests to backend;
- exposes a frontend health endpoint.

Run the full local stack from the repository root:

```bash
LOTTERY_HOST_HTTP_PORT=8090 docker compose --env-file .env.example up -d --build
```

Then open:

```text
http://127.0.0.1:8090
```

## Environment

Production API base URL is supplied at build time:

```text
VITE_API_BASE_URL=/api/v1
```

For Docker deployment, the default setup uses relative `/api/v1`, allowing Nginx to proxy backend requests inside the compose network.

## Quality Checks

Before completing frontend changes, run:

```bash
pnpm api:generate
pnpm build:prod
```

When UI components change, also run:

```bash
pnpm build-storybook
```

For frontend changes affecting visible screens, verify the application in a browser against the local Docker or Vite URL.
