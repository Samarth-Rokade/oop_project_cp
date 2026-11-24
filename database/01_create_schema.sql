-- =====================================================================
-- 01_create_schema.sql
-- Digital Farmer-to-Buyer Marketplace : tables, keys and constraints
--
-- Design notes
--   * Every table uses InnoDB (transactions, row-level locking, FKs).
--   * Controlled values (roles, statuses) use CHECK constraints so the
--     rule lives in the schema and is visible in the ER/relational docs.
--   * Money uses DECIMAL(10,2) - never FLOAT/DOUBLE for currency.
--   * MySQL does not allow a CHECK on a column that is also used by an
--     FK with ON DELETE/ON UPDATE actions, so FK columns have no CHECKs.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS farmer_marketplace
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE farmer_marketplace;

-- ---------------------------------------------------------------------
-- Users : common data for every account (superclass in the ER model)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id         INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(100)  NOT NULL,
    email           VARCHAR(150)  NOT NULL,
    phone           VARCHAR(15)   NULL,
    password_hash   CHAR(60)      NOT NULL,            -- BCrypt hash, always 60 chars
    role            VARCHAR(10)   NOT NULL,
    account_status  VARCHAR(12)   NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role   CHECK (role IN ('FARMER', 'BUYER', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'PENDING')),
    CONSTRAINT chk_users_email  CHECK (email LIKE '%_@_%._%')
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- FarmerProfile : 1:1 with users (subclass, shared primary key)
-- ---------------------------------------------------------------------
CREATE TABLE farmer_profile (
    farmer_id            INT UNSIGNED  NOT NULL,
    farm_name            VARCHAR(120)  NOT NULL,
    farm_address         VARCHAR(255)  NOT NULL,
    verification_status  VARCHAR(12)   NOT NULL DEFAULT 'PENDING',
    CONSTRAINT pk_farmer_profile PRIMARY KEY (farmer_id),
    CONSTRAINT fk_farmer_user FOREIGN KEY (farmer_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_farmer_verification
        CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- BuyerProfile : 1:1 with users (subclass, shared primary key)
-- ---------------------------------------------------------------------
CREATE TABLE buyer_profile (
    buyer_id    INT UNSIGNED  NOT NULL,
    buyer_type  VARCHAR(15)   NOT NULL DEFAULT 'INDIVIDUAL',
    CONSTRAINT pk_buyer_profile PRIMARY KEY (buyer_id),
    CONSTRAINT fk_buyer_user FOREIGN KEY (buyer_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_buyer_type
        CHECK (buyer_type IN ('INDIVIDUAL', 'RETAILER', 'WHOLESALER', 'RESTAURANT'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Address : one user can have many addresses
-- ---------------------------------------------------------------------
CREATE TABLE address (
    address_id    INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id       INT UNSIGNED  NOT NULL,
    address_line  VARCHAR(255)  NOT NULL,
    city          VARCHAR(80)   NOT NULL,
    district      VARCHAR(80)   NOT NULL,
    state         VARCHAR(80)   NOT NULL,
    postal_code   CHAR(6)       NOT NULL,
    is_default    BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_address PRIMARY KEY (address_id),
    CONSTRAINT fk_address_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_address_pin CHECK (postal_code REGEXP '^[1-9][0-9]{5}$')
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Category and Unit : lookup tables managed by the administrator
-- ---------------------------------------------------------------------
CREATE TABLE category (
    category_id    INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    category_name  VARCHAR(60)   NOT NULL,
    description    VARCHAR(255)  NULL,
    CONSTRAINT pk_category PRIMARY KEY (category_id),
    CONSTRAINT uq_category_name UNIQUE (category_name)
) ENGINE = InnoDB;

CREATE TABLE unit (
    unit_code  VARCHAR(10)  NOT NULL,          -- e.g. KG, QUINTAL, DOZEN
    unit_name  VARCHAR(40)  NOT NULL,
    CONSTRAINT pk_unit PRIMARY KEY (unit_code)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Product : one farmer lists many products; one category has many
-- ---------------------------------------------------------------------
CREATE TABLE product (
    product_id          INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    farmer_id           INT UNSIGNED   NOT NULL,
    category_id         INT UNSIGNED   NOT NULL,
    product_name        VARCHAR(100)   NOT NULL,
    description         VARCHAR(500)   NULL,
    unit_code           VARCHAR(10)    NOT NULL,
    available_quantity  DECIMAL(10,2)  NOT NULL DEFAULT 0,
    price_per_unit      DECIMAL(10,2)  NOT NULL,
    harvest_date        DATE           NULL,
    image_path          VARCHAR(255)   NULL,
    listing_status      VARCHAR(12)    NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_product PRIMARY KEY (product_id),
    CONSTRAINT fk_product_farmer FOREIGN KEY (farmer_id)
        REFERENCES farmer_profile (farmer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id)
        REFERENCES category (category_id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_unit FOREIGN KEY (unit_code)
        REFERENCES unit (unit_code) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_product_qty    CHECK (available_quantity >= 0),
    CONSTRAINT chk_product_price  CHECK (price_per_unit > 0),
    CONSTRAINT chk_product_status CHECK (listing_status IN ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK', 'REMOVED'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Orders : one buyer places many orders
-- (named "orders" because ORDER is an SQL keyword)
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    order_id             INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    buyer_id             INT UNSIGNED   NOT NULL,
    delivery_address_id  INT UNSIGNED   NOT NULL,
    order_date           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_status         VARCHAR(16)    NOT NULL DEFAULT 'PLACED',
    total_amount         DECIMAL(12,2)  NOT NULL DEFAULT 0,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                 ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_orders PRIMARY KEY (order_id),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id)
        REFERENCES buyer_profile (buyer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_address FOREIGN KEY (delivery_address_id)
        REFERENCES address (address_id) ON DELETE RESTRICT,
    CONSTRAINT chk_orders_total  CHECK (total_amount >= 0),
    CONSTRAINT chk_orders_status CHECK (order_status IN
        ('PLACED', 'CONFIRMED', 'PACKED', 'DISPATCHED', 'DELIVERED',
         'REJECTED', 'CANCELLED', 'DELIVERY_FAILED'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- OrderItem : associative entity resolving Orders M:N Product.
-- unit_price_at_order freezes the price so later price changes never
-- alter historical orders.
-- ---------------------------------------------------------------------
CREATE TABLE order_item (
    order_item_id        INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    order_id             INT UNSIGNED   NOT NULL,
    product_id           INT UNSIGNED   NOT NULL,
    quantity             DECIMAL(10,2)  NOT NULL,
    unit_price_at_order  DECIMAL(10,2)  NOT NULL,
    line_total           DECIMAL(12,2)  AS (ROUND(quantity * unit_price_at_order, 2)) STORED,
    CONSTRAINT pk_order_item PRIMARY KEY (order_item_id),
    CONSTRAINT uq_order_item_product UNIQUE (order_id, product_id),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT fk_item_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE RESTRICT,
    CONSTRAINT chk_item_qty   CHECK (quantity > 0),
    CONSTRAINT chk_item_price CHECK (unit_price_at_order >= 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Payment : SIMULATED payments only - no real money is transferred.
-- One order can have several payment rows (retries, refunds).
-- ---------------------------------------------------------------------
CREATE TABLE payment (
    payment_id             INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    order_id               INT UNSIGNED   NOT NULL,
    payment_method         VARCHAR(12)    NOT NULL,
    amount                 DECIMAL(12,2)  NOT NULL,
    payment_status         VARCHAR(10)    NOT NULL DEFAULT 'PENDING',
    payment_date           TIMESTAMP      NULL,
    transaction_reference  VARCHAR(40)    NULL,
    CONSTRAINT pk_payment PRIMARY KEY (payment_id),
    CONSTRAINT uq_payment_txn UNIQUE (transaction_reference),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount >= 0),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('UPI', 'CARD', 'NETBANKING', 'COD')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'CANCELLED'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Delivery : one delivery record per order (status-based, no live GPS)
-- ---------------------------------------------------------------------
CREATE TABLE delivery (
    delivery_id         INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    order_id            INT UNSIGNED  NOT NULL,
    delivery_status     VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    dispatch_date       DATE          NULL,
    delivery_date       DATE          NULL,
    delivery_partner    VARCHAR(80)   NULL,
    tracking_reference  VARCHAR(40)   NULL,
    CONSTRAINT pk_delivery PRIMARY KEY (delivery_id),
    CONSTRAINT uq_delivery_order UNIQUE (order_id),
    CONSTRAINT fk_delivery_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT chk_delivery_status CHECK (delivery_status IN
        ('PENDING', 'PACKED', 'DISPATCHED', 'DELIVERED', 'FAILED')),
    CONSTRAINT chk_delivery_dates CHECK (delivery_date IS NULL OR dispatch_date IS NULL
                                         OR delivery_date >= dispatch_date)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- PriceHistory : filled automatically by trigger (see 06_triggers.sql)
-- ---------------------------------------------------------------------
CREATE TABLE price_history (
    price_history_id  INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    product_id        INT UNSIGNED   NOT NULL,
    old_price         DECIMAL(10,2)  NOT NULL,
    new_price         DECIMAL(10,2)  NOT NULL,
    changed_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by        INT UNSIGNED   NULL,
    CONSTRAINT pk_price_history PRIMARY KEY (price_history_id),
    CONSTRAINT fk_price_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_price_user FOREIGN KEY (changed_by)
        REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- AuditLog : important actions. user_id is NULL for system events.
-- ---------------------------------------------------------------------
CREATE TABLE audit_log (
    log_id       BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id      INT UNSIGNED     NULL,
    action       VARCHAR(40)      NOT NULL,
    entity_type  VARCHAR(30)      NOT NULL,
    entity_id    INT UNSIGNED     NULL,
    action_time  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details      VARCHAR(500)     NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (log_id),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE = InnoDB;
