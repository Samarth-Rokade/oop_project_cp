<#
.SYNOPSIS
  Restores a backup made by backup.ps1 and checks it.

.DESCRIPTION
  By default the backup is restored into a SEPARATE database
  (farmer_marketplace_restore_test) so the live data is never touched,
  then row counts are compared with the live database.

  Use -ReplaceLive to overwrite the real database (asks for confirmation).

.EXAMPLE
  .\scripts\restore.ps1 -File backups\farmer_marketplace_20261124_221500.sql
.EXAMPLE
  .\scripts\restore.ps1 -File backups\farmer_marketplace_20261124_221500.sql -ReplaceLive
#>
param(
    [Parameter(Mandatory = $true)][string]$File,
    [switch]$ReplaceLive,
    [string]$Container = "farmer-marketplace-db",
    [string]$LiveDatabase = "farmer_marketplace",
    [string]$TestDatabase = "farmer_marketplace_restore_test"
)
$ErrorActionPreference = "Stop"

if (-not (Test-Path $File)) { throw "Backup file not found: $File" }
$target = if ($ReplaceLive) { $LiveDatabase } else { $TestDatabase }

if ($ReplaceLive) {
    $answer = Read-Host "This REPLACES all data in '$LiveDatabase'. Type the database name to continue"
    if ($answer -ne $LiveDatabase) { Write-Host "Cancelled."; exit 1 }
}

$tmp = "/tmp/restore_$(Get-Date -Format 'yyyyMMddHHmmss').sql"
docker cp $File "${Container}:$tmp" | Out-Null

Write-Host "Restoring into '$target'..."
$mysql = "MYSQL_PWD=`"`$MYSQL_ROOT_PASSWORD`" mysql -uroot"
docker exec $Container sh -c "$mysql -e 'DROP DATABASE IF EXISTS $target; CREATE DATABASE $target CHARACTER SET utf8mb4;' && $mysql $target < $tmp"
if ($LASTEXITCODE -ne 0) { throw "Restore failed." }
docker exec $Container rm -f $tmp | Out-Null

# Compare exact row counts of every table (live vs restored)
$tables = "users","farmer_profile","buyer_profile","address","category","unit","product","orders","order_item","payment","delivery","price_history","audit_log"
$selects = ($tables | ForEach-Object { "SELECT '$_' AS tbl, (SELECT COUNT(*) FROM $LiveDatabase.$_) AS live, (SELECT COUNT(*) FROM $target.$_) AS restored" }) -join " UNION ALL "
$routines = "SELECT 'routines+triggers' AS tbl, " +
            "(SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema='$LiveDatabase') + (SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema='$LiveDatabase'), " +
            "(SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema='$target') + (SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema='$target')"
Write-Host ""
# Sent on stdin: Windows PowerShell mangles quotes in long native arguments
"$selects UNION ALL $routines;" | docker exec -i $Container sh -c "$mysql -t"
Write-Host ""
if ($ReplaceLive) {
    Write-Host "Live database restored. Restart the app."
} else {
    Write-Host "Restored copy is in '$TestDatabase'. Live counts may be higher if data changed after the backup."
}
