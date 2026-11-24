<#
.SYNOPSIS
  Backs up the marketplace database (tables, data, stored routines, triggers).

.EXAMPLE
  .\scripts\backup.ps1
  -> backups\farmer_marketplace_20261124_221500.sql

.NOTES
  --single-transaction takes a consistent InnoDB snapshot without locking
  the tables, so the app can keep running during the backup.
  Database users/grants are not included; 07_users_grants.sql recreates them.
#>
param(
    [string]$OutDir = "backups",
    [string]$Container = "farmer-marketplace-db",
    [string]$Database = "farmer_marketplace"
)
$ErrorActionPreference = "Stop"

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
New-Item -ItemType Directory -Force $OutDir | Out-Null
$file = Join-Path $OutDir "${Database}_$stamp.sql"
$tmp = "/tmp/backup_$stamp.sql"

Write-Host "Backing up '$Database' from container '$Container'..."
# The dump is written inside the container and copied out with 'docker cp',
# so PowerShell never re-encodes it.
docker exec $Container sh -c "MYSQL_PWD=`"`$MYSQL_ROOT_PASSWORD`" mysqldump -uroot --single-transaction --routines --triggers --no-tablespaces --set-gtid-purged=OFF --result-file=$tmp $Database"
if ($LASTEXITCODE -ne 0) { throw "mysqldump failed. Is the container running? (docker compose up -d)" }
docker cp "${Container}:$tmp" $file | Out-Null
docker exec $Container rm -f $tmp | Out-Null

$size = [math]::Round((Get-Item $file).Length / 1KB, 1)
Write-Host "Backup written to $file ($size KB)"
