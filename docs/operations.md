# EduPilot operations and production checklist

## PostgreSQL roles and RLS

The application runtime must connect only as the restricted `DB_USERNAME` role. Flyway must connect as the separate `DB_MIGRATION_USERNAME` role. The bootstrap `POSTGRES_ADMIN_USER` is reserved for database administration and initialization; it must never be used by the application.

On a fresh Docker volume, `docker/postgres/init/01-create-app-role.sh` creates both roles. The migration role owns objects created by Flyway. The runtime role receives DML privileges on application tables and sequence usage, but has no schema CREATE privilege and must not own tenant tables. PostgreSQL RLS is enabled and forced on tenant business tables, center memberships, user identities, center metadata, and refresh tokens.

For an existing database, Docker initialization scripts do not rerun. Schedule a maintenance window and take/verify a backup first. Connect as the database administrator and provision the migration role, transfer existing application objects from the old runtime role, and restore only the runtime privileges required by the application. Example for a dedicated EduPilot database (replace role names and use a secret manager; do not paste real passwords into shell history):

```sql
CREATE ROLE edupilot_migrator LOGIN PASSWORD 'REPLACE_WITH_A_UNIQUE_SECRET' NOSUPERUSER NOCREATEDB NOCREATEROLE BYPASSRLS;
ALTER ROLE edupilot_app NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
ALTER ROLE edupilot_migrator NOSUPERUSER NOCREATEDB NOCREATEROLE BYPASSRLS;
GRANT CONNECT ON DATABASE edupilot TO edupilot_app, edupilot_migrator;
GRANT USAGE, CREATE ON SCHEMA public TO edupilot_migrator;

-- Run only after confirming edupilot_app owns only EduPilot application objects.
REASSIGN OWNED BY edupilot_app TO edupilot_migrator;

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE CREATE ON SCHEMA public FROM edupilot_app;
GRANT USAGE ON SCHEMA public TO edupilot_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO edupilot_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO edupilot_app;
REVOKE ALL PRIVILEGES ON TABLE public.flyway_schema_history FROM edupilot_app, PUBLIC;

ALTER DEFAULT PRIVILEGES FOR ROLE edupilot_migrator IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO edupilot_app;
ALTER DEFAULT PRIVILEGES FOR ROLE edupilot_migrator IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO edupilot_app;
```

Set `DB_MIGRATION_USERNAME=edupilot_migrator` and `DB_MIGRATION_PASSWORD` in the deployment secret store before starting the new application version. The migration role intentionally has `BYPASSRLS` so trusted Flyway migrations and preflight checks can inspect all tenant rows even on `FORCE ROW LEVEL SECURITY` tables. It must be a non-superuser, its credential must be available only to deployment/migration automation, and it must never be used by the application runtime. Existing Flyway history and application objects must be owned by the migration role after the transfer. Verify that the runtime role is `NOSUPERUSER`, `NOBYPASSRLS`, owns no application tables, and has no schema CREATE privilege. Do not run the transfer blindly if the old runtime role owns unrelated objects; inspect ownership first and transfer only EduPilot objects. Before applying V20, confirm every existing center has at least one valid center membership so the migration can backfill `created_by_user_id`; V20 deliberately stops if any center has no membership owner. Reconcile orphan centers manually by assigning the verified creator/admin membership before retrying the migration.

Every tenant-scoped service transaction must call `TenantRlsContext.apply(centerId)` before querying tenant tables. Identity-owned operations must call `TenantRlsContext.applyUser(userId)` before querying or changing memberships and refresh tokens. Pre-authentication identity lookups must call `TenantRlsContext.applyUserEmail(email)` in the same transaction as the query. These values use transaction-local PostgreSQL configuration; they are intentionally not session-level settings. `center_memberships` permits the authenticated user to read only their own membership rows before center selection, while selected-center operations can access rows in that center; all membership writes require a selected-center context. `refresh_tokens` is restricted to the current user ID. `users` permits lookup by the submitted login email, self access by authenticated user ID, and tenant services access only to members of the selected center; runtime deletion is disabled by the absence of a DELETE policy. `centers` permits reads only for the selected center, the authenticated user's memberships, or the creator; inserts must identify the authenticated creator, and runtime update/delete are disabled until explicit center-management workflows exist. New tenant tables must receive an RLS policy in a Flyway migration and integration tests for SELECT, INSERT, UPDATE, and DELETE behavior. The custom `app.current_*` settings are trusted application context, not proof against arbitrary SQL execution as the runtime role: SQL injection or a compromised application process could set them directly. Keep all SQL parameterized, prohibit user-controlled native SQL, review repository queries, and treat RLS as defense in depth rather than a substitute for injection prevention.

## Fresh local setup

1. Copy `.env.example` to `.env` and replace every placeholder with a unique secret.
2. Start the infrastructure with `docker compose up -d`.
3. For an existing Postgres volume, follow the role-separation maintenance procedure above; Docker init scripts do not rerun.
4. Run the backend with both runtime and Flyway credentials, plus `REDIS_PASSWORD` and `JWT_SECRET`.
5. Verify `/actuator/health`, login rate limiting, and tenant-scoped reads/writes.

## Backup and restore

- Schedule `scripts/backup-postgres.sh` daily with a protected backup/restore database role and set `BACKUP_DIR` outside the application container.
- Store backups off-host; encrypt them at rest using the organization's KMS or an approved encryption tool. The script creates restrictive local permissions and SHA-256 checksums but does not encrypt files.
- Retain daily backups according to policy; default local retention is 14 days.
- Run `scripts/restore-postgres.sh` only against a maintenance/restore target after verifying the checksum. A restore overwrites database objects.
- Perform a restore drill at least quarterly and record RPO/RTO. A successful dump alone is not proof of recoverability.

## Load testing

`load-tests/k6-academic.js` exercises authenticated read endpoints at 10–100 virtual users. Run only against staging with a dedicated test account and synthetic data:

```bash
k6 run -e BASE_URL=https://staging.example.com -e ACCESS_TOKEN=... -e CENTER_ID=123 load-tests/k6-academic.js
```

Do not run this against production without a change window and explicit capacity limits. Tune thresholds from observed baseline, not assumptions.

## Before production

- Configure TLS termination, trusted proxy IP handling, secure cookies if browser token storage is redesigned, and strict CORS origins.
- Use separate migration and runtime DB credentials; verify the runtime role is not a table owner, superuser, or `BYPASSRLS`. Keep the migration role's `BYPASSRLS` credential restricted to deployment automation.
- Configure Redis persistence, authentication, network isolation, and monitoring.
- Add external uptime/alerting, centralized structured logs, audit retention, secret rotation, vulnerability scanning, and restore drills.
- Define data retention, privacy, incident response, and access review policies.
- Run staging migration, rollback/recovery rehearsal, security review, and load tests before opening traffic.

Billing/subscriptions are intentionally deferred and are not part of this operations scope.
