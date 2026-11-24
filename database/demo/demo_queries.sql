-- =====================================================================
-- demo/demo_queries.sql
-- Queries for the DBMS demonstration (spec sections 12-14).
-- NOT run automatically - open in MySQL Workbench / DBeaver / VS Code
-- and run section by section as root.
-- Add the team's remaining queries (set operations, nested queries,
-- relational algebra examples...) to this file.
-- =====================================================================
USE farmer_marketplace;

-- ---------- 12.3 SELECT ----------------------------------------------
-- Active products in a category, cheapest first
SELECT product_name, price_per_unit, unit_code, farm_name
FROM active_product_view
WHERE category_name = 'Fruits'
ORDER BY price_per_unit;

-- ---------- 12.6 Aggregates / GROUP BY / HAVING ----------------------
-- Categories whose active stock value exceeds 10,000
SELECT c.category_name,
       ROUND(SUM(p.available_quantity * p.price_per_unit), 2) AS stock_value
FROM product p
JOIN category c ON c.category_id = p.category_id
WHERE p.listing_status = 'ACTIVE'
GROUP BY c.category_name
HAVING stock_value > 10000;

-- ---------- 12.7 Joins -----------------------------------------------
SELECT o.order_id, u.full_name AS buyer, p.product_name, oi.quantity, oi.line_total
FROM orders o
JOIN users u       ON u.user_id = o.buyer_id
JOIN order_item oi ON oi.order_id = o.order_id
JOIN product p     ON p.product_id = oi.product_id
ORDER BY o.order_id;

-- ---------- 12.8 Nested query ----------------------------------------
-- Products priced above the average of their own category
SELECT product_name, price_per_unit
FROM product p
WHERE price_per_unit > (SELECT AVG(price_per_unit)
                        FROM product
                        WHERE category_id = p.category_id);

-- ---------- 12.12 Routines -------------------------------------------
CALL sp_buyer_order_history(5);
CALL sp_low_stock_products(10);
SELECT order_id, total_amount, fn_order_total(order_id) AS recomputed FROM orders;

-- Price-change trigger: change a price, then look at price_history
SET @app_user_id = 2;
UPDATE product SET price_per_unit = 30.00 WHERE product_id = 1;
SELECT * FROM price_history WHERE product_id = 1 ORDER BY changed_at;

-- Stock trigger: this must FAIL with "Insufficient stock..."
-- UPDATE product SET available_quantity = -5 WHERE product_id = 1;

-- ---------- 12.10 TCL : atomic order placement -----------------------
START TRANSACTION;
INSERT INTO orders (buyer_id, delivery_address_id, order_status, total_amount)
VALUES (5, 1, 'PLACED', 0);
SET @oid = LAST_INSERT_ID();
SAVEPOINT after_header;
INSERT INTO order_item (order_id, product_id, quantity, unit_price_at_order)
VALUES (@oid, 2, 10, 32.00);
UPDATE product SET available_quantity = available_quantity - 10 WHERE product_id = 2;
-- pretend something went wrong with the item:
ROLLBACK TO SAVEPOINT after_header;
-- ...or abandon the whole order:
ROLLBACK;            -- replace with COMMIT to keep it
SELECT * FROM orders WHERE order_id = @oid;   -- empty after ROLLBACK

-- ---------- 12.9 DCL -------------------------------------------------
SHOW GRANTS FOR 'market_app'@'%';
SHOW GRANTS FOR 'market_report'@'%';
REVOKE SELECT ON farmer_marketplace.order_summary_view FROM 'market_report'@'%';
SHOW GRANTS FOR 'market_report'@'%';
GRANT  SELECT ON farmer_marketplace.order_summary_view TO   'market_report'@'%';

-- ---------- 13.2 Indexing / EXPLAIN ----------------------------------
EXPLAIN SELECT * FROM product
WHERE category_id = 2 AND listing_status = 'ACTIVE'
ORDER BY price_per_unit;

EXPLAIN SELECT * FROM product IGNORE INDEX (idx_product_category_price)
WHERE category_id = 2 AND listing_status = 'ACTIVE'
ORDER BY price_per_unit;

-- ---------- 14 Isolation level ---------------------------------------
SELECT @@transaction_isolation;   -- InnoDB default: REPEATABLE-READ
