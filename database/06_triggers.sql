-- =====================================================================
-- 06_triggers.sql
--
-- Rule ownership:
--   * Price history  -> DATABASE (trigger). Every price change is
--     recorded no matter which client changes it.
--   * Negative stock -> DATABASE. The CHECK constraint chk_product_qty
--     is the real guard; the BEFORE trigger only turns the violation
--     into a readable message. Java (InventoryService) checks stock
--     first so users normally never reach this point.
--   * Order status transitions -> JAVA (OrderService). The trigger
--     only writes an audit row after a change has happened.
--
-- The app sets  SET @app_user_id = <logged-in user id>;  on its
-- connection so triggers can record who made a change.
-- =====================================================================
USE farmer_marketplace;

DROP TRIGGER IF EXISTS trg_product_price_history;
DROP TRIGGER IF EXISTS trg_product_stock_insert;
DROP TRIGGER IF EXISTS trg_product_stock_update;
DROP TRIGGER IF EXISTS trg_orders_status_audit;

DELIMITER $$

CREATE TRIGGER trg_product_price_history
AFTER UPDATE ON product
FOR EACH ROW
BEGIN
    IF NEW.price_per_unit <> OLD.price_per_unit THEN
        INSERT INTO price_history (product_id, old_price, new_price, changed_by)
        VALUES (NEW.product_id, OLD.price_per_unit, NEW.price_per_unit,
                COALESCE(@app_user_id, NEW.farmer_id));
    END IF;
END$$

CREATE TRIGGER trg_product_stock_insert
BEFORE INSERT ON product
FOR EACH ROW
BEGIN
    IF NEW.available_quantity < 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Available quantity cannot be negative';
    END IF;
END$$

CREATE TRIGGER trg_product_stock_update
BEFORE UPDATE ON product
FOR EACH ROW
BEGIN
    IF NEW.available_quantity < 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Insufficient stock: available quantity cannot go below zero';
    END IF;
END$$

CREATE TRIGGER trg_orders_status_audit
AFTER UPDATE ON orders
FOR EACH ROW
BEGIN
    IF NEW.order_status <> OLD.order_status THEN
        INSERT INTO audit_log (user_id, action, entity_type, entity_id, details)
        VALUES (@app_user_id, 'ORDER_STATUS', 'ORDER', NEW.order_id,
                CONCAT(OLD.order_status, ' -> ', NEW.order_status));
    END IF;
END$$

DELIMITER ;
