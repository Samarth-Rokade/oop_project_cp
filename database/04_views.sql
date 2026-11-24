-- =====================================================================
-- 04_views.sql
-- Reporting views. None of them expose password_hash or phone numbers.
-- =====================================================================
USE farmer_marketplace;

-- Products a buyer can order right now, with seller and category info
CREATE OR REPLACE VIEW active_product_view AS
SELECT p.product_id,
       p.product_name,
       c.category_name,
       p.unit_code,
       p.available_quantity,
       p.price_per_unit,
       p.harvest_date,
       f.farmer_id,
       f.farm_name,
       u.full_name AS farmer_name
FROM product p
JOIN category       c ON c.category_id = p.category_id
JOIN farmer_profile f ON f.farmer_id   = p.farmer_id
JOIN users          u ON u.user_id     = f.farmer_id
WHERE p.listing_status = 'ACTIVE'
  AND p.available_quantity > 0
  AND f.verification_status = 'VERIFIED'
  AND u.account_status = 'ACTIVE';

-- Sales per farmer, counting only orders that were not rejected/cancelled
CREATE OR REPLACE VIEW farmer_sales_summary AS
SELECT f.farmer_id,
       f.farm_name,
       COUNT(DISTINCT oi.order_id)     AS orders_received,
       COALESCE(SUM(oi.quantity), 0)   AS units_sold,
       COALESCE(SUM(oi.line_total), 0) AS total_revenue
FROM farmer_profile f
LEFT JOIN product p ON p.farmer_id = f.farmer_id
LEFT JOIN (order_item oi
           JOIN orders o ON o.order_id = oi.order_id
                        AND o.order_status NOT IN ('REJECTED', 'CANCELLED'))
       ON oi.product_id = p.product_id
GROUP BY f.farmer_id, f.farm_name;

-- One row per order with buyer, item count, payment and delivery status
CREATE OR REPLACE VIEW order_summary_view AS
SELECT o.order_id,
       o.order_date,
       o.order_status,
       o.total_amount,
       b.buyer_id,
       u.full_name            AS buyer_name,
       b.buyer_type,
       (SELECT COUNT(*) FROM order_item oi WHERE oi.order_id = o.order_id) AS item_count,
       (SELECT pm.payment_status FROM payment pm
         WHERE pm.order_id = o.order_id
         ORDER BY pm.payment_id DESC LIMIT 1)                               AS payment_status,
       d.delivery_status,
       CONCAT(a.city, ' - ', a.postal_code) AS deliver_to
FROM orders o
JOIN buyer_profile b ON b.buyer_id   = o.buyer_id
JOIN users u         ON u.user_id    = b.buyer_id
JOIN address a       ON a.address_id = o.delivery_address_id
LEFT JOIN delivery d ON d.order_id   = o.order_id;
