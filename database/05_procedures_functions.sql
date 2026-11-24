-- =====================================================================
-- 05_procedures_functions.sql
-- Stored routines.
--
-- Rule ownership (spec 12.12): order creation is a Java service-layer
-- transaction (OrderService), so it is NOT duplicated here. The
-- routines below are read-only helpers used by reports and demos.
-- =====================================================================
USE farmer_marketplace;

DROP FUNCTION  IF EXISTS fn_stock_level;
DROP FUNCTION  IF EXISTS fn_order_total;
DROP PROCEDURE IF EXISTS sp_buyer_order_history;
DROP PROCEDURE IF EXISTS sp_low_stock_products;

DELIMITER $$

-- Classifies a stock quantity: OUT_OF_STOCK / LOW / IN_STOCK
CREATE FUNCTION fn_stock_level(p_quantity DECIMAL(10,2))
RETURNS VARCHAR(12)
DETERMINISTIC NO SQL
BEGIN
    IF p_quantity <= 0 THEN
        RETURN 'OUT_OF_STOCK';
    ELSEIF p_quantity < 10 THEN
        RETURN 'LOW';
    ELSE
        RETURN 'IN_STOCK';
    END IF;
END$$

-- Recomputes an order's total from its stored line totals
CREATE FUNCTION fn_order_total(p_order_id INT UNSIGNED)
RETURNS DECIMAL(12,2)
READS SQL DATA
BEGIN
    DECLARE v_total DECIMAL(12,2);
    SELECT COALESCE(SUM(line_total), 0) INTO v_total
    FROM order_item
    WHERE order_id = p_order_id;
    RETURN v_total;
END$$

-- All orders of one buyer, newest first
CREATE PROCEDURE sp_buyer_order_history(IN p_buyer_id INT UNSIGNED)
READS SQL DATA
BEGIN
    SELECT o.order_id,
           o.order_date,
           o.order_status,
           o.total_amount,
           COUNT(oi.order_item_id) AS item_count
    FROM orders o
    LEFT JOIN order_item oi ON oi.order_id = o.order_id
    WHERE o.buyer_id = p_buyer_id
    GROUP BY o.order_id, o.order_date, o.order_status, o.total_amount
    ORDER BY o.order_date DESC;
END$$

-- Products at or below a stock threshold (for farmer/admin alerts)
CREATE PROCEDURE sp_low_stock_products(IN p_threshold DECIMAL(10,2))
READS SQL DATA
BEGIN
    SELECT p.product_id,
           p.product_name,
           f.farm_name,
           p.available_quantity,
           p.unit_code,
           fn_stock_level(p.available_quantity) AS stock_level
    FROM product p
    JOIN farmer_profile f ON f.farmer_id = p.farmer_id
    WHERE p.available_quantity <= p_threshold
      AND p.listing_status <> 'REMOVED'
    ORDER BY p.available_quantity;
END$$

DELIMITER ;
