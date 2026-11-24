-- =====================================================================
-- demo/demo_queries.sql
-- SQL demonstration for the DBMS report (spec section 12-14).
--
-- NOT run automatically. Run as root, section by section, in MySQL
-- Workbench / DBeaver / VS Code, or all at once:
--   docker exec -i farmer-marketplace-db mysql -uroot -prootpass -t farmer_marketplace < database/demo/demo_queries.sql
--
-- Everything that changes data is either inside a transaction that is
-- ROLLed BACK or works on a scratch table that is dropped again, so the
-- sample data is unchanged afterwards (except one harmless audit row in 12.10).
-- Section numbers match the requirements document.
-- =====================================================================
USE farmer_marketplace;

-- =====================================================================
-- 12.1 DDL - Data Definition Language (on a scratch table)
-- =====================================================================
CREATE TABLE demo_scratch (
    id        INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    note      VARCHAR(50)  NOT NULL DEFAULT 'n/a',
    qty       INT          NOT NULL CHECK (qty >= 0),
    code      CHAR(4)      NULL UNIQUE
) ENGINE = InnoDB;
ALTER TABLE demo_scratch ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE demo_scratch MODIFY note VARCHAR(100) NOT NULL DEFAULT 'n/a';
CREATE INDEX idx_demo_note ON demo_scratch (note);
CREATE VIEW demo_scratch_view AS SELECT id, note FROM demo_scratch WHERE qty > 0;
INSERT INTO demo_scratch (note, qty, code) VALUES ('first', 1, 'A001'), (DEFAULT, 0, NULL);
SELECT * FROM demo_scratch;
DESCRIBE demo_scratch;
TRUNCATE TABLE demo_scratch;               -- removes all rows, keeps the table
SELECT COUNT(*) AS rows_after_truncate FROM demo_scratch;
DROP VIEW demo_scratch_view;
DROP TABLE demo_scratch;

-- =====================================================================
-- 12.2 DML - INSERT / UPDATE / DELETE / SELECT (rolled back)
-- =====================================================================
START TRANSACTION;
INSERT INTO category (category_name, description) VALUES ('Flowers', 'Demo category');
UPDATE product SET available_quantity = available_quantity + 10 WHERE product_id = 9;
DELETE FROM category WHERE category_name = 'Flowers';
SELECT product_name, available_quantity FROM product WHERE product_id = 9;
ROLLBACK;

-- =====================================================================
-- 12.3 SELECT queries (the ten required ones)
-- =====================================================================
-- Q1 All active products (the view hides inactive/out-of-stock/unverified)
SELECT product_id, product_name, category_name, price_per_unit, unit_code, farm_name
FROM active_product_view ORDER BY product_name;

-- Q2 Search products by name
SELECT product_name, price_per_unit FROM active_product_view WHERE product_name LIKE '%an%';

-- Q3 Products within a price range
SELECT product_name, price_per_unit, unit_code FROM active_product_view
WHERE price_per_unit BETWEEN 30 AND 150 ORDER BY price_per_unit;

-- Q4 Products in a category
SELECT product_name, price_per_unit FROM active_product_view WHERE category_name = 'Fruits';

-- Q5 Products listed by one farmer (all statuses)
SELECT p.product_name, p.listing_status, p.available_quantity
FROM product p JOIN farmer_profile f ON f.farmer_id = p.farmer_id
WHERE f.farm_name = 'Patil Organic Farms';

-- Q6 All orders placed by a buyer
SELECT order_id, order_date, order_status, total_amount
FROM orders WHERE buyer_id = 5 ORDER BY order_date DESC;

-- Q7 Order items of one order
SELECT p.product_name, oi.quantity, p.unit_code, oi.unit_price_at_order, oi.line_total
FROM order_item oi JOIN product p ON p.product_id = oi.product_id
WHERE oi.order_id = 2;

-- Q8 Low-stock products
SELECT product_name, available_quantity, unit_code, fn_stock_level(available_quantity) AS stock_level
FROM product WHERE available_quantity < 10 AND listing_status <> 'REMOVED';

-- Q9 Farmers with their total COMPLETED (delivered) sales, including farmers with none
SELECT f.farm_name, COALESCE(SUM(oi.line_total), 0) AS completed_sales
FROM farmer_profile f
LEFT JOIN product p ON p.farmer_id = f.farmer_id
LEFT JOIN (order_item oi JOIN orders o ON o.order_id = oi.order_id AND o.order_status = 'DELIVERED')
       ON oi.product_id = p.product_id
GROUP BY f.farm_name
ORDER BY completed_sales DESC;

-- Q10 Most frequently ordered products
SELECT p.product_name, COUNT(DISTINCT oi.order_id) AS times_ordered, SUM(oi.quantity) AS total_qty
FROM order_item oi JOIN product p ON p.product_id = oi.product_id
GROUP BY p.product_id, p.product_name
ORDER BY times_ordered DESC, total_qty DESC
LIMIT 5;

-- =====================================================================
-- 12.4 Set operations
-- =====================================================================
-- UNION: everyone located in Pune district (buyers' addresses + farmers' addresses), duplicates removed
SELECT u.full_name, u.role FROM users u JOIN address a ON a.user_id = u.user_id WHERE a.district = 'Pune'
UNION
SELECT u.full_name, u.role FROM users u JOIN farmer_profile f ON f.farmer_id = u.user_id WHERE f.farm_address LIKE '%Pune%';

-- UNION ALL keeps duplicates (Priya has two Pune addresses, so she appears twice)
SELECT u.full_name FROM users u JOIN address a ON a.user_id = u.user_id WHERE a.district = 'Pune'
UNION ALL
SELECT u.full_name FROM users u JOIN farmer_profile f ON f.farmer_id = u.user_id WHERE f.farm_address LIKE '%Pune%';

-- INTERSECT: buyers who ordered Fruits AND Grains & Pulses (native since MySQL 8.0.31) ...
SELECT o.buyer_id FROM orders o JOIN order_item oi ON oi.order_id = o.order_id
  JOIN product p ON p.product_id = oi.product_id JOIN category c ON c.category_id = p.category_id
  WHERE c.category_name = 'Fruits'
INTERSECT
SELECT o.buyer_id FROM orders o JOIN order_item oi ON oi.order_id = o.order_id
  JOIN product p ON p.product_id = oi.product_id JOIN category c ON c.category_id = p.category_id
  WHERE c.category_name = 'Grains & Pulses';
-- ... and the equivalent with EXISTS (works on any MySQL version)
SELECT DISTINCT o.buyer_id FROM orders o
WHERE EXISTS (SELECT 1 FROM orders o1 JOIN order_item oi ON oi.order_id = o1.order_id
              JOIN product p ON p.product_id = oi.product_id WHERE o1.buyer_id = o.buyer_id AND p.category_id = 2)
  AND EXISTS (SELECT 1 FROM orders o2 JOIN order_item oi ON oi.order_id = o2.order_id
              JOIN product p ON p.product_id = oi.product_id WHERE o2.buyer_id = o.buyer_id AND p.category_id = 3);

-- EXCEPT (set difference): products never ordered ...
SELECT product_id FROM product
EXCEPT
SELECT product_id FROM order_item;
-- ... and the equivalent with a LEFT JOIN ... IS NULL anti-join
SELECT p.product_id FROM product p LEFT JOIN order_item oi ON oi.product_id = p.product_id
WHERE oi.order_item_id IS NULL;

-- =====================================================================
-- 12.5 String, date and numeric functions
-- =====================================================================
SELECT UPPER(product_name)                          AS upper_name,
       LOWER(unit_code)                             AS lower_unit,
       CONCAT(product_name, ' (', unit_code, ')')   AS label,
       LENGTH(product_name)                         AS name_length,
       SUBSTRING(product_name, 1, 3)                AS short_code,
       LPAD(product_id, 5, '0')                     AS padded_id
FROM product LIMIT 5;

SELECT CURRENT_DATE AS today, CURRENT_TIMESTAMP AS now_ts;

SELECT product_name, harvest_date,
       DATEDIFF(CURRENT_DATE, harvest_date)   AS days_since_harvest,
       DATE_FORMAT(harvest_date, '%d %b %Y')  AS pretty_date,
       MONTHNAME(harvest_date)                AS harvest_month,
       YEAR(harvest_date)                     AS harvest_year
FROM product WHERE harvest_date IS NOT NULL ORDER BY days_since_harvest LIMIT 5;

SELECT order_id, DATE(order_date) AS placed_on, DATEDIFF(CURRENT_DATE, order_date) AS days_ago,
       TIMESTAMPDIFF(HOUR, order_date, CURRENT_TIMESTAMP) AS hours_ago,
       DAYNAME(order_date) AS weekday
FROM orders ORDER BY order_date;

SELECT ph.product_id, ph.old_price, ph.new_price,
       ROUND((ph.new_price - ph.old_price) * 100 / ph.old_price, 2) AS pct_change,
       ABS(ph.new_price - ph.old_price)  AS abs_change,
       CEIL(ph.new_price)                AS ceil_price,
       FLOOR(ph.new_price)               AS floor_price,
       MOD(ph.product_id, 2)             AS odd_even
FROM price_history ph;

-- =====================================================================
-- 12.6 Aggregate functions, GROUP BY, HAVING
-- =====================================================================
SELECT COUNT(*) AS number_of_orders FROM orders;
SELECT SUM(total_amount) AS total_sales FROM orders WHERE order_status NOT IN ('REJECTED', 'CANCELLED', 'DELIVERY_FAILED');
SELECT ROUND(AVG(price_per_unit), 2) AS avg_price, MIN(price_per_unit) AS lowest, MAX(price_per_unit) AS highest FROM product;

-- Sales grouped by farmer
SELECT f.farm_name, COUNT(DISTINCT o.order_id) AS orders, SUM(oi.line_total) AS sales
FROM order_item oi
JOIN orders o ON o.order_id = oi.order_id
JOIN product p ON p.product_id = oi.product_id
JOIN farmer_profile f ON f.farmer_id = p.farmer_id
WHERE o.order_status NOT IN ('REJECTED', 'CANCELLED', 'DELIVERY_FAILED')
GROUP BY f.farm_name;

-- HAVING: categories with average price above 100 and at least 2 products
SELECT c.category_name, COUNT(*) AS products, ROUND(AVG(p.price_per_unit), 2) AS avg_price
FROM product p JOIN category c ON c.category_id = p.category_id
GROUP BY c.category_name
HAVING COUNT(*) >= 2 AND AVG(p.price_per_unit) > 100;

-- WITH ROLLUP: subtotals per category plus a grand total (OLAP-style, see advanced_topics.md)
SELECT COALESCE(c.category_name, '== ALL ==') AS category, SUM(oi.line_total) AS revenue
FROM order_item oi
JOIN orders o ON o.order_id = oi.order_id
JOIN product p ON p.product_id = oi.product_id
JOIN category c ON c.category_id = p.category_id
WHERE o.order_status NOT IN ('REJECTED', 'CANCELLED', 'DELIVERY_FAILED')
GROUP BY c.category_name WITH ROLLUP;

-- =====================================================================
-- 12.7 Joins
-- =====================================================================
-- Inner join: products with farmer details
SELECT p.product_name, f.farm_name, u.full_name AS farmer, u.phone
FROM product p
JOIN farmer_profile f ON f.farmer_id = p.farmer_id
JOIN users u ON u.user_id = f.farmer_id;

-- Left join: every farmer, even those without orders
SELECT f.farm_name, COUNT(DISTINCT oi.order_id) AS orders
FROM farmer_profile f
LEFT JOIN product p ON p.farmer_id = f.farmer_id
LEFT JOIN order_item oi ON oi.product_id = p.product_id
GROUP BY f.farm_name;

-- Multi-table join: order + buyer + items + product
SELECT o.order_id, u.full_name AS buyer, p.product_name, oi.quantity, oi.line_total
FROM orders o
JOIN users u ON u.user_id = o.buyer_id
JOIN order_item oi ON oi.order_id = o.order_id
JOIN product p ON p.product_id = oi.product_id
ORDER BY o.order_id;

-- Self join: competing products - same category, different farms, cheaper first
SELECT a.product_name AS product, fa.farm_name AS farm, a.price_per_unit,
       b.product_name AS competitor, fb.farm_name AS competitor_farm, b.price_per_unit AS competitor_price
FROM product a
JOIN product b ON b.category_id = a.category_id AND b.farmer_id <> a.farmer_id AND a.product_id < b.product_id
JOIN farmer_profile fa ON fa.farmer_id = a.farmer_id
JOIN farmer_profile fb ON fb.farmer_id = b.farmer_id
WHERE a.listing_status = 'ACTIVE' AND b.listing_status = 'ACTIVE'
LIMIT 8;

-- ON vs WHERE in a LEFT JOIN:
-- (a) condition in ON  -> every buyer is kept; non-delivered orders just do not match
SELECT u.full_name, COUNT(o.order_id) AS delivered_orders
FROM users u LEFT JOIN orders o ON o.buyer_id = u.user_id AND o.order_status = 'DELIVERED'
WHERE u.role = 'BUYER' GROUP BY u.full_name;
-- (b) condition in WHERE -> buyers without a delivered order are filtered out (behaves like an INNER JOIN)
SELECT u.full_name, COUNT(o.order_id) AS delivered_orders
FROM users u LEFT JOIN orders o ON o.buyer_id = u.user_id
WHERE u.role = 'BUYER' AND o.order_status = 'DELIVERED' GROUP BY u.full_name;

-- =====================================================================
-- 12.8 Nested queries
-- =====================================================================
-- Products priced above the average of their own category (correlated subquery)
SELECT p.product_name, p.price_per_unit
FROM product p
WHERE p.price_per_unit > (SELECT AVG(p2.price_per_unit) FROM product p2 WHERE p2.category_id = p.category_id);

-- Farmers whose sales exceed the average farmer's sales (derived table)
SELECT fs.farm_name, fs.total_revenue
FROM farmer_sales_summary fs
WHERE fs.total_revenue > (SELECT AVG(total_revenue) FROM farmer_sales_summary);

-- Buyers who have placed at least one order (IN and EXISTS)
SELECT full_name FROM users WHERE role = 'BUYER' AND user_id IN (SELECT buyer_id FROM orders);
SELECT u.full_name FROM users u WHERE u.role = 'BUYER' AND EXISTS (SELECT 1 FROM orders o WHERE o.buyer_id = u.user_id);

-- Products never ordered (NOT EXISTS)
SELECT p.product_name FROM product p
WHERE NOT EXISTS (SELECT 1 FROM order_item oi WHERE oi.product_id = p.product_id);

-- ALL: products more expensive than every vegetable
SELECT product_name, price_per_unit FROM product
WHERE price_per_unit > ALL (SELECT price_per_unit FROM product WHERE category_id = 1);

-- =====================================================================
-- 12.9 DCL - GRANT / REVOKE (users are created in 07_users_grants.sql)
-- =====================================================================
SHOW GRANTS FOR 'market_app'@'%';
SHOW GRANTS FOR 'market_report'@'%';
REVOKE SELECT ON farmer_marketplace.order_summary_view FROM 'market_report'@'%';
SHOW GRANTS FOR 'market_report'@'%';
GRANT SELECT ON farmer_marketplace.order_summary_view TO 'market_report'@'%';
-- A throwaway read-only user for one table
CREATE USER IF NOT EXISTS 'demo_reader'@'%' IDENTIFIED BY 'demo_reader_pass';
GRANT SELECT ON farmer_marketplace.category TO 'demo_reader'@'%';
SHOW GRANTS FOR 'demo_reader'@'%';
DROP USER 'demo_reader'@'%';

-- =====================================================================
-- 12.10 TCL - transactions, savepoints
-- =====================================================================
-- (a) An order placed by hand, then abandoned: nothing remains
START TRANSACTION;
INSERT INTO orders (buyer_id, delivery_address_id, order_status, total_amount) VALUES (5, 1, 'PLACED', 64.00);
SET @oid = LAST_INSERT_ID();
INSERT INTO order_item (order_id, product_id, quantity, unit_price_at_order) VALUES (@oid, 2, 2, 32.00);
UPDATE product SET available_quantity = available_quantity - 2 WHERE product_id = 2;
SAVEPOINT after_first_item;
INSERT INTO order_item (order_id, product_id, quantity, unit_price_at_order) VALUES (@oid, 3, 5, 22.00);
ROLLBACK TO SAVEPOINT after_first_item;    -- undo only the second item
SELECT COUNT(*) AS items_in_open_transaction FROM order_item WHERE order_id = @oid;
ROLLBACK;                                  -- undo everything
SELECT COUNT(*) AS orders_after_rollback FROM orders WHERE order_id = @oid;

-- (b) COMMIT makes a change permanent (a harmless audit row)
START TRANSACTION;
INSERT INTO audit_log (user_id, action, entity_type, details) VALUES (1, 'DEMO', 'DEMO', 'TCL demo commit');
COMMIT;

-- =====================================================================
-- 12.11 Views
-- =====================================================================
SELECT * FROM active_product_view LIMIT 5;
SELECT * FROM farmer_sales_summary;
SELECT * FROM order_summary_view;

-- =====================================================================
-- 12.12 Stored procedures, functions, triggers
-- =====================================================================
CALL sp_buyer_order_history(5);
CALL sp_low_stock_products(10);
SELECT order_id, total_amount, fn_order_total(order_id) AS recomputed,
       total_amount = fn_order_total(order_id) AS consistent
FROM orders;
SELECT fn_stock_level(0) AS a, fn_stock_level(5) AS b, fn_stock_level(50) AS c;

START TRANSACTION;
SET @app_user_id = 2;
UPDATE product SET price_per_unit = 30.00 WHERE product_id = 1;    -- trg_product_price_history fires
SELECT product_id, old_price, new_price, changed_by FROM price_history WHERE product_id = 1 ORDER BY price_history_id;
UPDATE orders SET order_status = 'CONFIRMED' WHERE order_id = 3;    -- trg_orders_status_audit fires
SELECT action, entity_id, details FROM audit_log ORDER BY log_id DESC LIMIT 1;
ROLLBACK;
-- The stock trigger rejects negative stock. Uncomment to see:
--   ERROR 1644 (45000): Insufficient stock: available quantity cannot go below zero
-- UPDATE product SET available_quantity = -5 WHERE product_id = 1;

-- =====================================================================
-- 12.13 Relational algebra -> SQL (numbering matches reports/relational_algebra.md)
-- =====================================================================
-- RA1  σ price_per_unit<50 (product)
SELECT * FROM product WHERE price_per_unit < 50;
-- RA2  π product_name, price_per_unit (σ category_id=2 (product))
SELECT DISTINCT product_name, price_per_unit FROM product WHERE category_id = 2;
-- RA3  π farm_name, product_name (farmer_profile ⋈ product)
SELECT DISTINCT f.farm_name, p.product_name FROM farmer_profile f JOIN product p ON p.farmer_id = f.farmer_id;
-- RA4  π buyer_id (σ category_id=2 (orders ⋈ order_item ⋈ product)) ∪ π buyer_id (σ category_id=3 (...))
SELECT o.buyer_id FROM orders o JOIN order_item oi ON oi.order_id = o.order_id JOIN product p ON p.product_id = oi.product_id WHERE p.category_id = 2
UNION
SELECT o.buyer_id FROM orders o JOIN order_item oi ON oi.order_id = o.order_id JOIN product p ON p.product_id = oi.product_id WHERE p.category_id = 3;
-- RA5  π product_id (product) − π product_id (order_item)
SELECT product_id FROM product EXCEPT SELECT product_id FROM order_item;
-- RA6  σ buyer.user_id = orders.buyer_id (ρ buyer (σ role='BUYER' (users)) × orders)   - Cartesian product then selection
SELECT buyer.full_name, orders.order_id FROM (SELECT * FROM users WHERE role = 'BUYER') AS buyer CROSS JOIN orders
WHERE buyer.user_id = orders.buyer_id;
-- RA7  Division: buyers who have bought from EVERY verified farm
--      π buyer_id, farmer_id (orders ⋈ order_item ⋈ product)  ÷  π farmer_id (σ verification_status='VERIFIED' (farmer_profile))
SELECT u.full_name
FROM users u
WHERE u.role = 'BUYER'
  AND NOT EXISTS (                                   -- there is no verified farm ...
      SELECT 1 FROM farmer_profile f
      WHERE f.verification_status = 'VERIFIED'
        AND NOT EXISTS (                             -- ... that this buyer has not bought from
            SELECT 1 FROM orders o
            JOIN order_item oi ON oi.order_id = o.order_id
            JOIN product p ON p.product_id = oi.product_id
            WHERE o.buyer_id = u.user_id AND p.farmer_id = f.farmer_id));

-- =====================================================================
-- 13 Index usage - EXPLAIN (see reports/query_optimization.md for timings on 100,000 rows)
-- =====================================================================
EXPLAIN SELECT product_name, price_per_unit FROM product
WHERE category_id = 2 AND listing_status = 'ACTIVE' ORDER BY price_per_unit;

EXPLAIN SELECT product_name, price_per_unit FROM product IGNORE INDEX (idx_product_category_price)
WHERE category_id = 2 AND listing_status = 'ACTIVE' ORDER BY price_per_unit;

EXPLAIN SELECT * FROM users WHERE email = 'priya@buyer.com';

-- =====================================================================
-- 14 Isolation level
-- =====================================================================
SELECT @@transaction_isolation AS isolation_level, @@autocommit AS autocommit;
