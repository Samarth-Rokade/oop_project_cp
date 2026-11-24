-- =====================================================================
-- 03_sample_data.sql
-- Demo data. Every sample account uses the password:  password123
-- (stored only as a BCrypt hash).
--
--   admin@market.com        ADMIN
--   ramesh@farm.com         FARMER  (verified)
--   sunita@farm.com         FARMER  (verified)
--   vikas@farm.com          FARMER  (verification pending)
--   priya@buyer.com         BUYER   (individual)
--   spicegarden@buyer.com   BUYER   (restaurant)
--   freshmart@buyer.com     BUYER   (retailer)
--
-- IDs are explicit so demo queries and test cases can refer to them.
-- Order totals equal the sum of their line totals.
-- =====================================================================
USE farmer_marketplace;

SET @pw = '$2a$10$oN/o.Qfl0fFPRr2Wl560k.VSw.0J6tjFMx6JW1yK8ymPEi3.SFvXu';

INSERT INTO users (user_id, full_name, email, phone, password_hash, role, account_status, created_at) VALUES
 (1, 'Marketplace Admin',   'admin@market.com',      '9000000001', @pw, 'ADMIN',  'ACTIVE',  '2026-01-05 10:00:00'),
 (2, 'Ramesh Patil',        'ramesh@farm.com',       '9822000002', @pw, 'FARMER', 'ACTIVE',  '2026-01-10 09:15:00'),
 (3, 'Sunita Deshmukh',     'sunita@farm.com',       '9822000003', @pw, 'FARMER', 'ACTIVE',  '2026-01-12 11:30:00'),
 (4, 'Vikas Jadhav',        'vikas@farm.com',        '9822000004', @pw, 'FARMER', 'PENDING', '2026-03-02 16:45:00'),
 (5, 'Priya Kulkarni',      'priya@buyer.com',       '9890000005', @pw, 'BUYER',  'ACTIVE',  '2026-01-20 13:00:00'),
 (6, 'Spice Garden Restaurant', 'spicegarden@buyer.com', '9890000006', @pw, 'BUYER', 'ACTIVE', '2026-02-01 10:20:00'),
 (7, 'FreshMart Retail',    'freshmart@buyer.com',   '9890000007', @pw, 'BUYER',  'ACTIVE',  '2026-02-14 12:10:00');

INSERT INTO farmer_profile (farmer_id, farm_name, farm_address, verification_status) VALUES
 (2, 'Patil Organic Farms',  'Gat No. 112, Manchar, Ambegaon, Pune',  'VERIFIED'),
 (3, 'Deshmukh Agro',        'Plot 7, Rahuri, Ahmednagar',            'VERIFIED'),
 (4, 'Jadhav Vegetable Farm','Near Gram Panchayat, Junnar, Pune',     'PENDING');

INSERT INTO buyer_profile (buyer_id, buyer_type) VALUES
 (5, 'INDIVIDUAL'),
 (6, 'RESTAURANT'),
 (7, 'RETAILER');

INSERT INTO address (address_id, user_id, address_line, city, district, state, postal_code, is_default) VALUES
 (1, 5, 'Flat 302, Sai Residency, Karve Road', 'Pune',   'Pune',       'Maharashtra', '411004', TRUE),
 (2, 6, 'Shop 4, FC Road',                     'Pune',   'Pune',       'Maharashtra', '411005', TRUE),
 (3, 7, 'Warehouse 9, Bhosari MIDC',           'Pune',   'Pune',       'Maharashtra', '411026', TRUE),
 (4, 5, 'Plot 18, Baner Road',                 'Pune',   'Pune',       'Maharashtra', '411045', FALSE),
 (5, 2, 'Gat No. 112, Manchar',                'Manchar','Pune',       'Maharashtra', '410503', TRUE),
 (6, 3, 'Plot 7, Rahuri',                      'Rahuri', 'Ahmednagar', 'Maharashtra', '413705', TRUE);

INSERT INTO category (category_id, category_name, description) VALUES
 (1, 'Vegetables',       'Fresh seasonal vegetables'),
 (2, 'Fruits',           'Fresh fruits'),
 (3, 'Grains & Pulses',  'Rice, wheat, dals and other staples'),
 (4, 'Dairy',            'Milk and milk products'),
 (5, 'Spices',           'Whole and ground spices');

INSERT INTO unit (unit_code, unit_name) VALUES
 ('KG',      'Kilogram'),
 ('QUINTAL', 'Quintal (100 kg)'),
 ('TONNE',   'Tonne (1000 kg)'),
 ('DOZEN',   'Dozen'),
 ('LITRE',   'Litre'),
 ('PIECE',   'Piece'),
 ('BUNCH',   'Bunch');

INSERT INTO product (product_id, farmer_id, category_id, product_name, description, unit_code,
                     available_quantity, price_per_unit, harvest_date, listing_status, created_at) VALUES
 (1,  2, 1, 'Tomato',          'Firm red hybrid tomatoes',        'KG',      450.00,   28.00, '2026-11-18', 'ACTIVE',       '2026-11-18 08:00:00'),
 (2,  2, 1, 'Onion',           'Red onion, medium size',          'KG',      760.00,   32.00, '2026-11-10', 'ACTIVE',       '2026-11-10 08:00:00'),
 (3,  2, 1, 'Potato',          'Table potatoes',                  'KG',     1000.00,   22.00, '2026-11-05', 'ACTIVE',       '2026-11-05 08:00:00'),
 (4,  2, 2, 'Banana',          'Robusta bananas',                 'DOZEN',   190.00,   55.00, '2026-11-20', 'ACTIVE',       '2026-11-20 08:00:00'),
 (5,  3, 2, 'Alphonso Mango',  'Ratnagiri Alphonso, export grade','DOZEN',   118.00,  480.00, '2026-11-15', 'ACTIVE',       '2026-11-15 08:00:00'),
 (6,  3, 2, 'Pomegranate',     'Bhagwa variety',                  'KG',      145.00,  140.00, '2026-11-12', 'ACTIVE',       '2026-11-12 08:00:00'),
 (7,  3, 3, 'Basmati Rice',    'Aged basmati, long grain',        'QUINTAL',  38.00, 9500.00, '2026-10-25', 'ACTIVE',       '2026-10-25 08:00:00'),
 (8,  3, 3, 'Toor Dal',        'Unpolished toor dal',             'KG',      280.00,  135.00, '2026-10-30', 'ACTIVE',       '2026-10-30 08:00:00'),
 (9,  3, 5, 'Turmeric',        'Rajapuri turmeric fingers',       'KG',        8.00,  180.00, '2026-09-28', 'ACTIVE',       '2026-09-28 08:00:00'),
 (10, 2, 4, 'Cow Milk',        'Fresh A2 cow milk, daily',        'LITRE',   100.00,   60.00, '2026-11-24', 'ACTIVE',       '2026-11-24 06:00:00'),
 (11, 2, 1, 'Spinach',         'Palak bunches',                   'BUNCH',     0.00,   15.00, '2026-11-19', 'OUT_OF_STOCK', '2026-11-19 07:00:00'),
 (12, 4, 1, 'Brinjal',         'Purple brinjal',                  'KG',      200.00,   35.00, '2026-11-21', 'INACTIVE',     '2026-11-21 07:00:00');

-- updated_at would otherwise be the time this script ran
UPDATE product SET updated_at = created_at;

INSERT INTO orders (order_id, buyer_id, delivery_address_id, order_date, order_status, total_amount) VALUES
 (1, 5, 1, '2026-11-18 18:30:00', 'DELIVERED',   2680.00),
 (2, 6, 2, '2026-11-21 09:10:00', 'CONFIRMED',  21700.00),
 (3, 5, 4, '2026-11-23 20:05:00', 'PLACED',       960.00),
 (4, 7, 3, '2026-11-19 11:00:00', 'CANCELLED',    550.00),
 (5, 6, 2, '2026-11-22 08:45:00', 'DISPATCHED',  1200.00);

UPDATE orders SET updated_at = order_date;

-- line_total is a generated column and is calculated by MySQL
INSERT INTO order_item (order_item_id, order_id, product_id, quantity, unit_price_at_order) VALUES
 (1, 1, 1, 50.00,   28.00),   -- 1400.00
 (2, 1, 2, 40.00,   32.00),   -- 1280.00
 (3, 2, 7,  2.00, 9500.00),   -- 19000.00
 (4, 2, 8, 20.00,  135.00),   -- 2700.00
 (5, 3, 5,  2.00,  480.00),   -- 960.00
 (6, 4, 3, 25.00,   22.00),   -- 550.00 (cancelled - stock restored)
 (7, 5, 4, 10.00,   55.00),   -- 550.00
 (8, 5, 6,  5.00,  130.00);   -- 650.00 (ordered before price rose to 140)

INSERT INTO payment (payment_id, order_id, payment_method, amount, payment_status, payment_date, transaction_reference) VALUES
 (1, 1, 'UPI',        2680.00, 'PAID',     '2026-11-18 18:31:00', 'SIM-UPI-000001'),
 (2, 2, 'NETBANKING',21700.00, 'PAID',     '2026-11-21 09:12:00', 'SIM-NB-000002'),
 (3, 3, 'COD',         960.00, 'PENDING',  NULL,                  NULL),
 (4, 4, 'UPI',         550.00, 'REFUNDED', '2026-11-19 11:02:00', 'SIM-UPI-000004'),
 (5, 5, 'CARD',       1200.00, 'PAID',     '2026-11-22 08:46:00', 'SIM-CARD-000005');

INSERT INTO delivery (delivery_id, order_id, delivery_status, dispatch_date, delivery_date, delivery_partner, tracking_reference) VALUES
 (1, 1, 'DELIVERED',  '2026-11-19', '2026-11-20', 'KisanLogistics', 'KL-1001'),
 (2, 2, 'PENDING',    NULL,         NULL,         NULL,             NULL),
 (3, 5, 'DISPATCHED', '2026-11-23', NULL,         'KisanLogistics', 'KL-1005');

INSERT INTO price_history (product_id, old_price, new_price, changed_at, changed_by) VALUES
 (6, 130.00, 140.00, '2026-11-23 07:30:00', 3),
 (1,  30.00,  28.00, '2026-11-20 07:00:00', 2);

INSERT INTO audit_log (user_id, action, entity_type, entity_id, action_time, details) VALUES
 (1, 'VERIFY_FARMER',  'FARMER_PROFILE', 2, '2026-01-11 10:00:00', 'Farmer verified by admin'),
 (1, 'VERIFY_FARMER',  'FARMER_PROFILE', 3, '2026-01-13 10:00:00', 'Farmer verified by admin'),
 (7, 'CANCEL_ORDER',   'ORDER',          4, '2026-11-19 11:30:00', 'Buyer cancelled; stock restored'),
 (2, 'ORDER_STATUS',   'ORDER',          1, '2026-11-20 17:00:00', 'DISPATCHED -> DELIVERED');
