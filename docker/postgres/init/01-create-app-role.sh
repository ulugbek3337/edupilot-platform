#!/bin/sh
set -eu

: "${DB_USERNAME:?DB_USERNAME must be set}"
: "${DB_PASSWORD:?DB_PASSWORD must be set}"
: "${DB_MIGRATION_USERNAME:?DB_MIGRATION_USERNAME must be set}"
: "${DB_MIGRATION_PASSWORD:?DB_MIGRATION_PASSWORD must be set}"

if [ "$DB_USERNAME" = "$DB_MIGRATION_USERNAME" ] || [ "$DB_USERNAME" = "$POSTGRES_USER" ] || [ "$DB_MIGRATION_USERNAME" = "$POSTGRES_USER" ]; then
  echo "Runtime, migration, and bootstrap PostgreSQL roles must be three distinct users." >&2
  exit 1
fi

psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=app_user="$DB_USERNAME" --set=app_password="$DB_PASSWORD" \
  --set=migration_user="$DB_MIGRATION_USERNAME" --set=migration_password="$DB_MIGRATION_PASSWORD" \
  --set=db_name="$POSTGRES_DB" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS', :'app_user', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user') \gexec
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE BYPASSRLS', :'migration_user', :'migration_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'migration_user') \gexec

ALTER ROLE :"app_user" NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
ALTER ROLE :"migration_user" NOSUPERUSER NOCREATEDB NOCREATEROLE BYPASSRLS;

GRANT CONNECT ON DATABASE :"db_name" TO :"app_user", :"migration_user";
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO :"app_user";
GRANT USAGE, CREATE ON SCHEMA public TO :"migration_user";

ALTER DEFAULT PRIVILEGES FOR ROLE :"migration_user" IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"app_user";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migration_user" IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO :"app_user";
SQL
