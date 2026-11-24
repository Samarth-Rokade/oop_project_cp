#!/usr/bin/env sh
# Backs up the marketplace database (macOS / Linux). See backup.ps1 for details.
#   ./scripts/backup.sh            -> backups/farmer_marketplace_YYYYMMDD_HHMMSS.sql
set -e
CONTAINER=${CONTAINER:-farmer-marketplace-db}
DATABASE=${DATABASE:-farmer_marketplace}
OUT_DIR=${1:-backups}

STAMP=$(date +%Y%m%d_%H%M%S)
mkdir -p "$OUT_DIR"
FILE="$OUT_DIR/${DATABASE}_$STAMP.sql"

echo "Backing up '$DATABASE' from container '$CONTAINER'..."
docker exec "$CONTAINER" sh -c "MYSQL_PWD=\"\$MYSQL_ROOT_PASSWORD\" mysqldump -uroot --single-transaction --routines --triggers --no-tablespaces --set-gtid-purged=OFF $DATABASE" > "$FILE"
echo "Backup written to $FILE"
