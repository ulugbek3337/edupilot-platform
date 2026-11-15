# EduPilot

EduPilot is a multi-tenant LMS SaaS platform built as a modular monolith. The project is structured for deployment, with JWT security and extension points for future modules.

## Stack

- Backend: Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA, PostgreSQL, Flyway
- Frontend: React, TypeScript, Vite
- Infra: Docker Compose, PostgreSQL, Redis, Nginx

## Structure

- `backend/` – Spring Boot API and domain logic
- `frontend/` – Vite React app
- `docker-compose.yml` – Local infrastructure for PostgreSQL and Redis

## Quick start

1. Configure the required environment variables before starting the backend:
   - `DB_PASSWORD` – PostgreSQL password
   - `JWT_SECRET` – Base64-encoded secret with at least 32 bytes for HS256
   - `APP_CORS_ALLOWED_ORIGINS` – Comma-separated exact frontend origins; defaults to `http://localhost:5173` for local development
2. Start infrastructure:
   ```bash
   docker compose up -d
   ```
3. Run backend:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
4. Run frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

For production, set `APP_CORS_ALLOWED_ORIGINS` to the exact HTTPS origin(s) serving the frontend. Do not use `*` when credentials are enabled. Keep secrets outside source control.

## Core requirements implemented

- Multi-tenant center scoping with `CurrentCenterContext`
- JWT auth and refresh token posture
- Center membership and authorization checks
- Role-based access control with center-aware permissions
- Domain modules for centers, users, memberships, and auth
- Flyway migration strategy and PostgreSQL configuration
- Uzbek and Russian UI translations with persisted user language preference
- Localized API validation and application errors using `Accept-Language`
- Structured content translations keyed by entity, field, and language

## Languages and translated content

The interface supports Uzbek (`uz`, locale `uz-UZ`) and Russian (`ru`, locale `ru-RU`). Visitors' selection is stored in browser local storage. Once a user session is established, `applyAuthenticatedLanguage` applies the preference returned by the API; authenticated language changes are saved through `PATCH /api/v1/auth/me/language`. The account preference is independent of the selected center.

Backend messages are in `backend/src/main/resources/i18n/messages_uz.properties` and `messages_ru.properties`. Frontend strings are split by language and feature namespace under `frontend/src/i18n/locales/`. Translated educational fields belong in `content_translations`, which stores one translated field per owner, field, and language rather than cloning business records. The owning module should validate the owner reference when it writes or reads a row.

## Notes

This starter is organized so the project can expand into courses, attendance, grades, payments, and notifications.
