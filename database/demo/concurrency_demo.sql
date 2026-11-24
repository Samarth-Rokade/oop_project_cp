-- =====================================================================
-- demo/concurrency_demo.sql
-- Two buyers try to buy the last units of Turmeric (8 KG in stock) at the
-- same time. Run it by hand in TWO terminals to see the lock (spec 14.3-14.4).
--
-- Open two MySQL shells:
--   docker exec -it farmer-marketplace-db mysql -uroot -prootpass farmer_marketplace
--
-- Run the steps in the order of the numbers. Everything is rolled back at
-- the end, so the sample data is unchanged.
-- =====================================================================

-- ---------------------------------------------------------------------
-- (1) SESSION A - Priya wants 6 KG
-- ---------------------------------------------------------------------
START TRANSACTION;
SELECT available_quantity FROM product WHERE product_id = 9 FOR UPDATE;   -- 8.00, row is now locked by A

-- ---------------------------------------------------------------------
-- (2) SESSION B - FreshMart wants 5 KG
-- ---------------------------------------------------------------------
START TRANSACTION;
SELECT available_quantity FROM product WHERE product_id = 9 FOR UPDATE;
-- ... B WAITS here (no output) because A holds the exclusive lock.

-- ---------------------------------------------------------------------
-- (3) SESSION A - 6 <= 8, so A reduces the stock and commits
-- ---------------------------------------------------------------------
UPDATE product SET available_quantity = available_quantity - 6 WHERE product_id = 9;
COMMIT;                       -- releases the lock

-- ---------------------------------------------------------------------
-- (4) SESSION B - wakes up and reads 2.00 (the committed value), not 8.00
-- ---------------------------------------------------------------------
-- 5 > 2, so the application rejects the order (InsufficientStockException)
ROLLBACK;

-- ---------------------------------------------------------------------
-- (5) SESSION A - put the stock back so the demo can be repeated
-- ---------------------------------------------------------------------
UPDATE product SET available_quantity = 8.00 WHERE product_id = 9;

-- ---------------------------------------------------------------------
-- What goes wrong WITHOUT the lock (lost update):
--   A: SELECT available_quantity ... (8)        B: SELECT available_quantity ... (8)
--   A: 6 <= 8 -> UPDATE ... SET qty = 8 - 6     B: 5 <= 8 -> UPDATE ... SET qty = 8 - 5
--   Both orders are accepted: 11 KG sold from 8 KG of stock.
-- The CHECK (available_quantity >= 0) would stop "qty - 6 - 5" only if both
-- updates used available_quantity - n; with values computed in Java it would not.
-- That is why OrderService locks the rows with SELECT ... FOR UPDATE.
--
-- Optional: watch the lock while B is waiting (in a third session):
--   SELECT * FROM performance_schema.data_lock_waits\G
-- ---------------------------------------------------------------------
