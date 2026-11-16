# EduPilot

EduPilot is a multi-tenant LMS platform built as a modular monolith.

## Stack
- Backend: Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, PostgreSQL, Flyway
- Frontend: React, TypeScript, Vite
- Infrastructure: Docker Compose, PostgreSQL 16, Redis 7

## Structure
- `backend/` – Spring Boot API and domain logic
- `frontend/` – React application
- `docker-compose.yml` – Local PostgreSQL and Redis
- `scripts/` – PostgreSQL backup and restore helpers
- `load-tests/` – k6 staging workload
- `docs/operations.md` – operational and production checklist

## Quick start
1. Copy `.env.example` to `.env` and replace every placeholder with unique secrets.
2. Start fresh local infrastructure: `docker compose up -d`.
3. On a fresh PostgreSQL volume, the init script creates separate runtime (`DB_USERNAME`) and migration (`DB_MIGRATION_USERNAME`) roles. The bootstrap `POSTGRES_ADMIN_USER` is not used by the application.
4. Run the backend with `DB_USERNAME`, `DB_PASSWORD`, `DB_MIGRATION_USERNAME`, `DB_MIGRATION_PASSWORD`, `REDIS_PASSWORD`, and `JWT_SECRET` configured.
5. Run the frontend: `cd frontend && npm ci && npm run dev`.

Existing PostgreSQL volumes do not rerun Docker initialization scripts. Create the restricted application role manually as described in [operations documentation](docs/operations.md); do not delete volumes to re-run initialization unless the data is disposable and backed up.

## Implemented platform foundations
- JWT authentication, refresh-token rotation, and center membership authorization
- Multi-tenant Student, Teacher, Course, Group, Lesson, Attendance, and Grade APIs
- PostgreSQL RLS for academic tenant data, center memberships, global user identities, center metadata, and user-owned refresh tokens (V6–V10, V18–V20 migrations)
- Redis-backed rate limiting for authentication endpoints
- Uzbek and Russian interface translations
- Docker Compose infrastructure with a bootstrap role, a deployment-only migration owner (`BYPASSRLS`, non-superuser), and a restricted runtime role (`NOBYPASSRLS`, no table ownership)
- Backup/restore scripts and k6 staging load test

## Deferred
Billing and subscriptions are intentionally paused and will be designed in a later phase. Do not enable payment-related flows until billing requirements and provider integration are approved.

## Operational guidance
Read [docs/operations.md](docs/operations.md) before staging or production deployment. Production readiness still requires environment-specific TLS, secrets management, backup encryption and restore drills, monitoring/alerting, privacy review, and a measured load test.
