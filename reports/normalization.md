# Normalization and Functional Dependencies

All example rows are real sample data from
[`03_sample_data.sql`](../database/03_sample_data.sql).

## 1. Starting point: an unnormalized order table

A spreadsheet-style design (brief §11.1):

| OrderID | BuyerName | BuyerPhone | Product1 | Qty1 | Product2 | Qty2 | FarmerName | FarmerPhone |
|---|---|---|---|---|---|---|---|---|
| 1 | Priya Kulkarni | 9890000005 | Tomato | 50 | Onion | 40 | Ramesh Patil | 9822000002 |
| 2 | Spice Garden Restaurant | 9890000006 | Basmati Rice | 2 | Toor Dal | 20 | Sunita Deshmukh | 9822000003 |
| 3 | Priya Kulkarni | 9890000005 | Alphonso Mango | 2 | | | Sunita Deshmukh | 9822000003 |
| 5 | Spice Garden Restaurant | 9890000006 | Pomegranate | 5 | Toor Dal | 10 | Sunita Deshmukh | 9822000003 |

Problems:

* **Repeating group** — `Product1/Qty1, Product2/Qty2`. A third product
  needs a new column; most rows waste the empty ones.
* **Update anomaly** — Priya's phone is stored in every order she places.
  Changing it in one row and forgetting another leaves two "truths".
* **Insertion anomaly** — a new farmer's phone cannot be stored until
  somebody orders from them.
* **Deletion anomaly** — deleting order 3 (her only mango order) would also
  lose the fact that Deshmukh Agro sells mangoes at ₹480.
* The price is missing entirely: once the farmer changes the price, the
  order total can no longer be reproduced.

## 2. First normal form (1NF)

Remove the repeating group: one row per order **line**, atomic values only.

`ORDER_LINE_1NF(order_id, product_name, buyer_name, buyer_phone, order_date, quantity, unit_price_at_order, farmer_name, farmer_phone, product_price)`

| order_id | product_name | buyer_name | quantity | unit_price_at_order | farmer_name | … |
|---|---|---|---|---|---|---|
| 1 | Tomato | Priya Kulkarni | 50 | 28.00 | Ramesh Patil | … |
| 1 | Onion | Priya Kulkarni | 40 | 32.00 | Ramesh Patil | … |
| 2 | Basmati Rice | Spice Garden Restaurant | 2 | 9500.00 | Sunita Deshmukh | … |

Key: **(order_id, product_name)**. The anomalies are still there — Priya's
phone repeats on every line.

## 3. Functional dependencies on the 1NF relation

| FD | Meaning |
|---|---|
| order_id → buyer_name, buyer_phone, order_date | an order has one buyer and one date |
| buyer_name → buyer_phone | (really buyer_id → phone; names are used here only for readability) |
| product_name → farmer_name, product_price | a listing belongs to one farm and has one *current* price |
| farmer_name → farmer_phone | |
| order_id, product_name → quantity, unit_price_at_order | quantity and the price *paid* depend on the whole key |

## 4. Second normal form (2NF) — remove partial dependencies

`order_id → buyer_name, order_date` and `product_name → farmer_name, product_price`
depend on **part** of the key. Split them out:

* `ORDERS(order_id, buyer_name, buyer_phone, order_date)`
* `PRODUCT(product_name, farmer_name, farmer_phone, product_price)`
* `ORDER_LINE(order_id, product_name, quantity, unit_price_at_order)`

`unit_price_at_order` stays in `ORDER_LINE`: it is **not** the same as
`product_price`. Spice Garden paid ₹130/kg for Pomegranate in order 5 on
22 Nov; the farmer raised the price to ₹140 on 23 Nov (see `price_history`).
The price paid depends on *which order*, i.e. on the whole key. That is why
the real schema has `order_item.unit_price_at_order`.

## 5. Third normal form (3NF) — remove transitive dependencies

* `ORDERS`: order_id → buyer_name → buyer_phone is transitive. Move buyer
  data to its own relation, referenced by an id.
* `PRODUCT`: product_name → farmer_name → farmer_phone is transitive. Move
  farmer data out.

Using surrogate ids, the result is exactly the implemented schema:
`users`, `farmer_profile`, `buyer_profile`, `product`, `orders`,
`order_item` (plus `category`, `unit`, `address`, …).

## 6. FDs and normal form of every implemented relation

| Relation | Candidate keys | Non-trivial FDs | Highest NF |
|---|---|---|---|
| users | user_id, **email** | user_id → all; email → all | BCNF |
| farmer_profile / buyer_profile | farmer_id / buyer_id | id → all | BCNF |
| category | category_id, **category_name** | both determine all | BCNF |
| unit | unit_code | unit_code → unit_name | BCNF |
| product | product_id | product_id → farmer_id, category_id, unit_code, name, quantity, price, … | BCNF (two farms may both sell "Tomato", so name is not a key) |
| orders | order_id | order_id → buyer_id, delivery_address_id, order_date, order_status, total_amount | BCNF* |
| order_item | order_item_id, **(order_id, product_id)** | both keys → all; **quantity, unit_price_at_order → line_total** | 2NF* |
| payment | payment_id (transaction_reference is unique but nullable) | payment_id → all | BCNF |
| delivery | delivery_id, **order_id** | both → all | BCNF |
| price_history, audit_log | surrogate id | id → all | BCNF |
| address | address_id | address_id → all; **postal_code → district, state ?** | 2NF / 3NF* |

The relations marked * deserve an honest discussion:

**order_item.line_total.** `{quantity, unit_price_at_order} → line_total`,
and `{quantity, unit_price_at_order}` is not a superkey, so strictly this is
a **transitive dependency** and the relation is only in 2NF. The design keeps
it on purpose, as a `GENERATED … STORED` column: MySQL computes it, nobody
can write a wrong value (constraint test C12 shows a manual value is
rejected), so the update anomaly that normalization protects against cannot
happen. Reports read it directly instead of recomputing it.

**orders.total_amount.** It equals the sum of the order's line totals — a
dependency *between* relations, not an FD inside `orders`, so it does not
break BCNF. It is stored redundantly for fast lists; `fn_order_total`
re-derives it and the demo query in 12.12 confirms all five sample orders are
consistent.

**address.postal_code.** If we assume an Indian PIN code always lies in one
district and state, then `postal_code → district, state` is a transitive
dependency and `address` is not in 3NF. In practice some PIN codes cover
several districts, there is no authoritative PIN table in the project, and
addresses are entered by users and rarely updated, so the attributes are kept
together. The fully normalized alternative would be
`PIN_CODE(postal_code, district, state)` + `ADDRESS(…, postal_code)`, which
is lossless because `postal_code` is the key of `PIN_CODE`.

Conclusion: the schema is in **BCNF except** for the two deliberate,
controlled cases above. We do not claim the whole database is in BCNF.

## 7. Armstrong's axioms, with schema examples

| Rule | Statement | Example |
|---|---|---|
| Reflexivity | Y ⊆ X ⇒ X → Y | {order_id, product_id} → order_id |
| Augmentation | X → Y ⇒ XZ → YZ | order_id → buyer_id ⇒ order_id, product_id → buyer_id, product_id |
| Transitivity | X → Y, Y → Z ⇒ X → Z | order_item_id → product_id, product_id → farmer_id ⇒ order_item_id → farmer_id |
| Union (derived) | X → Y, X → Z ⇒ X → YZ | order_id → buyer_id, order_id → order_date ⇒ order_id → buyer_id, order_date |
| Decomposition (derived) | X → YZ ⇒ X → Y | product_id → farmer_id, price ⇒ product_id → price |
| Pseudo-transitivity (derived) | X → Y, WY → Z ⇒ WX → Z | product_id → unit_price, {quantity, unit_price} → line_total ⇒ {quantity, product_id} → line_total (*current* price) |

## 8. Attribute closure

Work on a small universal relation (letters keep it readable):

R(**O**=order_id, **B**=buyer_id, **D**=order_date, **P**=product_id,
**N**=product_name, **F**=farmer_id, **Q**=quantity, **U**=unit_price_at_order,
**L**=line_total)

F = { O → BD,  P → NF,  OP → QU,  QU → L }

**(OP)⁺**

| Step | Using | Closure |
|---|---|---|
| start | | {O, P} |
| 1 | O → BD | {O, P, B, D} |
| 2 | P → NF | {O, P, B, D, N, F} |
| 3 | OP → QU | {O, P, B, D, N, F, Q, U} |
| 4 | QU → L | {O, P, B, D, N, F, Q, U, L} = **R** |

So **OP is a superkey**. **O⁺ = {O, B, D}** and **P⁺ = {P, N, F}**, neither is
R, so OP is minimal: a **candidate key**. No FD has O or P on its right-hand
side, so every key must contain both — OP is the **only** candidate key.
(This matches `UNIQUE(order_id, product_id)` on `order_item`.)

## 9. Minimal (canonical) cover

Start from a deliberately messy set:

G = { O → B, O → D, O → BD, P → N, PN → F, OP → Q, OP → U, OP → L, QU → L, OP → B }

**Step 1 — single attribute on the right.**
O → B, O → D, O → B *(dup)*, O → D *(dup)*, P → N, PN → F, OP → Q, OP → U,
OP → L, QU → L, OP → B. Duplicates removed.

**Step 2 — remove extraneous left-hand attributes.**

* PN → F: P⁺ = {P, N, F} already contains F, so **N is extraneous** → P → F.
* OP → B: O⁺ contains B, so **P is extraneous** → O → B (already present, drop).
* OP → Q, OP → U, OP → L: O⁺ = {O, B, D}, P⁺ = {P, N, F}; neither alone gives
  Q, U or L → keep both attributes.
* QU → L: Q⁺ = {Q}, U⁺ = {U} → keep.

**Step 3 — remove redundant dependencies.**

* OP → L: without it, (OP)⁺ = {O, P, B, D, N, F, Q, U} and then QU → L adds
  L → **redundant**, remove.
* Every other FD is needed (removing it loses its right side from the
  closure of its left side).

**Minimal cover** Fc = { O → B, O → D, P → N, P → F, OP → Q, OP → U, QU → L }

## 10. 3NF synthesis from the minimal cover

Group Fc by left-hand side and make one relation per group:

| Relation | From | Real table |
|---|---|---|
| R1(**O**, B, D) | O → B, O → D | `orders` |
| R2(**P**, N, F) | P → N, P → F | `product` |
| R3(**O, P**, Q, U) | OP → Q, OP → U | `order_item` |
| R4(**Q, U**, L) | QU → L | — (see below) |

The candidate key OP is contained in R3, so no extra key relation is needed.

R4 is the interesting one: the algorithm wants a separate "price × quantity
→ total" relation. That is a mathematical function, not business data, so
the implementation computes it instead — the generated `line_total` column
(discussed in §6).

## 11. Lossless join and dependency preservation

**Lossless.** Decomposing R into R1, R2, R3 (+R4) is lossless because each
join shares a key of one side:

* R3 ∩ R1 = {O}, and O → R1 (O is the key of R1) ✔
* R3 ∩ R2 = {P}, and P → R2 ✔
* R3 ∩ R4 = {Q, U}, and QU → R4 ✔

**A lossy decomposition, with real data.** Split `order_item` into
`A(order_id, quantity)` and `B(product_id, quantity)` and join them back on
quantity:

| A: order_id, quantity | B: product_id, quantity |
|---|---|
| 2, 2.00 (Basmati) | 7 (Basmati), 2.00 |
| 3, 2.00 (Mango) | 5 (Mango), 2.00 |

A ⋈ B on quantity = 2.00 gives **four** rows, including the spurious
(order 2, Mango) and (order 3, Basmati). Quantity is not a key of either
side, so information is lost.

**Dependency preservation.** Every FD in Fc lies entirely inside one of
R1–R4, so each can be checked inside a single table (by its primary key or
`UNIQUE` constraint) without joins ✔.

## 12. Multivalued dependencies and 4NF

The project does not store farmer certifications or languages, so this is an
illustrative example (brief §11.3). Suppose we recorded, per farmer, the
certifications they hold and the languages they speak — two **independent**
facts:

FARMER_INFO(farmer_id, certification, language)

| farmer_id | certification | language |
|---|---|---|
| 2 | Organic India | Marathi |
| 2 | Organic India | Hindi |
| 2 | GAP | Marathi |
| 2 | GAP | Hindi |

farmer_id ↠ certification and farmer_id ↠ language: every certification must
be paired with every language. Adding a third language for farmer 2 needs
**two** new rows. The only key is all three attributes, so the relation is in
BCNF, but the non-trivial MVDs have a left side (farmer_id) that is not a
superkey, so it **violates 4NF**.

4NF decomposition:

| FARMER_CERTIFICATION | | FARMER_LANGUAGE | |
|---|---|---|---|
| **farmer_id** | **certification** | **farmer_id** | **language** |
| 2 | Organic India | 2 | Marathi |
| 2 | GAP | 2 | Hindi |

Four rows become two plus two, and each fact is stored once. The join on
farmer_id reproduces the original exactly (lossless, because
farmer_id ↠ certification holds).
