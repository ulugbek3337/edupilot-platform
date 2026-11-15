#!/usr/bin/env bash
set -Eeuo pipefail
if [[ $# -ne 1 ]]; then echo "Usage: DB_NAME=target DB_USERNAME=restore_user DB_PASSWORD=... $0 backup.dump" >&2; exit 2; fi
: "${DB_NAME:?Set target DB_NAME}"
: "${DB_USERNAME:?Set restore role}"
: "${DB_PASSWORD:?Set DB_PASSWORD}"
backup="$1"
[[ -f "$backup" ]] || { echo "Backup file not found: $backup" >&2; exit 2; }
if [[ -f "$backup.sha256" ]]; then sha256sum --check "$backup.sha256"; fi
echo "WARNING: this operation replaces objects in database '$DB_NAME'."
read -r -p "Type RESTORE to continue: " confirm
[[ "$confirm" == "RESTORE" ]] || { echo "Restore cancelled."; exit 1; }
export PGPASSWORD="$DB_PASSWORD"
pg_restore --host "${DB_HOST:-127.0.0.1}" --port "${DB_PORT:-5432}" \
  --username "$DB_USERNAME" --dbname "$DB_NAME" --clean --if-exists --no-owner --no-acl "$backup"
unset PGPASSWORD
echo "Restore completed. Verify application health and record counts before reopening traffic."
