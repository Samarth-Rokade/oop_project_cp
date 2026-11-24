-- =====================================================================
-- 07_users_grants.sql   (DCL - spec 12.9)
--
-- Least privilege: the Swing app never connects as root.
--   market_app     -> what the application needs (no DDL, no GRANT)
--   market_report  -> read-only access to the reporting views
--
-- These passwords are for the LOCAL dev database only. If you change
-- them, also change your db.properties.
-- =====================================================================
USE farmer_marketplace;

CREATE USER IF NOT EXISTS 'market_app'@'%'    IDENTIFIED BY 'market_app_pass';
CREATE USER IF NOT EXISTS 'market_report'@'%' IDENTIFIED BY 'market_report_pass';

GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE
    ON farmer_marketplace.* TO 'market_app'@'%';

GRANT SELECT ON farmer_marketplace.active_product_view  TO 'market_report'@'%';
GRANT SELECT ON farmer_marketplace.farmer_sales_summary TO 'market_report'@'%';
GRANT SELECT ON farmer_marketplace.order_summary_view   TO 'market_report'@'%';

FLUSH PRIVILEGES;
