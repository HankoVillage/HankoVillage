#!/usr/bin/env bash
# Daily DB backup and expired refresh token cleanup.
# systemd runs it every day at 04:00 KST (deploy/systemd/hanko-backup.timer).
# Run it by hand from the project folder with:  deploy/backup.sh
#
# 1. pg_dump the whole DB into ~/backups (owner-only file)
# 2. check the file (not empty, pg_restore can read its table of contents)
# 3. upload it to the hanko-backup bucket and verify size and MD5 (the server can only create and list there)
# 4. keep the 3 newest daily backups on the server
# 5. delete refresh tokens that expired more than 30 days ago (runs even if the backup failed)
# 6. log one line per step to ~/backups/backup.log; email an alert if any step failed
# The log and the alert hold no personal data.

set -uo pipefail
umask 077

PROJECT_DIR="${PROJECT_DIR:-$HOME/hankovillage}"
BACKUP_DIR="${BACKUP_DIR:-$HOME/backups}"
LOG="$BACKUP_DIR/backup.log"
KEEP_LOCAL_COPIES=3

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
cd "$PROJECT_DIR" || { echo "$(date -u +%FT%TZ) start FAILED: no project folder" >> "$LOG"; exit 1; }

log() { echo "$(date -u +%FT%TZ) $*" >> "$LOG"; }
failed=""

# After a reboot (Persistent=true catches up a missed run) the containers may still be starting.
for _ in $(seq 1 30); do
  docker compose exec -T db pg_isready -q >/dev/null 2>&1 && break
  sleep 2
done

name="daily-$(date -u +%Y%m%dT%H%M%SZ).dump"
file="$BACKUP_DIR/$name"

# 1, 2. dump and check
if docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$file" \
   && [ -s "$file" ] \
   && docker compose exec -T db pg_restore --list < "$file" > /dev/null; then
  log "dump ok: $name $(stat -c %s "$file") bytes"
else
  log "dump FAILED"
  failed="$failed dump"
  rm -f "$file"
fi

# 3. upload (only when the dump is good)
if [ -z "$failed" ]; then
  if result=$(docker compose exec -T api python -m scripts.backup_upload "db/$name" < "$file" 2>&1); then
    log "$result"
  else
    log "upload FAILED: ${result:-no output}"
    failed="$failed upload"
  fi
fi

# 4. keep the newest daily copies on the server (manual before-*.dump files are not touched).
#    Skipped when this run failed, so local copies are not lost while uploads are broken.
if [ -z "$failed" ]; then
  ls -1t "$BACKUP_DIR"/daily-*.dump 2>/dev/null | tail -n +$((KEEP_LOCAL_COPIES + 1)) | while read -r old; do
    rm -f "$old" && log "local copy removed: $(basename "$old")"
  done
fi

# 5. expired refresh tokens: a token past expires_at is refused anyway; 30 days keeps room to investigate
if deleted=$(docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAq -v ON_ERROR_STOP=1' <<'SQL'
WITH d AS (DELETE FROM refresh_tokens WHERE expires_at < now() - interval '30 days' RETURNING 1)
SELECT count(*) FROM d;
SQL
); then
  log "token cleanup ok: ${deleted} rows"
else
  log "token cleanup FAILED"
  failed="$failed token-cleanup"
fi

# 6. alert
if [ -n "$failed" ]; then
  alert=$(docker compose exec -T api python -m scripts.backup_alert $failed 2>&1) || true
  log "${alert:-alert: no output}"
  log "done with failures:$failed"
  exit 1
fi
log "done"
