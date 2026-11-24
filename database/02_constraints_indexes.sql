-- =====================================================================
-- 02_constraints_indexes.sql
-- Secondary indexes for the most frequent queries.
--
-- Primary keys, UNIQUE constraints and FK columns are already indexed
-- (InnoDB creates an index for every FK automatically). The indexes
-- below support searches/filters the GUI runs often. Compare them with
-- EXPLAIN in database/demo/07_demo_queries.sql (section 13 of the spec).
-- =====================================================================
USE farmer_marketplace;

-- Buyer search: browse active products by name
CREATE INDEX idx_product_status_name   ON product (listing_status, product_name);

-- Buyer filter: active products in a category, cheapest first
CREATE INDEX idx_product_category_price ON product (category_id, listing_status, price_per_unit);

-- Farmer dashboard: "my listings" by status
CREATE INDEX idx_product_farmer_status ON product (farmer_id, listing_status);

-- Buyer order history, newest first
CREATE INDEX idx_orders_buyer_date     ON orders (buyer_id, order_date);

-- Admin dashboard: orders by status
CREATE INDEX idx_orders_status_date    ON orders (order_status, order_date);

-- Admin "users by role" screens
CREATE INDEX idx_users_role_status     ON users (role, account_status);

-- Price history per product, in time order
CREATE INDEX idx_price_product_time    ON price_history (product_id, changed_at);

-- Audit log browsing
CREATE INDEX idx_audit_entity          ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_time            ON audit_log (action_time);
