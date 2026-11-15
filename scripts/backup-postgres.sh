#!/usr/bin/env bash
set -Eeuo pipefail
: "${DB_NAME:?Set DB_NAME}"
: "${DB_USERNAME:?Set the restricted application or backup role}"
: "${DB_PASSWORD:?Set DB_PASSWORD}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
umask 077
mkdir -p "$BACKUP_DIR"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
file="$BACKUP_DIR/edupilot-$stamp.dump"
export PGPASSWORD="$DB_PASSWORD"
pg_dump --host "$DB_HOST" --port "$DB_PORT" --username "$DB_USERNAME" --dbname "$DB_NAME" \
  --format=custom --no-owner --no-acl --file "$file"
unset PGPASSWORD
sha256sum "$file" > "$file.sha256"
find "$BACKUP_DIR" -type f -name 'edupilot-*.dump' -mtime "+$RETENTION_DAYS" -delete
find "$BACKUP_DIR" -type f -name 'edupilot-*.dump.sha256' -mtime "+$RETENTION_DAYS" -delete
printf 'Backup created: %s\n' "$file"
