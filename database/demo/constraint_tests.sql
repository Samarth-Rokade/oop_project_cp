-- =====================================================================
-- demo/constraint_tests.sql
-- Tests the database constraints DIRECTLY, without the Java app (spec 18).
-- Every statement below is EXPECTED TO FAIL; the expected error is in the
-- comment above it. Run with --force so the client continues after errors:
--
--   docker exec -i farmer-marketplace-db mysql -uroot -prootpass --force farmer_marketplace < database/demo/constraint_tests.sql
--
-- Nothing is changed: each failing statement is rejected by MySQL.
-- =====================================================================
USE farmer_marketplace;

-- C01 duplicate email                     -> ERROR 1062 Duplicate entry ... for key 'users.uq_users_email'
INSERT INTO users (full_name, email, password_hash, role) VALUES ('Copy', 'priya@buyer.com', REPEAT('x', 60), 'BUYER');

-- C02 unknown role                        -> ERROR 3819 Check constraint 'chk_users_role' is violated
INSERT INTO users (full_name, email, password_hash, role) VALUES ('Bad', 'bad@x.com', REPEAT('x', 60), 'GUEST');

-- C03 invalid email format                -> ERROR 3819 Check constraint 'chk_users_email' is violated
INSERT INTO users (full_name, email, password_hash, role) VALUES ('Bad', 'not-an-email', REPEAT('x', 60), 'BUYER');

-- C04 negative stock (trigger runs first) -> ERROR 1644 Available quantity cannot be negative
INSERT INTO product (farmer_id, category_id, product_name, unit_code, available_quantity, price_per_unit)
VALUES (2, 1, 'Bad stock', 'KG', -1, 10);

-- C05 zero price                          -> ERROR 3819 Check constraint 'chk_product_price' is violated
INSERT INTO product (farmer_id, category_id, product_name, unit_code, available_quantity, price_per_unit)
VALUES (2, 1, 'Free tomato', 'KG', 10, 0);

-- C06 product of a non-existent farmer    -> ERROR 1452 Cannot add or update a child row (fk_product_farmer)
INSERT INTO product (farmer_id, category_id, product_name, unit_code, available_quantity, price_per_unit)
VALUES (999, 1, 'Orphan', 'KG', 10, 10);

-- C07 a BUYER used as farmer              -> ERROR 1452 (user 5 has no farmer_profile row)
INSERT INTO product (farmer_id, category_id, product_name, unit_code, available_quantity, price_per_unit)
VALUES (5, 1, 'Buyer product', 'KG', 10, 10);

-- C08 unknown unit                        -> ERROR 1452 (fk_product_unit)
INSERT INTO product (farmer_id, category_id, product_name, unit_code, available_quantity, price_per_unit)
VALUES (2, 1, 'Odd unit', 'BUCKET', 10, 10);

-- C09 invalid order status                -> ERROR 3819 Check constraint 'chk_orders_status' is violated
UPDATE orders SET order_status = 'LOST' WHERE order_id = 1;

-- C10 order item with zero quantity       -> ERROR 3819 Check constraint 'chk_item_qty' is violated
INSERT INTO order_item (order_id, product_id, quantity, unit_price_at_order) VALUES (3, 1, 0, 28);

-- C11 same product twice in one order     -> ERROR 1062 Duplicate entry '1-1' for key 'order_item.uq_order_item_product'
INSERT INTO order_item (order_id, product_id, quantity, unit_price_at_order) VALUES (1, 1, 1, 28);

-- C12 setting line_total by hand          -> ERROR 3105 The value specified for generated column 'line_total' ... is not allowed
INSERT INTO order_item (order_id, product_id, quantity, unit_price_at_order, line_total) VALUES (3, 2, 1, 32, 999);

-- C13 delete a product that was ordered   -> ERROR 1451 Cannot delete or update a parent row (fk_item_product)
DELETE FROM product WHERE product_id = 1;

-- C14 delete a category in use            -> ERROR 1451 (fk_product_category)
DELETE FROM category WHERE category_id = 1;

-- C15 invalid PIN code                    -> ERROR 3819 Check constraint 'chk_address_pin' is violated
INSERT INTO address (user_id, address_line, city, district, state, postal_code) VALUES (5, 'X', 'Pune', 'Pune', 'MH', '011234');

-- C16 delivery before dispatch            -> ERROR 3819 Check constraint 'chk_delivery_dates' is violated
UPDATE delivery SET delivery_date = '2026-11-01' WHERE order_id = 1;

-- C17 unknown payment method              -> ERROR 3819 Check constraint 'chk_payment_method' is violated
INSERT INTO payment (order_id, payment_method, amount) VALUES (3, 'BITCOIN', 960);

-- C18 the application user cannot change the schema (run as market_app):
--   docker exec farmer-marketplace-db mysql -umarket_app -pmarket_app_pass farmer_marketplace -e "DROP TABLE audit_log"
--   -> ERROR 1142 DROP command denied to user 'market_app'

SELECT 'All statements above should have failed - check the errors.' AS result;
