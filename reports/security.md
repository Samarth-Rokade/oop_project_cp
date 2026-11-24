# Database Security and Data Governance

Brief §16, item by item.

| Requirement | Implementation | Where |
|---|---|---|
| Dedicated database account for the application | The app connects as `market_app`, never as `root` | [`07_users_grants.sql`](../database/07_users_grants.sql), [`db.properties.example`](../db.properties.example) |
| Only the permissions it needs | `SELECT, INSERT, UPDATE, DELETE, EXECUTE` on `farmer_marketplace.*` — no DDL, no `GRANT`. Verified: `DROP TABLE audit_log` as `market_app` → `ERROR 1142 DROP command denied` | 07_users_grants.sql, test C18 |
| Read-only reporting account | `market_report` can only `SELECT` the three views | 07_users_grants.sql |
| Admin credentials separate from app credentials | `root` is used only for setup, demos and backups (scripts read `MYSQL_ROOT_PASSWORD` inside the container) | `scripts/`, `docker-compose.yml` |
| Prepared statements for user input | Every DAO uses `PreparedStatement` with `?`. Dynamic filters (`CatalogDAO`, `AdminDAO.findAudit`) only append fixed SQL fragments; values are always bound. Sort order comes from an enum, never from user text. `LIKE` wildcards in user text are escaped | `src/main/java/dao/` |
| Role and ownership checked before changes | Services take the acting `Farmer`/`Buyer`/`Admin` object; farmers can only change their own products/orders (also `WHERE farmer_id = ?` in SQL); buyers only their own orders; admin accounts cannot be suspended | `ProductService.loadOwned`, `OrderService.transition`, `AdminService.lockEditableUser` |
| Passwords hashed | BCrypt (cost 10, random salt per password). Plain passwords are never stored or logged; password `char[]` arrays are wiped after use. The login error is the same for "unknown email" and "wrong password" so it does not reveal which emails exist | `util/PasswordUtil.java`, `AuthenticationService` |
| Avoid unnecessary personal data | Only name, email, optional phone and delivery addresses. No date of birth, ID numbers or payment card data (payments are simulated). Reporting views expose no password hashes or phone numbers | `04_views.sql` |
| Protect exports and backups | `exports/`, `backups/`, `db.properties` and `.env` are in `.gitignore`, so they are never committed. Backups contain password hashes, so they must be stored like the database itself | `.gitignore` |
| Audit trail | `audit_log`: registrations, order status changes (trigger), rejections with reasons, every admin action. Viewable and searchable in the admin *Activity log* | `06_triggers.sql`, `AuditDAO`, `AuditLogPanel` |
| Sample / synthetic data only | All people, farms, phone numbers and addresses in `03_sample_data.sql` are invented | 03_sample_data.sql |

## Known limitations (local course prototype)

* The local passwords (`rootpass`, `market_app_pass`) are fixed and
  documented so the project runs the same everywhere. A real deployment would
  read them from environment variables or a secrets manager and use TLS for
  the JDBC connection (`useSSL=false` is only acceptable on localhost).
* The database account is shared by all app users; per-user rules are
  enforced in the service layer, not by MySQL privileges. This is the normal
  design for an application server, but it means the Java checks matter.
* Any MySQL session can disable foreign-key checks for itself
  (`SET foreign_key_checks = 0`); the application never does.
