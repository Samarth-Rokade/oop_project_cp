# Digital Farmer-to-Buyer Marketplace

*Connecting fresh supply with trusted demand.*

Integrated course project for **OOP with Java** and **DBMS**: a Java Swing
desktop application backed by MySQL through JDBC. The full requirements are
in `Digital_Farmer_to_Buyer_Marketplace_Project_Requirements (1).md`.

---

## What you need

| Tool | Notes |
|---|---|
| **JDK 21** ([Eclipse Temurin](https://adoptium.net/)) | Check with `java -version` |
| **Docker Desktop** | Runs MySQL 8.4. On Windows it sets up WSL2 for you. |
| **VS Code** + *Extension Pack for Java* | VS Code suggests the extensions when you open the folder |

**Maven does not need to be installed.** The project includes the Maven Wrapper
(`mvnw` / `mvnw.cmd`), which downloads the correct Maven version the first time
it runs.

---

## First-time setup (on a new machine)

```bash
git clone <repo-url>
cd <project-folder>

# 1. Start MySQL (first start creates the database and loads sample data)
docker compose up -d

# 2. Build and run the tests
./mvnw.cmd test        # Windows
./mvnw test            # macOS / Linux

# 3. Run the app
./mvnw.cmd -q exec:java
```

Or in VS Code: open the folder, wait for the Java extension to finish
loading, then press **F5** (uses the *Run Marketplace* configuration).

> Always run the app from the **project root folder**, because that is where
> it looks for `db.properties`.

### Demo accounts

All demo accounts use the password **`password123`**.

| Email | Role |
|---|---|
| admin@market.com | Admin |
| ramesh@farm.com, sunita@farm.com | Farmer (verified) |
| vikas@farm.com | Farmer (verification pending) |
| priya@buyer.com, spicegarden@buyer.com, freshmart@buyer.com | Buyer |

---

## Database

| | |
|---|---|
| Host / port | `localhost:3307` (3307 avoids clashing with any locally installed MySQL) |
| Database | `farmer_marketplace` |
| App user | `market_app` / `market_app_pass` (SELECT/INSERT/UPDATE/DELETE/EXECUTE only) |
| Report user | `market_report` / `market_report_pass` (read-only on the views) |
| Root | `root` / `rootpass` (for running the demo queries and backups) |

These credentials are for the **local development database only**.

### Scripts (`database/`)

On the **first** start of the container, they run automatically in order:

| File | Contents |
|---|---|
| `01_create_schema.sql` | Tables, primary/foreign keys, `CHECK`/`UNIQUE` constraints |
| `02_constraints_indexes.sql` | Secondary indexes |
| `03_sample_data.sql` | Demo users, products, orders, payments |
| `04_views.sql` | `active_product_view`, `farmer_sales_summary`, `order_summary_view` |
| `05_procedures_functions.sql` | `sp_buyer_order_history`, `sp_low_stock_products`, `fn_stock_level`, `fn_order_total` |
| `06_triggers.sql` | Price-history, negative-stock and order-status audit triggers |
| `07_users_grants.sql` | Least-privilege users (DCL) |
| `demo/demo_queries.sql` | **Not** auto-run. Queries for the DBMS demo (joins, TCL, EXPLAIN...) |

### Common commands

```bash
docker compose up -d          # start
docker compose down           # stop (data is kept)
docker compose down -v        # DELETE all data; scripts re-run on next start
docker compose logs -f mysql  # view MySQL logs

# MySQL shell inside the container
docker exec -it farmer-marketplace-db mysql -uroot -prootpass farmer_marketplace

# Backup and restore (spec 14.5)
docker exec farmer-marketplace-db mysqldump -uroot -prootpass --routines --triggers farmer_marketplace > backup_marketplace.sql
docker exec -i farmer-marketplace-db mysql -uroot -prootpass farmer_marketplace < backup_marketplace.sql
```

**After you change any file in `database/`**, run `docker compose down -v` and then
`docker compose up -d` so every teammate's database is rebuilt from the same scripts.

### Without Docker

Install MySQL 8.4 Community Server, then run the scripts in order:

```bash
mysql -u root -p < database/01_create_schema.sql
mysql -u root -p < database/02_constraints_indexes.sql
# ...through 07_users_grants.sql
```

Copy `db.properties.example` to `db.properties` and change the port to **3306**.

---

## Project structure

```
src/main/java/
  app/        Main (entry point), Session (logged-in user)
  config/     DatabaseConfig (JDBC connections)
  model/      User (abstract) -> Farmer, Buyer, Admin; enums
  dao/        Database access with PreparedStatement only
  service/    Business rules (UI -> service -> DAO -> MySQL)
  exception/  Custom exceptions
  ui/         Swing screens (FlatLaf look and feel)
  util/       PasswordUtil (BCrypt)
database/     SQL scripts (see above)
reports/      ER diagram, normalization and other DBMS write-ups
exports/      CSV exports from the app (git-ignored)
```

### Architecture rules

- **UI → Service → DAO → Database.** Swing classes never contain SQL.
- All SQL uses `PreparedStatement`.
- Long-running work (database calls) runs in a `SwingWorker`, never on the Event Dispatch Thread.
- Passwords are stored as BCrypt hashes only.
- The app connects as `market_app`, never as root.
