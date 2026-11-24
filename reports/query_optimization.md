# Indexing, Storage and Query Optimization

## 1. How InnoDB stores the data

* **Pages.** Everything is stored in 16 KB pages (`innodb_page_size = 16384`).
  Pages are cached in the **buffer pool** (128 MB in our container), so a
  repeated query usually reads memory, not disk.
* **Clustered index = the table.** InnoDB keeps each table as a **B+ tree
  ordered by the primary key**; the leaf pages hold the full rows. This is an
  *index-organized* (clustered) file, not a heap file. Looking up
  `product_id = 54321` walks the tree from root to leaf and reads one row —
  `EXPLAIN` shows it as `const` ("Rows fetched before execution").
* **Secondary indexes.** Every other index is a separate B+ tree whose
  leaves hold the indexed columns **plus the primary key**. Finding a row
  through a secondary index therefore means two lookups: the secondary tree,
  then the clustered tree. If all needed columns are already in the
  secondary index, the second lookup is skipped (a **covering index**,
  `Using index` in `EXPLAIN`).
* **Single-level vs multi-level indexes.** A single-level index is one sorted
  list of (key, pointer). A B+ tree is a **multi-level** index: an index on the
  index. With a few hundred keys per 16 KB page, three levels already reach
  millions of rows, so any row is found in about three page reads.
* **File organizations (for comparison).** Heap (unordered, append),
  sequential (sorted file), hashed (bucket by hash of key) and clustered
  B+ tree (InnoDB). MySQL's `MEMORY` engine offers hash indexes; InnoDB uses
  B+ trees, which also support range and prefix searches.
* **Foreign keys need an index.** InnoDB creates one automatically unless a
  usable index already exists. In this schema it reused
  `idx_product_category_price` and `idx_product_farmer_status` (both start
  with the FK column) instead of creating `fk_product_category` /
  `fk_product_farmer`.

## 2. How a query is processed

1. **Parser** — checks syntax and builds a parse tree.
2. **Preprocessor / resolver** — checks that tables and columns exist and that
   the user has the privileges (the `market_app` account fails here for `DROP`).
3. **Optimizer** — cost-based: estimates the rows each access path would read
   (using index statistics refreshed by `ANALYZE TABLE`) and picks the
   cheapest plan, join order and index. `EXPLAIN` shows this plan.
4. **Executor** — runs the plan through the storage engine and returns rows.
   `EXPLAIN ANALYZE` actually runs it and reports the real time per step.

## 3. Indexes in the schema

From `information_schema.statistics` (all created by
[`01_create_schema.sql`](../database/01_create_schema.sql) and
[`02_constraints_indexes.sql`](../database/02_constraints_indexes.sql)):

| Brief §13.1 asks for | Index that serves it | Used by |
|---|---|---|
| `users.email` unique | `uq_users_email (email)` | login |
| `product.category_id` | `idx_product_category_price (category_id, listing_status, price_per_unit)` | buyer category filter sorted by price |
| `product.farmer_id` | `idx_product_farmer_status (farmer_id, listing_status)` | farmer's "My Products" |
| `product.listing_status` | `idx_product_status_name (listing_status, product_name)` | buyer name search |
| `orders.buyer_id` | `idx_orders_buyer_date (buyer_id, order_date)` | buyer's "My Orders", newest first |
| `orders.order_date` | `idx_orders_status_date (order_status, order_date)` and the one above | admin filter by status; date is the *second* column, so a date-only range scan would need its own index — none of our screens does that |
| `order_item.order_id` | `uq_order_item_product (order_id, product_id)` | order details |
| `order_item.product_id` | `fk_item_product (product_id)` (auto-created for the FK) | "has this product been ordered?" before delete |

**Column order matters.** A composite index (A, B, C) helps queries that
filter on A, on A and B, or on A, B and C — the *leftmost prefix*. The
category index puts `category_id` first because every catalogue query fixes
the category and status and then **sorts** by price; with price last, the
rows come out of the index already sorted.

## 4. Experiments

### Set-up

* MySQL 8.4.13 in Docker, laptop, warm buffer pool.
* A throwaway copy of the database with **100,012 products** added by
  [`bulk_test_data.sql`](../database/demo/bulk_test_data.sql) (12 sample rows
  are far too few: MySQL scans tiny tables whether or not an index exists).
* "Without index" uses `IGNORE INDEX (…)` on the same data, so the only
  difference between the two runs is the index.
* Times are from `EXPLAIN ANALYZE` (single run after warm-up). They are
  indicative, not benchmarks; what matters is the **order of magnitude** and
  the **number of rows examined**.

### Results

| Query | Plan **with** index | Time | Plan **without** index | Time |
|---|---|---|---|---|
| A. Active fruits, cheapest 20 (`category_id = 2 AND listing_status = 'ACTIVE' ORDER BY price LIMIT 20`) | `ref` on `idx_product_category_price`, reads 20 rows, no sort | **0.31 ms** | `ALL` (full scan of 100,012 rows) + `Using filesort` | **49.8 ms** |
| B. Name prefix (`listing_status = 'ACTIVE' AND product_name LIKE 'Bulk product 4242%'`) | covering **range** scan on `idx_product_status_name`, 10 rows | **0.22 ms** | full table scan, 100,012 rows | **52.3 ms** |
| C. Leading wildcard (`product_name LIKE '%4242'`) | `ref` on the status part only, ~49,818 index entries examined (`Using where; Using index`) | — | — | — |
| D. Primary key (`product_id = 54321`) | `const` via the clustered index | ≈ 0 ms | — | — |

Raw plan for A **with** the index:

```
-> Limit: 20 row(s)  (actual time=0.301..0.305 rows=20 loops=1)
    -> Index lookup on product using idx_product_category_price
       (category_id=2, listing_status='ACTIVE')  (actual time=0.17..0.173 rows=20 loops=1)
```

and **without** it:

```
-> Limit: 20 row(s)  (actual time=49.8..49.8 rows=20 loops=1)
    -> Sort: product.price_per_unit, limit input to 20 row(s) per chunk  (actual time=49.8..49.8 rows=20)
        -> Filter: ((listing_status = 'ACTIVE') and (category_id = 2))  (actual time=0.0481..45.8 rows=20003)
            -> Table scan on product  (actual time=0.0257..32.2 rows=100012 loops=1)
```

Observations:

* **A:** with the index MySQL jumps to the first (category 2, ACTIVE) entry and
  reads the next 20 — already in price order, so `LIMIT 20` stops after 20
  rows. Without it, it reads all 100,012 rows, keeps 20,003 and sorts them.
  ~160× faster here.
* **B:** a `LIKE 'prefix%'` search is a range on a sorted index. The index
  also contains `product_id` (every secondary index does), so this query
  never touches the table at all (**covering index**).
* **C:** a leading `%` cannot use the sorted order of `product_name`. MySQL
  can still use the `listing_status` part and scans half the index instead of
  the table — better than nothing, but not a real search. Full-text search
  (`FULLTEXT` index or an external engine such as Elasticsearch, see
  [advanced_topics.md](advanced_topics.md)) is the proper fix for
  "contains" searches on large catalogues.
* With the 12 real sample products both plans take well under a millisecond;
  the experiment says nothing about speed at that size, only about the plan.

### The cost of indexes

Same 100,012 rows inserted (`INSERT … SELECT`) into two copies of `product`,
one with and one without the secondary indexes, three runs each:

| | Run 1 | Run 2 | Run 3 | Storage |
|---|---|---|---|---|
| Without secondary indexes | 1,927 ms | 1,950 ms | 1,834 ms | data 8.52 MB, index 0 MB |
| With the 4 secondary indexes | 4,918 ms | 4,767 ms | 4,612 ms | data 8.52 MB, index 15.06 MB |

Every insert, update of an indexed column or delete must also update each
secondary B+ tree, so writes became ~2.5× slower and the indexes take
**more space than the table itself**. This is why indexes are chosen for the
queries the application really runs, not added to every column.

Index sizes from `mysql.innodb_index_stats` (100,012 rows):

| Index | Size |
|---|---|
| PRIMARY (clustered = the table) | 8.52 MB |
| idx_product_status_name | 6.52 MB |
| idx_product_category_price | 4.52 MB |
| idx_product_farmer_status | 2.52 MB |
| fk_product_unit | 1.52 MB |

## 5. How to reproduce

```powershell
.\scripts\backup.ps1
.\scripts\restore.ps1 -File backups\<file>.sql          # creates farmer_marketplace_restore_test
docker exec -i farmer-marketplace-db mysql -uroot -prootpass farmer_marketplace_restore_test < database/demo/bulk_test_data.sql
docker exec -it farmer-marketplace-db mysql -uroot -prootpass farmer_marketplace_restore_test
mysql> EXPLAIN ANALYZE SELECT product_id, product_name, price_per_unit FROM product
       WHERE category_id = 2 AND listing_status = 'ACTIVE' ORDER BY price_per_unit LIMIT 20;
mysql> EXPLAIN ANALYZE SELECT product_id, product_name, price_per_unit FROM product
       IGNORE INDEX (idx_product_category_price, idx_product_status_name)
       WHERE category_id = 2 AND listing_status = 'ACTIVE' ORDER BY price_per_unit LIMIT 20;
```

Never load the bulk data into the real `farmer_marketplace` database.

## 6. Parquet (optional, not implemented)

Parquet is a **columnar** file format: values of one column are stored
together and compressed, so an analytical query such as "total revenue per
category per month" reads only three columns instead of whole rows. It suits
large, append-only analytical datasets (exports for Spark, Pandas or a data
lake), not the transactional database. The project exports CSV; a Parquet
export of the sales report would be a small extension (for example with
DuckDB: `COPY (SELECT * FROM 'sales.csv') TO 'sales.parquet' (FORMAT PARQUET)`).
It is not part of the implementation.
