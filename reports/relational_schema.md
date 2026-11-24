# Relational Schema

Source: [`database/01_create_schema.sql`](../database/01_create_schema.sql).
All tables use the InnoDB engine (transactions, row-level locks, foreign keys).
Money and quantities are `DECIMAL`, never `FLOAT`, so ₹0.10 + ₹0.20 is exactly ₹0.30.

## 1. Relations

Underlined = primary key, *italic* = foreign key.

| Relation | Attributes | Other keys / checks |
|---|---|---|
| **users** | <u>user_id</u>, full_name, email, phone, password_hash, role, account_status, created_at | `UNIQUE(email)`; role ∈ {FARMER, BUYER, ADMIN}; account_status ∈ {ACTIVE, SUSPENDED, PENDING}; email pattern |
| **farmer_profile** | <u>*farmer_id*</u>, farm_name, farm_address, verification_status | verification ∈ {PENDING, VERIFIED, REJECTED} |
| **buyer_profile** | <u>*buyer_id*</u>, buyer_type | type ∈ {INDIVIDUAL, RETAILER, WHOLESALER, RESTAURANT} |
| **address** | <u>address_id</u>, *user_id*, address_line, city, district, state, postal_code, is_default | PIN `^[1-9][0-9]{5}$` |
| **category** | <u>category_id</u>, category_name, description | `UNIQUE(category_name)` |
| **unit** | <u>unit_code</u>, unit_name | |
| **product** | <u>product_id</u>, *farmer_id*, *category_id*, *unit_code*, product_name, description, available_quantity, price_per_unit, harvest_date, image_path, listing_status, created_at, updated_at | quantity ≥ 0; price > 0; status ∈ {ACTIVE, INACTIVE, OUT_OF_STOCK, REMOVED} |
| **orders** | <u>order_id</u>, *buyer_id*, *delivery_address_id*, order_date, order_status, total_amount, updated_at | total ≥ 0; status ∈ 8 lifecycle values |
| **order_item** | <u>order_item_id</u>, *order_id*, *product_id*, quantity, unit_price_at_order, line_total | `UNIQUE(order_id, product_id)`; quantity > 0; price ≥ 0; `line_total` **generated** = ROUND(quantity × unit_price, 2) |
| **payment** | <u>payment_id</u>, *order_id*, payment_method, amount, payment_status, payment_date, transaction_reference | `UNIQUE(transaction_reference)`; method ∈ {UPI, CARD, NETBANKING, COD} |
| **delivery** | <u>delivery_id</u>, *order_id*, delivery_status, dispatch_date, delivery_date, delivery_partner, tracking_reference | `UNIQUE(order_id)`; delivery_date ≥ dispatch_date |
| **price_history** | <u>price_history_id</u>, *product_id*, old_price, new_price, changed_at, *changed_by* | |
| **audit_log** | <u>log_id</u>, *user_id*, action, entity_type, entity_id, action_time, details | user_id nullable (system events) |

Controlled values use `CHECK` constraints rather than MySQL `ENUM`, so the
rule is standard SQL and visible in the schema. (MySQL does not allow a
`CHECK` on a column that is also used by a foreign key with a referential
action, so foreign-key columns have no `CHECK`.)

## 2. Referential actions

| Foreign key | ON DELETE | Why |
|---|---|---|
| farmer_profile / buyer_profile / address → users | CASCADE | a profile or address has no meaning without its user |
| product → farmer_profile | RESTRICT | products with order history must not vanish |
| product → category, unit | RESTRICT | cannot delete a category or unit that is in use (tests C14 and admin screen) |
| orders → buyer_profile, address | RESTRICT | orders are business records and must be kept |
| order_item → orders | CASCADE | an item is part of its order |
| order_item → product | RESTRICT | ordered products cannot be deleted (test C13); the farmer hides them instead |
| payment, delivery → orders | CASCADE | part of the order |
| price_history → product | CASCADE | history of a deleted (never-ordered) product is not needed |
| price_history.changed_by, audit_log.user_id → users | SET NULL | keep the record even if the user is removed |
| product.unit_code → unit | ON UPDATE CASCADE | renaming a unit code updates its products |

Soft deletion: users and ordered products are never physically deleted by
the application. Users are **suspended** (`account_status`), products are
**hidden** (`INACTIVE`) or **removed by the admin** (`REMOVED`). Only products
that were never ordered can be deleted.

## 3. Views, routines, triggers, users

| Object | Purpose | File |
|---|---|---|
| `active_product_view` | what buyers may order: ACTIVE, stock > 0, farmer verified and active | 04_views.sql |
| `farmer_sales_summary` | orders, units and revenue per farm (excludes rejected/cancelled) | 04_views.sql |
| `order_summary_view` | one row per order with buyer, item count, payment and delivery status | 04_views.sql |
| `fn_stock_level(qty)` | OUT_OF_STOCK / LOW / IN_STOCK | 05_procedures_functions.sql |
| `fn_order_total(order_id)` | recomputes a total from the stored line totals | 05_procedures_functions.sql |
| `sp_buyer_order_history(buyer)` | a buyer's orders, newest first | 05_procedures_functions.sql |
| `sp_low_stock_products(threshold)` | stock alerts | 05_procedures_functions.sql |
| `trg_product_price_history` | AFTER UPDATE: records every price change and who made it | 06_triggers.sql |
| `trg_product_stock_insert/update` | BEFORE INSERT/UPDATE: readable error for negative stock | 06_triggers.sql |
| `trg_orders_status_audit` | AFTER UPDATE: audit row for every order status change | 06_triggers.sql |
| `market_app` | the application's database account: SELECT, INSERT, UPDATE, DELETE, EXECUTE only | 07_users_grants.sql |
| `market_report` | read-only access to the three views | 07_users_grants.sql |

None of the views exposes `password_hash` or phone numbers.

**Dialect note.** The syllabus mentions PL/SQL, which is Oracle's procedural
language. These routines are written in MySQL's stored-program syntax
(`DELIMITER`, `DECLARE`, `SIGNAL SQLSTATE`), which is *not* PL/SQL. If the
instructor expects literal PL/SQL, a separate Oracle exercise is needed.

## 4. Which layer owns each rule

| Rule | Owner | Backup |
|---|---|---|
| Email unique | DB (`uq_users_email`) | Java checks first for a friendly message |
| Stock ≥ 0, price > 0, quantity > 0 | DB (`CHECK`) | Java validates the form; trigger gives a readable message |
| Price history recorded | DB (trigger) — no client can skip it | — |
| Order status change audited | DB (trigger) | — |
| Allowed order status transitions | Java (`OrderStatus.canTransitionTo`) | `CHECK` limits values, not transitions |
| Stock checked and reduced with the order | Java transaction with `SELECT … FOR UPDATE` | `CHECK (available_quantity >= 0)` |
| Order total = sum of line totals | Java computes from DB prices | `fn_order_total` lets anyone verify it |
| Farmer edits only own products | Java (ownership check) | SQL `WHERE farmer_id = ?` |
| Inactive / unverified products not orderable | View + Java re-check under lock | — |
| Ordered product cannot be deleted | DB (`fk_item_product RESTRICT`) | Java explains why first |

Each rule has one owner; where Java checks the same thing it is only to show
a clear message before the database would refuse.

## 5. Codd's rules — how MySQL/InnoDB and this design measure up

| # | Rule | Status here |
|---|---|---|
| 0 | Foundation: managed entirely through relational means | ✔ all data in tables, managed with SQL |
| 1 | Information: all data as values in tables | ✔ including users, statuses, audit trail |
| 2 | Guaranteed access: table + PK + column | ✔ every table has a primary key |
| 3 | Systematic NULLs | ✔ NULL means "not known / not applicable" (e.g. `harvest_date`, `delivery_date`) |
| 4 | Active online catalog | ✔ `information_schema` (used in the restore check) |
| 5 | Comprehensive data sublanguage | ✔ SQL for DDL, DML, DCL, TCL |
| 6 | View updating | ◐ simple views are updatable in MySQL; ours use joins/aggregates and are read-only |
| 7 | Set-level insert/update/delete | ✔ e.g. `INSERT … SELECT` in bulk_test_data.sql |
| 8 | Physical data independence | ✔ adding indexes changed no query or Java code |
| 9 | Logical data independence | ◐ views shield reports; adding `description` to `active_product_view` needed one app change |
| 10 | Integrity independence | ✔ constraints live in the catalog, not in the app |
| 11 | Distribution independence | n/a — single server |
| 12 | Non-subversion | ✘ partly violated: any session (even `market_app`) may run `SET foreign_key_checks = 0` for itself and bypass FKs. The application never does this; it is a property of MySQL, not of the design |

No commercial DBMS satisfies all twelve rules fully; MySQL is "relational
enough" for this project's needs.
