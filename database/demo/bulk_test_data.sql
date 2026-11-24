-- =====================================================================
-- demo/bulk_test_data.sql
-- Adds 100,000 synthetic products so index experiments (spec 13.2) are
-- meaningful. 12 rows are far too few: MySQL may scan them either way.
--
-- !! Run ONLY on a throwaway copy, e.g. the restore test database:
--   .\scripts\backup.ps1 ; .\scripts\restore.ps1 -File backups\<file>.sql
--   docker exec -i farmer-marketplace-db mysql -uroot -prootpass farmer_marketplace_restore_test < database/demo/bulk_test_data.sql
-- =====================================================================
SET SESSION cte_max_recursion_depth = 200000;

INSERT INTO product (farmer_id, category_id, product_name, description, unit_code,
                     available_quantity, price_per_unit, harvest_date, listing_status)
WITH RECURSIVE seq (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 100000
)
SELECT 2 + (n % 2),                                     -- farmers 2 and 3 (verified)
       1 + (n % 5),                                     -- categories 1..5
       CONCAT('Bulk product ', n),
       NULL,
       'KG',
       n % 500,                                         -- 0..499 (some out of stock)
       10 + (n % 990),                                  -- price 10..999
       DATE_SUB('2026-11-20', INTERVAL (n % 120) DAY),
       CASE WHEN n % 500 = 0 THEN 'OUT_OF_STOCK'
            WHEN n % 10 = 0 THEN 'INACTIVE'
            ELSE 'ACTIVE' END
FROM seq;

ANALYZE TABLE product;                                  -- refresh index statistics
SELECT COUNT(*) AS products FROM product;
