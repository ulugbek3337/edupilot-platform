# EduPilot operations and production checklist

## PostgreSQL roles and RLS

The application runtime must connect only as the restricted `DB_USERNAME` role. Flyway must connect as the separate `DB_MIGRATION_USERNAME` role. The bootstrap `POSTGRES_ADMIN_USER` is reserved for database administration and initialization; it must never be used by the application.

On a fresh Docker volume, `docker/postgres/init/01-create-app-role.sh` creates both roles. The migration role owns objects created by Flyway. The runtime role receives DML privileges on application tables and sequence usage, but has no schema CREATE privilege and must not own tenant tables. PostgreSQL RLS is enabled and forced on tenant business tables.

For an existing database, Docker initialization scripts do not rerun. Schedule a maintenance window and take/verify a backup first. Connect as the database administrator and provision the migration role, transfer existing application objects from the old runtime role, and restore only the runtime privileges required by the application. Example for a dedicated EduPilot database (replace role names and use a secret manager; do not paste real passwords into shell history):

```sql
CREATE ROLE edupilot_migrator LOGIN PASSWORD 'REPLACE_WITH_A_UNIQUE_SECRET';
GRANT CONNECT ON DATABASE edupilot TO edupilot_migrator;
GRANT USAGE, CREATE ON SCHEMA public TO edupilot_migrator;

-- Run only after confirming edupilot_app owns only EduPilot application objects.
REASSIGN OWNED BY edupilot_app TO edupilot_migrator;

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE CREATE ON SCHEMA public FROM edupilot_app;
GRANT USAGE ON SCHEMA public TO edupilot_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO edupilot_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO edupilot_app;

ALTER DEFAULT PRIVILEGES FOR ROLE edupilot_migrator IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO edupilot_app;
ALTER DEFAULT PRIVILEGES FOR ROLE edupilot_migrator IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO edupilot_app;
```

Set `DB_MIGRATION_USERNAME=edupilot_migrator` and `DB_MIGRATION_PASSWORD` in the deployment secret store before starting the new application version. Existing Flyway history and application objects must be owned by the migration role after the transfer. Verify that the runtime role is not a superuser, has no `BYPASSRLS`, owns no tenant tables, and has no schema CREATE privilege. Do not run the transfer blindly if the old runtime role owns unrelated objects; inspect ownership first and transfer only EduPilot objects.

Every tenant-scoped service transaction must call `TenantRlsContext.apply(centerId)` before querying tenant tables. This uses transaction-local PostgreSQL configuration; it is intentionally not a session-level setting. New tenant tables must receive an RLS policy in a Flyway migration and integration tests for SELECT, INSERT, UPDATE, and DELETE behavior.

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
- Use separate migration and runtime DB credentials; verify the runtime role is not a table owner, superuser, or `BYPASSRLS`.
- Configure Redis persistence, authentication, network isolation, and monitoring.
- Add external uptime/alerting, centralized structured logs, audit retention, secret rotation, vulnerability scanning, and restore drills.
- Define data retention, privacy, incident response, and access review policies.
- Run staging migration, rollback/recovery rehearsal, security review, and load tests before opening traffic.

Billing/subscriptions are intentionally deferred and are not part of this operations scope.
