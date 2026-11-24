# Syllabus Coverage Matrix

Legend: ✅ implemented in the application (file given) · 📝 explained in a
report · ❌ **gap** — not in the code yet; needs a small, clearly labelled
demonstration (brief §19 says language-only topics should be shown that way
rather than forced into the business design).

Paths are relative to `src/main/java/`.

## 1. OOP with Java

### Unit 1 — Classes and objects

| Topic | Status | Evidence |
|---|---|---|
| Need for OOP / characteristics | ✅📝 | Marketplace split into objects with one responsibility each — [architecture.md](architecture.md) |
| Classes and objects | ✅ | `model/User, Farmer, Buyer, Product, Order, Payment, Cart …` |
| Object storage (references, lifecycle) | 📝 | See note A below |
| Access modifiers | ✅ | private fields everywhere; `protected` constructor in `model/User`; package-private `CartItem.setQuantity` so only `Cart` can change it |
| `this` reference | ✅ | constructors and setters of every model class |
| `main` method | ✅ | `app/Main.java` |
| Static vs instance methods | ✅ | static utilities `util/Formats`, `util/PasswordUtil`; instance methods on models/services |
| Static block | ✅ | `model/OrderStatus` — `static { TRANSITIONS.put(…) }` builds the lifecycle table |
| Instance initializer block | ❌ | none |
| Constructors (default, parameterized, copy, overloaded) | ✅ | `Product()` and copy constructor `Product(Product)`; `Address()` and `Address(…)`; `ProductService()` / `ProductService(dao, dao)` |

### Unit 2 — I/O, arrays, strings, methods

| Topic | Status | Evidence |
|---|---|---|
| Input/output streams | ✅ | `util/Csv` (`BufferedWriter`, `Files.readString`), `util/ReceiptFormatter` |
| Scanner | ❌ | none (GUI app) |
| Arrays, multidimensional arrays | ✅ | `String[][] rows` in `ui/FarmerDashboard.profilePage`; `int[] widths` in table setup |
| `java.util.Arrays` | ◐ | `Arrays.fill` wipes passwords, `Arrays.equals` compares them (`RegistrationService`, `LoginFrame`); **sort/binarySearch ❌** |
| String, StringBuilder | ✅ | `StringBuilder` in `ReceiptFormatter`, `CatalogDAO` (dynamic SQL), `CartPanel.reviewHtml` |
| StringBuffer | ❌ | none |
| Methods, parameter passing | ✅ | services receive model objects (`placeOrder(Buyer, Cart, int, PaymentMethod)`) |
| Returning multiple values | ✅ | records `ReportService.FarmerReport`, `ProductImportService.ImportPreview`, `OrderDAO.LockedProduct` |

### Unit 3 — Inheritance and polymorphism

| Topic | Status | Evidence |
|---|---|---|
| Inheritance | ✅ | `User → Farmer, Buyer, Admin`; `ValidationException → InsufficientStockException`; `SidebarDashboard → Farmer/Buyer/AdminDashboard` |
| Constructor in inheritance (`super()`) | ✅ | `Farmer(…) { super(…); … }` |
| `final` with inheritance | ◐ | `final` classes (`util/Csv`, `ui/Dialogs`, …), `final` fields; **final method ❌** |
| Superclass members | ✅ | `farmer.getFullName()` inherited from `User` |
| Same data member in parent/child (field hiding) | ❌ | none (avoided on purpose) |
| Base reference to derived object | ✅ | `User user = authService.login(…)` holds a `Farmer`/`Buyer`/`Admin`; `DashboardFrame.createContent` uses `instanceof` patterns |
| Method overloading | ✅ | `getOrdersOf(Buyer)` / `getOrdersOf(Farmer)`, `getItems(Buyer, …)` / `getItems(Farmer, …)`; `Async.run` with 3 or 4 parameters |
| Overloading `main()` | ❌ | none |
| Dynamic binding | ✅ | `user.getDashboardTitle()`, `user.getRole()` — overridden in each subclass |
| Static binding | ✅ | overloaded `getOrdersOf` resolved at compile time; static methods |
| Method hiding (static) | ❌ | none |
| Private and final methods | ◐ | many private helpers; **final method ❌** |
| Passing and returning objects | ✅ | DAO/service methods throughout |

### Unit 4 — Abstraction and nested types

| Topic | Status | Evidence |
|---|---|---|
| Interfaces | ✅ | `interfaces/ProductOperations`, `interfaces/OrderOperations` |
| Abstract class | ✅ | `model/User`, `ui/SidebarDashboard` |
| Abstract class vs interface | 📝 | [architecture.md §2](architecture.md#2-class-diagram-model-and-service-layer) |
| Nested interface | ✅ | `ProductFormDialog.SaveAction`, `OrderService.TransactionStep`, `UserDAO.ProfileInsert` |
| Nested (static) class / record | ✅ | `Tables.PaddedRenderer`, `BrowsePanel.ScrollableGrid`, `BeanTableModel.Column` |
| Inner (non-static) class | ❌ | none |
| Anonymous class | ✅ | `new DocumentListener() {…}` (`ProductPanel`), `new AbstractAction() {…}`, `new SwingWorker<>() {…}` (`ui/Async`) |

### Unit 5 — Exceptions, collections, threads

| Topic | Status | Evidence |
|---|---|---|
| Exception handling | ✅ | try/catch/finally, try-with-resources on every JDBC call |
| Checked vs unchecked | ✅ | checked `ValidationException` (user can fix it) vs unchecked `DataAccessException` wrapping `SQLException` |
| `throw` / `throws` | ✅ | service methods `throws ValidationException` |
| User-defined exceptions | ✅ | `InsufficientStockException`, `InvalidOrderStatusException`, `DuplicateEntryException` |
| Collections: ArrayList, Map, Set | ✅ | `ArrayList`, `LinkedHashMap` (`Cart`), `EnumMap`/`EnumSet` (`OrderStatus`), `HashMap` |
| Vector | ❌ | none |
| Iterator vs for-each | ◐ | for-each and `forEach` everywhere; **explicit `Iterator` ❌** |
| Multithreading | ✅ | `SwingWorker` for all DB/file work (`ui/Async`); concurrency tests used `ExecutorService` + `CountDownLatch` |
| Thread life cycle, priority, methods | ❌ | not demonstrated beyond `SwingWorker` |

### Unit 6 — Files, JDBC, Swing, Java 8

| Topic | Status | Evidence |
|---|---|---|
| File handling | ✅ | CSV export/import, receipts (`util/Csv`, `service/ProductImportService`, `util/ReceiptFormatter`) |
| Primitive/object data streams | ❌ | no `DataOutputStream` / `ObjectOutputStream` (the brief forbids serialized objects as a database replacement, so this belongs in a separate demo) |
| JDBC | ✅ | `config/DatabaseConfig`, `dao/*`, transactions in `service/OrderService` |
| Swing components | ◐ | `JFrame`, `JPanel`, `JLabel`, `JTextField`, `JPasswordField`, `JButton`, `JTable`, `JComboBox` (choice), `JOptionPane`, `JSpinner`, `JTabbedPane`, `JSplitPane`, `JFileChooser`; **`JList` ❌** |
| Layout managers | ✅ | Flow, Border, Grid, **Card** (`SidebarDashboard`, `RegisterDialog`), GridBag, Box |
| Event handling | ✅ | buttons; mouse (double-click to edit, card click/hover); keyboard (Ctrl+N, Ctrl+F, F5, Enter, Delete, Esc) |
| Lambda expressions | ✅ | listeners `e -> …`, `Async.run(…)` callbacks |
| Stream API | ✅ | filtering/sorting/grouping in `ProductPanel.applyFilters`, `Cart.groupByFarmer`, `OrderService.placeOrder` |
| Functional interfaces | ✅ | `Predicate` (filters), `Consumer`, `BiConsumer`, `BiPredicate`, `Function`, `Callable`; custom `@FunctionalInterface`s |
| Default / static interface methods | ✅ | `ProductOperations.isLowStock` (default and static) |
| `forEach()` | ✅ | `listeners.forEach(Runnable::run)`, `history.forEach(…)` |
| `Optional` | ✅ | `UserDAO.findByEmail`, `ProductDAO.findById`, `selected()` helpers |
| `StringJoiner` | ✅ | validation messages in `ProductService.validate`, order-id lists |
| Parallel array sorting | ❌ | none |

**Note A — object storage.** Objects live on the heap; variables hold
references. `Session.currentUser` keeps the logged-in `User` reachable for
the whole session; when the user logs out the reference is cleared and the
object becomes eligible for garbage collection. Because two variables can
point to the same object, `ProductFormDialog` edits a **copy**
(`new Product(original)`) so pressing *Cancel* never changes the product shown
in the table.

### OOP gaps — summary

Scanner · `Arrays.sort`/`binarySearch` · `StringBuffer` · instance
initializer block · field hiding · overloading `main` · static method hiding ·
final method · non-static inner class · `Vector` · explicit `Iterator` ·
thread life cycle/priority · `DataOutputStream`/`ObjectOutputStream` ·
`JList` · `Arrays.parallelSort`.

All are language features with no natural place in the marketplace. They can
be covered by a few small console programs in a separate `demo` package.

## 2. DBMS

| Unit / topic | Status | Evidence |
|---|---|---|
| Need for a DBMS / evolution | 📝 | Files and spreadsheets cause the anomalies in [normalization.md §1](normalization.md#1-starting-point-an-unnormalized-order-table); one shared, consistent database for three roles |
| DBMS architecture | 📝 | Three-schema view: **external** = views and the app's screens, **conceptual** = the relational schema, **internal** = InnoDB pages and B+ trees ([query_optimization.md §1](query_optimization.md#1-how-innodb-stores-the-data)); layered app in [architecture.md](architecture.md) |
| Design process | 📝 | [er_diagram.md §1](er_diagram.md#1-design-process) |
| ER model | ✅ | [er_diagram.md](er_diagram.md) |
| Extended ER (specialization) | ✅ | [er_diagram.md §3](er_diagram.md#3-extended-er-specialization-of-users) |
| Relational model, keys, constraints | ✅ | [relational_schema.md](relational_schema.md), `01_create_schema.sql` |
| Codd's rules | 📝 | [relational_schema.md §5](relational_schema.md#5-codds-rules--how-mysqlinnodb-and-this-design-measure-up) |
| Normalization: need, FDs, Armstrong's axioms, closure, minimal cover | ✅📝 | [normalization.md §1–9](normalization.md) |
| Decomposition: lossless join, dependency preservation | ✅📝 | [normalization.md §11](normalization.md#11-lossless-join-and-dependency-preservation) (incl. a lossy example with real rows) |
| 1NF, 2NF, 3NF, BCNF | ✅📝 | [normalization.md §2–6](normalization.md) — with the honest exceptions |
| MVD and 4NF | 📝 | [normalization.md §12](normalization.md#12-multivalued-dependencies-and-4nf) |
| Relational synthesis | 📝 | [normalization.md §10](normalization.md#10-3nf-synthesis-from-the-minimal-cover) |
| Relational algebra | ✅ | [relational_algebra.md](relational_algebra.md), demo 12.13 |
| DDL, DML | ✅ | `database/01–07`, demo 12.1–12.2 |
| SELECT, set operations, functions | ✅ | demo 12.3–12.5 |
| Aggregates, GROUP BY, HAVING | ✅ | demo 12.6; `ReportDAO` |
| Joins (inner, left, multi-table, self; ON vs WHERE) | ✅ | demo 12.7 |
| Nested queries | ✅ | demo 12.8 |
| DCL, TCL | ✅ | `07_users_grants.sql`, demo 12.9–12.10; JDBC transactions in `OrderService` |
| Stored procedures, functions, triggers | ✅ | `05_*.sql`, `06_*.sql`. **MySQL syntax, not Oracle PL/SQL** — confirm with the instructor whether a separate PL/SQL exercise is needed |
| Relational algebra → SQL mapping | ✅ | [relational_algebra.md](relational_algebra.md#mapping-table) |
| Storage and file structures | 📝 | [query_optimization.md §1](query_optimization.md#1-how-innodb-stores-the-data) |
| Indexed files, index levels | ✅📝 | indexes in `02_*.sql`; B+ tree levels explained |
| Query processing | 📝 | [query_optimization.md §2](query_optimization.md#2-how-a-query-is-processed) |
| Query optimization (EXPLAIN, index comparison) | ✅ | [query_optimization.md §4](query_optimization.md#4-experiments) — measured on 100,012 rows |
| Parquet | 📝 | explained, not implemented (optional) |
| Transactions and ACID | ✅ | `OrderService`; [transaction_demo.md §1, §3](transaction_demo.md) |
| Transaction states, schedules | 📝 | [transaction_demo.md §2, §4](transaction_demo.md) |
| Conflict / view serializability, precedence graph | 📝 | [transaction_demo.md §4](transaction_demo.md#4-schedules-two-buyers-the-last-units-of-turmeric) |
| Concurrency control (locks, isolation, overselling test) | ✅ | `SELECT … FOR UPDATE`, lock ordering; tested with two threads |
| Recovery (rollback, backup, restore) | ✅ | T09 rollback test; `scripts/backup.ps1`, `scripts/restore.ps1` |
| Parallel databases | 📝 | [advanced_topics.md §1](advanced_topics.md#1-parallel-databases) |
| Distributed databases | 📝 | [advanced_topics.md §2](advanced_topics.md#2-distributed-databases) |
| Elasticsearch index | 📝 | explained, not implemented (optional) |
| Cloud databases / DBaaS | 📝 | [advanced_topics.md §7](advanced_topics.md#7-cloud-databases-and-dbaas) |
| NoSQL types, BASE, CAP | 📝 | [advanced_topics.md §4](advanced_topics.md#4-nosql) |
| Big Data, Hadoop, HDFS, MapReduce | 📝 | [advanced_topics.md §5](advanced_topics.md#5-big-data-and-hadoop) |
| Data warehouse, star vs snowflake, OLAP vs OLTP | 📝✅ | [advanced_topics.md §6](advanced_topics.md#6-data-warehouse-oltp-and-olap); `WITH ROLLUP` query runs on the real data |

No DBMS syllabus item is uncovered. Topics in Unit 5–6 are explained, not
implemented, exactly as the brief requires ("do not claim … unless a working
demonstration is actually built").
