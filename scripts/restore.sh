#!/usr/bin/env sh
# Restores a backup into a SEPARATE test database and compares row counts (macOS / Linux).
#   ./scripts/restore.sh backups/farmer_marketplace_20261124_221500.sql
# To overwrite the live database instead:  TARGET=farmer_marketplace ./scripts/restore.sh <file>
set -e
FILE=$1
[ -f "$FILE" ] || { echo "Usage: $0 <backup.sql>"; exit 1; }
CONTAINER=${CONTAINER:-farmer-marketplace-db}
LIVE=farmer_marketplace
TARGET=${TARGET:-farmer_marketplace_restore_test}

if [ "$TARGET" = "$LIVE" ]; then
  printf "This REPLACES all data in '%s'. Type the database name to continue: " "$LIVE"
  read -r answer
  [ "$answer" = "$LIVE" ] || { echo "Cancelled."; exit 1; }
fi

MYSQL='MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot'
echo "Restoring into '$TARGET'..."
docker exec "$CONTAINER" sh -c "$MYSQL -e 'DROP DATABASE IF EXISTS $TARGET; CREATE DATABASE $TARGET CHARACTER SET utf8mb4;'"
docker exec -i "$CONTAINER" sh -c "$MYSQL $TARGET" < "$FILE"

SQL=""
for t in users farmer_profile buyer_profile address category unit product orders order_item payment delivery price_history audit_log; do
  [ -n "$SQL" ] && SQL="$SQL UNION ALL "
  SQL="$SQL SELECT '$t' AS tbl, (SELECT COUNT(*) FROM $LIVE.$t) AS live, (SELECT COUNT(*) FROM $TARGET.$t) AS restored"
done
docker exec "$CONTAINER" sh -c "$MYSQL -t -e \"$SQL\""
