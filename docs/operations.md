# EduPilot operations and production checklist

## PostgreSQL roles and RLS

The application must connect as the restricted `DB_USERNAME` role, never as the bootstrap superuser. On a fresh Docker volume, `docker/postgres/init/01-create-app-role.sh` creates that role before Flyway runs. Flyway then creates application tables owned by the restricted role. V6 enables and forces RLS for tenant business tables.

For an existing database, create a dedicated login role through an administrator connection, grant CONNECT and USAGE/CREATE on schema public, and grant the role ownership of application objects or run Flyway as that restricted role before enabling V6. Do not use a superuser or a role with BYPASSRLS for the application.

Every tenant-scoped service transaction must call `TenantRlsContext.apply(centerId)` before querying tenant tables. This uses transaction-local PostgreSQL configuration; it is intentionally not a session-level setting. New tenant tables must receive an RLS policy in a Flyway migration and an integration test.

## Fresh local setup

1. Copy `.env.example` to `.env` and replace every placeholder with a unique secret.
2. Start the infrastructure with `docker compose up -d`.
3. On an existing Postgres volume, Docker init scripts do not rerun. Create the restricted application role manually; never delete a volume to make initialization rerun unless the data is disposable and backed up.
4. Run the backend with the same `DB_USERNAME`, `DB_PASSWORD`, `REDIS_PASSWORD`, and `JWT_SECRET` values.
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
- Use non-superuser DB credentials; verify RLS using the actual application DB role.
- Configure Redis persistence, authentication, network isolation, and monitoring.
- Add external uptime/alerting, centralized structured logs, audit retention, secret rotation, vulnerability scanning, and restore drills.
- Define data retention, privacy, incident response, and access review policies.
- Run staging migration, rollback/recovery rehearsal, security review, and load tests before opening traffic.

Billing/subscriptions are intentionally deferred and are not part of this operations scope.
