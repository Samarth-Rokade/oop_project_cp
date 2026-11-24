# Test Cases

Three kinds of tests:

1. **Scenario tests T01–T16** from brief §18. Each was run against a
   throwaway copy of the database built from the project scripts (Docker,
   MySQL 8.4.13), by small Java programs that call the real service classes —
   the same code the GUI calls. Dates: 24 Nov 2026.
2. **Direct constraint tests C01–C18** — SQL statements that MySQL must
   reject, run without the application
   ([`constraint_tests.sql`](../database/demo/constraint_tests.sql)).
3. **Unit tests** — 39 JUnit tests that need no database (`mvnw test`).

## 1. Scenario tests (brief §18)

| ID | Scenario | Input | Expected | Actual result | Status |
|---|---|---|---|---|---|
| T01 | Register with a new valid email | Farmer "Asha Pawar", `Asha.Pawar@Gmail.com`, password `farmer2026`; buyer "Hotel Shivneri" | Account created | Farmer #8 created (account PENDING, verification PENDING); buyer #9 created (ACTIVE); both can log in; password stored as 60-char BCrypt hash `$2a$10$…` | ✅ Pass |
| T02 | Register with an existing email | `ASHA.PAWAR@gmail.com` (different case); then two threads registering `race@example.com` at the same moment | Duplicate rejected | "This email is already registered. Try logging in instead."; race: one created, one refused by the UNIQUE constraint; exactly 1 row | ✅ Pass |
| T03 | Log in with invalid credentials | `priya@buyer.com` + wrong password; unknown email | Access denied, clear message | Both: "Incorrect email or password." (same message, does not reveal which emails exist) | ✅ Pass |
| T04 | Farmer adds a valid product | Ramesh adds "  Test Carrot  ", 50 KG, ₹40; pending farmer Asha adds "Grapes" | Saved; visible to eligible buyers | Carrot saved as ACTIVE with the name trimmed. Grapes saved but **not** in the buyer catalogue while Asha is unverified (by design). After the admin verified Vikas and he showed Brinjal, it appeared in the catalogue | ✅ Pass |
| T05 | Farmer enters negative stock | Update stock to −3 | Rejected | "Quantity cannot be negative." (Java); DB also refuses: trigger "Insufficient stock: available quantity cannot go below zero" | ✅ Pass |
| T06 | Buyer searches for an existing product | Name contains "an"; category Fruits; price ₹30–150 (demo Q1–Q4 on `active_product_view`, the view the search uses) | Matching active products shown | "an" → Banana, Alphonso Mango, Pomegranate; Fruits → Banana, Pomegranate, Alphonso Mango; ₹30–150 → Onion, Banana, Cow Milk, Toor Dal, Pomegranate. Out-of-stock Spinach and hidden Brinjal never appear. The Browse screen showed the same 10 products | ✅ Pass |
| T07 | Buyer orders a quantity within stock | Priya: Tomato 5, Mango 1, Onion 2.5 (two farms, UPI) | Order created | Orders #6 (Patil, ₹220.00) and #7 (Deshmukh, ₹480.00), status PLACED, payment PAID; stock 450→445, 760→757.5, 118→117 | ✅ Pass |
| T08 | Buyer orders more than available stock | Turmeric 8 kg when 5 kg left (+ Tomato 1 in the same cart) | Rejected; stock unchanged | `InsufficientStockException`: "…you asked for 8 KG but only 5 KG is available."; no order created; Tomato stock unchanged | ✅ Pass |
| T09 | Order creation fails midway | Trigger forced the 2nd farm's order line to fail after the 1st farm's order was inserted | Everything rolled back | Priya's orders 2 → 2; Tomato 450 → 450; Toor Dal 270 → 270 (see [transaction_demo.md §6](transaction_demo.md#6-rollback-after-a-failure-midway-test-t09)) | ✅ Pass |
| T10 | Farmer edits another farmer's product | Ramesh → update stock of product 5 (Sunita's); delete product 5; Sunita accepts Ramesh's order | Denied | "Product not found in your listings." / "Order not found in your orders." — nothing changed | ✅ Pass |
| T11 | Buyer views another buyer's order | Priya cancels order 5 (Spice Garden's); requests its items | Denied | "Order not found." | ✅ Pass |
| T12 | Cancel an eligible order | Priya cancels order #6 (PLACED, UPI paid) | Status changes; stock restored | CANCELLED, payment REFUNDED, Tomato 445 → 450; audit row "PLACED -> CANCELLED" by Priya Kulkarni. Cancelling a PACKED or DELIVERED order is refused | ✅ Pass |
| T13 | Change a product price | Tomato ₹28 → ₹29 as Ramesh; Test Carrot ₹40 → ₹45.50 | Price history recorded | `price_history` row 40.00 → 45.50, +13.75 %, changed by Ramesh Patil (written by the trigger) | ✅ Pass |
| T14 | Generate a sales report | Farmer and admin reports, all periods | Correct grouped totals | Sunita ₹24,660 = 21,700 + 960 + 2,000; Patil ₹2,680 (cancelled order excluded); Grains & Pulses ₹23,050 = 19,000 + 2,700 + 1,350; matches the SQL in demo 12.6 | ✅ Pass |
| T15 | Run a report while the GUI is active | Open Sales / Reports and interact meanwhile | GUI stays responsive | **Not measured automatically.** By design every report, search and file operation runs in a `SwingWorker` (`ui/Async.java`) and the window shows a wait cursor instead of freezing. Check during the live demo by scrolling or switching tabs while a report loads | ⏳ Verify in demo |
| T16 | Back up and restore the database | `scripts/backup.ps1`, then `scripts/restore.ps1` into a separate database | Data restored | 38.5 KB dump; all 13 tables and 8 routines/triggers match row-for-row; restored procedure and trigger work | ✅ Pass |

### Additional scenarios tested

| Scenario | Result |
|---|---|
| Two buyers buy the last units at the same instant | One placed, the other rejected; no overselling (row lock) |
| Price changed after it was put in the cart | Order blocked: "The price of Potato changed from 22.00 to 24.00 per KG…" |
| Order delivered with cash on delivery | Payment PENDING → PAID with today's date |
| Farmer rejects an order (reason required) | Stock restored, payment refunded, reason in audit log |
| Skipping a lifecycle step (PLACED → PACKED) | "Order #7 is placed, so it cannot be packed." |
| Admin suspends a farmer | Login blocked; farmer's products vanish from the catalogue; reactivating restores both |
| Admin removes a listing | Farmer cannot re-activate it; restore brings it back as hidden |
| Delete a product that has been ordered | Refused (Java message + FK `RESTRICT`) |
| CSV import with 4 rows, 2 bad | 2 imported in one transaction; "Row 4: price "abc" is not a number", "Row 5: unknown category "Herbs"" |

## 2. Direct database constraint tests

Run: `docker exec -i farmer-marketplace-db mysql -uroot -prootpass --force farmer_marketplace < database/demo/constraint_tests.sql`

| ID | Statement | MySQL response | Status |
|---|---|---|---|
| C01 | Duplicate email | 1062 Duplicate entry … `users.uq_users_email` | ✅ |
| C02 | Role `GUEST` | 3819 Check constraint `chk_users_role` | ✅ |
| C03 | Email `not-an-email` | 3819 `chk_users_email` | ✅ |
| C04 | Product with stock −1 | 1644 Available quantity cannot be negative (trigger) | ✅ |
| C05 | Price 0 | 3819 `chk_product_price` | ✅ |
| C06 | Product of farmer 999 | 1452 FK `fk_product_farmer` | ✅ |
| C07 | Buyer id used as farmer | 1452 FK `fk_product_farmer` | ✅ |
| C08 | Unit `BUCKET` | 1452 FK `fk_product_unit` | ✅ |
| C09 | Order status `LOST` | 3819 `chk_orders_status` | ✅ |
| C10 | Order item quantity 0 | 3819 `chk_item_qty` | ✅ |
| C11 | Same product twice in one order | 1062 `order_item.uq_order_item_product` | ✅ |
| C12 | Writing `line_total` by hand | 3105 value for generated column not allowed | ✅ |
| C13 | Delete an ordered product | 1451 FK `fk_item_product` | ✅ |
| C14 | Delete a category in use | 1451 FK `fk_product_category` | ✅ |
| C15 | PIN code `011234` | 3819 `chk_address_pin` | ✅ |
| C16 | Delivery date before dispatch date | 3819 `chk_delivery_dates` | ✅ |
| C17 | Payment method `BITCOIN` | 3819 `chk_payment_method` | ✅ |
| C18 | `DROP TABLE` as `market_app` | 1142 DROP command denied | ✅ |

## 3. Unit tests (no database)

`./mvnw.cmd test` → **39 tests, 0 failures**.

| Class | Tests | Covers |
|---|---|---|
| `PasswordUtilTest` | 3 | hashing, verification, rejection of non-BCrypt values |
| `ProductServiceValidationTest` | 11 | product rules, all errors reported together, stock-driven status, low-stock threshold |
| `CartTest` | 6 | merging quantities, stock limit, totals, grouping by farm, change events |
| `OrderStatusTest` | 4 | allowed and forbidden lifecycle transitions |
| `RegistrationValidationTest` | 6 | password policy, email/phone format, normalisation |
| `CsvTest` | 4 | quoting, parsing quotes/commas/line breaks, UTF-8 BOM round trip |
| `ProductImportTest` | 4 | header in any order, per-row problems with row numbers |
| `ReceiptFormatterTest` | 1 | receipt contents and total |
