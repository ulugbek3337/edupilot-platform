# Security review and production readiness

**Review date:** 2026-11-16  
**Scope:** Repository-level review of authentication, authorization, tenant isolation, student admission, CI, and operational scripts. This is not an external penetration test or a review of a deployed production environment.

## Controls verified in source

- JWT authentication is stateless. The authentication filter reloads user details for requests and validates account status.
- Refresh tokens are persisted, rotated, and deleted on password changes or student credential resets.
- Authentication endpoints are rate-limited through Redis. If Redis is unavailable, authentication requests fail closed with HTTP 503.
- Tenant selection from `X-Center-Id` is checked against an active center membership; the header is not trusted by itself.
- PostgreSQL row-level security is enabled and forced for tenant-owned student, teacher, course, group, lesson, attendance, grade, parent-link, notification, audit-log, and group-enrollment tables.
- Student accounts have a dedicated STUDENT membership role. Academic APIs require CENTER_ADMIN or TEACHER, parent APIs require PARENT, notifications exclude STUDENT, and audit logs require CENTER_ADMIN.
- First-login password changes are enforced in the backend filter. Temporary passwords are BCrypt-hashed and returned only from the admission/reset operation.
- Student deactivation disables the provisioned student login and revokes refresh tokens (PR #9).
- Database restore requires a matching SHA-256 checksum and an explicit operator confirmation.
- CI runs backend/frontend tests, npm audit, Docker Compose validation, shell syntax checks, and PostgreSQL backup/restore smoke tests. CodeQL and Dependabot are configured.

## Tenant isolation requirements

The production application database role must be a dedicated non-superuser role and must not have `BYPASSRLS`. Never run the application using the PostgreSQL bootstrap/superuser account. Every new tenant-owned table must receive an RLS policy in a Flyway migration and a cross-tenant integration test. Services must set tenant context transaction-locally before accessing tenant rows.

## Remaining production gates

These items cannot be completed safely from repository access alone and must be configured or verified in the target environment:

1. Terminate HTTPS with TLS 1.2+ and configure trusted reverse-proxy handling.
2. Store database, Redis, and JWT secrets in a secret manager; rotate any development or bootstrap credentials before production.
3. Verify the deployed PostgreSQL role is non-superuser and has no `BYPASSRLS`; run tenant-isolation tests using that exact role.
4. Encrypt backups, copy them off-host, enforce retention/access controls, and complete a restore drill with recorded RPO/RTO.
5. Configure centralized logs, uptime/health monitoring, alert routing, and audit-log retention.
6. Supply a staging URL and dedicated synthetic test account to run the existing k6 workload; establish capacity thresholds from measured results.
7. Decide whether email verification, MFA, and stricter account recovery are required by the launch threat model.
8. Perform an independent penetration test and privacy/data-retention review before handling real student data.

## Validation boundaries

A green CI run proves the repository's automated checks passed for the tested commit. It does not prove production availability, TLS configuration, real backup recoverability, external attack resistance, or capacity under live traffic. Do not describe the platform as production-ready until the environment-specific gates above are completed.
