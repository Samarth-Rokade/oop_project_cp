# Relational Algebra

Each expression below is a real question the application asks. The SQL is
section **12.13** of [`demo_queries.sql`](../database/demo/demo_queries.sql)
(RA1–RA7) and the results are from running it on the sample data.

Abbreviations: `product` = P, `farmer_profile` = F, `orders` = O,
`order_item` = OI, `users` = U.

## RA1 — Selection σ: cheap products

**Question:** which products cost less than ₹50 per unit?

  σ<sub>price_per_unit < 50</sub>(P)

```sql
SELECT * FROM product WHERE price_per_unit < 50;
```

Result: Tomato (28), Onion (32), Potato (22), Spinach (15), Brinjal (35).

## RA2 — Projection π over a selection

**Question:** names and prices of fruits (category 2).

  π<sub>product_name, price_per_unit</sub>(σ<sub>category_id = 2</sub>(P))

```sql
SELECT DISTINCT product_name, price_per_unit FROM product WHERE category_id = 2;
```

Result: Banana 55, Pomegranate 140, Alphonso Mango 480.
`DISTINCT` is needed because relational algebra works on **sets** while SQL
returns **bags** (duplicates allowed).

## RA3 — Natural join ⋈: farms and what they sell

  π<sub>farm_name, product_name</sub>(F ⋈<sub>F.farmer_id = P.farmer_id</sub> P)

```sql
SELECT DISTINCT f.farm_name, p.product_name
FROM farmer_profile f JOIN product p ON p.farmer_id = f.farmer_id;
```

Result: 12 rows (6 Patil Organic Farms, 5 Deshmukh Agro, 1 Jadhav Vegetable Farm).

## RA4 — Union ∪: buyers of fruits or grains

  π<sub>buyer_id</sub>(σ<sub>category_id = 2</sub>(O ⋈ OI ⋈ P))
  ∪ π<sub>buyer_id</sub>(σ<sub>category_id = 3</sub>(O ⋈ OI ⋈ P))

```sql
SELECT o.buyer_id FROM orders o JOIN order_item oi ON oi.order_id = o.order_id
  JOIN product p ON p.product_id = oi.product_id WHERE p.category_id = 2
UNION
SELECT o.buyer_id FROM orders o JOIN order_item oi ON oi.order_id = o.order_id
  JOIN product p ON p.product_id = oi.product_id WHERE p.category_id = 3;
```

Result: buyers 6 (Spice Garden) and 5 (Priya). The two operands are
**union-compatible** (same number of columns, same domains).
Intersection (fruits **and** grains) gives only buyer 6 — see 12.4 in the
demo file, which shows both `INTERSECT` and an `EXISTS` equivalent.

## RA5 — Set difference −: products never ordered

  π<sub>product_id</sub>(P) − π<sub>product_id</sub>(OI)

```sql
SELECT product_id FROM product
EXCEPT
SELECT product_id FROM order_item;
```

Result: 4 (Banana), 9 (Turmeric), 10 (Cow Milk), 11 (Spinach), 12 (Brinjal).
MySQL supports `EXCEPT` and `INTERSECT` from 8.0.31; on older versions the
equivalent is `NOT EXISTS` or `LEFT JOIN … IS NULL` (both in the demo file,
same result). These are the only products a farmer is allowed to delete.

## RA6 — Rename ρ and Cartesian product ×

  π<sub>buyer.full_name, O.order_id</sub>(σ<sub>buyer.user_id = O.buyer_id</sub>(ρ<sub>buyer</sub>(σ<sub>role = 'BUYER'</sub>(U)) × O))

```sql
SELECT buyer.full_name, orders.order_id
FROM (SELECT * FROM users WHERE role = 'BUYER') AS buyer CROSS JOIN orders
WHERE buyer.user_id = orders.buyer_id;
```

3 buyers × 5 orders = 15 combinations; the selection keeps the 5 real pairs.
A join is exactly "Cartesian product followed by selection", which is why
the optimizer never actually builds the 15-row product.

## RA7 — Division ÷: buyers who bought from every verified farm

  π<sub>buyer_id, farmer_id</sub>(O ⋈ OI ⋈ P) ÷ π<sub>farmer_id</sub>(σ<sub>verification_status = 'VERIFIED'</sub>(F))

The divisor is {2 (Patil), 3 (Deshmukh)}. A buyer qualifies only if they
appear in the dividend with **both** farmer ids.

SQL has no division operator. The standard translation is a double negation:
"there is **no** verified farm that this buyer has **not** bought from".

```sql
SELECT u.full_name
FROM users u
WHERE u.role = 'BUYER'
  AND NOT EXISTS (
      SELECT 1 FROM farmer_profile f
      WHERE f.verification_status = 'VERIFIED'
        AND NOT EXISTS (
            SELECT 1 FROM orders o
            JOIN order_item oi ON oi.order_id = o.order_id
            JOIN product p ON p.product_id = oi.product_id
            WHERE o.buyer_id = u.user_id AND p.farmer_id = f.farmer_id));
```

Result: **Priya Kulkarni** only. Her order 1 is from Patil and order 3 from
Deshmukh. Spice Garden bought only from Deshmukh; FreshMart only from Patil.

## Mapping table

| # | Relational algebra | SQL construct |
|---|---|---|
| RA1 | σ (selection) | `WHERE` |
| RA2 | π (projection) | `SELECT DISTINCT col…` |
| RA3 | ⋈ (theta / natural join) | `JOIN … ON` |
| RA4 | ∪ (union) | `UNION` (`UNION ALL` keeps duplicates) |
| — | ∩ (intersection) | `INTERSECT`, or `EXISTS` / `IN` |
| RA5 | − (difference) | `EXCEPT`, `NOT EXISTS`, `LEFT JOIN … IS NULL` |
| RA6 | ρ (rename), × (product) | `AS alias`, `CROSS JOIN` |
| RA7 | ÷ (division) | nested `NOT EXISTS … NOT EXISTS` |
| — | γ (grouping / aggregation, extended RA) | `GROUP BY` with `COUNT/SUM/AVG` (report queries in `ReportDAO`) |

## Product search as the app runs it

The buyer's catalogue search (`CatalogDAO.search`) with text "an", category
Fruits and a maximum price of ₹200 is:

  π<sub>product_id, product_name, …</sub>(σ<sub>name LIKE '%an%' ∧ category_id = 2 ∧ price ≤ 200</sub>(active_product_view))

where `active_product_view` itself is

  σ<sub>listing_status='ACTIVE' ∧ available_quantity>0 ∧ verification_status='VERIFIED' ∧ account_status='ACTIVE'</sub>(P ⋈ Category ⋈ F ⋈ U)

The Java code only adds the σ conditions that the user filled in, so an empty
filter is simply absent rather than "always true".
