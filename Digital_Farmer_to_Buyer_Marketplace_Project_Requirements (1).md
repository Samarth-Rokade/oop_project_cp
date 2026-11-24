# Digital Farmer-to-Buyer Marketplace

## Integrated Course Project Requirements for OOP with Java and Database Management Systems (DBMS)

**Project type:** Integrated course project\
**Primary application:** Java desktop application\
**GUI:** Java Swing\
**Database:** MySQL\
**Database connectivity:** JDBC\
**Proposed tagline:** Connecting fresh supply with trusted demand.

------------------------------------------------------------------------

## 1. Project Overview

The Digital Farmer-to-Buyer Marketplace is a database-driven application
that enables farmers and Farmer Producer Organizations (FPOs) to connect
directly with buyers such as retailers, wholesalers, restaurants, and
individual customers.

Farmers can register, publish agricultural products, update available
quantities, set prices, and manage incoming orders. Buyers can discover
products, compare prices and availability, place orders, and view order
and delivery progress. An administrator manages users, verifies farmer
accounts, monitors listings, and oversees marketplace activity.

The application will be implemented in Java using object-oriented design
and Java Swing for the graphical user interface. MySQL will store
persistent application data, and JDBC will connect the Java application
to the database.

This is one integrated project for two courses: - **OOP with Java:** The
application will demonstrate classes, objects, encapsulation,
inheritance, polymorphism, abstraction, interfaces, collections,
exception handling, multithreading, file handling, GUI programming, and
Java 8 features. - **DBMS:** The database design and implementation will
demonstrate ER modelling, relational design, normalization, functional
dependencies, SQL, relational algebra, stored routines, triggers,
indexing, query processing concepts, transactions, concurrency control,
recovery, and selected analytical database concepts.

The project should remain achievable as a student course project.
Advanced DBMS topics that are not practical to implement fully in a
small desktop application will be demonstrated through documented
schemas, SQL experiments, execution plans, simulations, or a clearly
separated extension.

------------------------------------------------------------------------

## 2. Problem Statement

Agricultural producers often depend on multiple intermediaries to reach
buyers. This can reduce the farmer's share of the final selling price
and make it difficult for farmers to understand current demand, compare
prices, and find reliable buyers.

Buyers may also face difficulties finding verified farmers, checking the
availability and quality details of produce, comparing prices, and
tracking orders. Information about products, prices, stock, and delivery
is frequently fragmented across calls, messages, and informal records.

There is a need for a centralized digital platform that makes
agricultural supply visible, improves direct communication between
farmers and buyers, and maintains reliable records of products, prices,
orders, payments, and deliveries.

**Problem statement:** Design and develop a secure, database-driven
marketplace that connects farmers directly with buyers, supports
agricultural product discovery and order management, maintains accurate
inventory and transaction records, and provides transparent information
about prices, availability, and order progress.

------------------------------------------------------------------------

## 3. Proposed Solution

Develop a Java-based desktop marketplace with three principal user
roles:

1.  **Farmer / Seller:** Registers and manages a farmer profile, adds
    agricultural products, updates stock and prices, and processes
    orders.
2.  **Buyer:** Searches and filters available products, views product
    details, places orders, and tracks order progress.
3.  **Administrator:** Verifies farmer accounts, manages users and
    product categories, monitors listings and orders, and views reports.

The system will use a centralized MySQL database as the source of truth.
All important operations---such as creating an order, reducing stock,
recording a payment, and changing order status---will be performed
through validated application services and database transactions.

### 3.1 Main objectives

-   Reduce unnecessary dependence on intermediaries by enabling direct
    farmer-to-buyer discovery and ordering.
-   Provide a common place to publish agricultural products, prices,
    quantities, and availability.
-   Make product and farmer information easier to discover.
-   Maintain accurate stock when orders are placed, cancelled, or
    fulfilled.
-   Provide a traceable order lifecycle from placement to delivery.
-   Maintain transaction records for orders and payments.
-   Generate useful farmer-wise, product-wise, and sales reports.
-   Apply sound object-oriented design and relational database
    principles.

### 3.2 Expected outcomes

-   Farmers can publish and manage available produce.
-   Buyers can discover products and place orders.
-   The system can prevent orders from exceeding available stock.
-   Users can view order and payment status.
-   Administrators can monitor marketplace activity.
-   Reports can summarize sales, orders, inventory, and farmer
    performance.

------------------------------------------------------------------------

## 4. Scope and Project Boundaries

### 4.1 Features included in the core implementation

-   Registration and login for farmers, buyers, and administrators.
-   Role-based access to application features.
-   Farmer profile and verification status.
-   Product category management.
-   Product listing, editing, activation/deactivation, and deletion
    where permitted.
-   Product quantity, unit, price, harvest date, and description
    management.
-   Product search and filtering.
-   Cart or order creation.
-   Order item details and order total calculation.
-   Inventory validation and stock updates.
-   Order status management.
-   Payment record management using simulated payment statuses.
-   Delivery details and status tracking.
-   Price history.
-   Farmer-wise and product-wise reports.
-   Input validation, exception handling, and basic audit information.
-   Database backup and restore demonstration.

### 4.2 Optional enhancements

Implement these only after the core system is complete:

-   Product reviews and ratings.
-   Wishlist.
-   Notifications within the application.
-   CSV report export.
-   Product quality/grade information.
-   Demand summaries and basic sales trends.
-   Multiple delivery partners.
-   Multilingual interface.
-   A mobile or web client using the same database-backed service layer.

### 4.3 Explicit limitations

-   The first version is a course-project prototype, not a production
    e-commerce or logistics platform.
-   Real payment gateway integration is not required. Payments can be
    represented using simulated methods and statuses.
-   Real-time GPS tracking is not required. Delivery progress can be
    updated through status records.
-   The application does not guarantee market prices or agricultural
    quality.
-   AI price prediction, crop disease detection, and live logistics
    integrations are future enhancements, not core requirements.
-   Distributed DBMS, Hadoop, and NoSQL concepts should not be claimed
    as implemented unless a working demonstration is actually built.

------------------------------------------------------------------------

## 5. Users and Functional Requirements

### 5.1 Farmer / Seller

The farmer must be able to:

-   Register and log in.
-   Maintain name, contact information, address, and farm details.
-   View account verification status.
-   Add a product with name, category, description, unit, available
    quantity, price, harvest date, and optional image path.
-   Edit product details and update available quantity.
-   Change a listing's availability.
-   View all personal product listings.
-   View received orders containing their products.
-   Accept or reject an order, subject to the defined order workflow.
-   Update the seller-side fulfilment status.
-   View sales history and product-wise performance.
-   View price history for their own listings.

### 5.2 Buyer

The buyer must be able to:

-   Register and log in.
-   Maintain name, contact details, and delivery addresses.
-   Browse active products.
-   Search by product name and filter by category, farmer, price range,
    and availability.
-   View product details, seller information, price, unit, quantity, and
    harvest information.
-   Add products to a cart or create an order.
-   Specify quantity and delivery address.
-   Review order items, quantities, and calculated total before
    confirming.
-   View previous and current orders.
-   View payment and delivery status.
-   Cancel an order only when its status permits cancellation.
-   View the details of each order.

### 5.3 Administrator

The administrator must be able to:

-   Log in using an administrator account.
-   View dashboard summaries.
-   View, activate, suspend, or verify user accounts.
-   Review farmer verification details.
-   Manage product categories and units.
-   View and moderate product listings.
-   View all orders and their status.
-   View payment records.
-   View sales and inventory reports.
-   Review important activity records.

### 5.4 System-level functional requirements

-   A user must not access another user's private data without
    authorization.
-   A farmer may modify only their own listings.
-   A buyer may view and manage only their own orders.
-   An order must contain at least one valid order item.
-   The system must validate quantity and available stock before order
    confirmation.
-   An order's total must be calculated by the application from stored
    item prices and quantities; it must not be trusted from an arbitrary
    user-entered total.
-   Stock reduction and order creation must succeed or fail together.
-   Rejected or cancelled orders must follow a defined stock-restoration
    rule.
-   Important status changes must be recorded with timestamps.
-   Inactive products must not be available for new orders.
-   Passwords must not be stored as plain text. Store a securely hashed
    password using an appropriate password-hashing library.

------------------------------------------------------------------------

## 6. Non-Functional Requirements

### 6.1 Usability

-   Provide a simple, consistent Swing interface.
-   Use clear navigation based on the logged-in user's role.
-   Display understandable validation and error messages.
-   Use tables for product, order, and report listings.

### 6.2 Reliability

-   Use database transactions for multi-step operations.
-   Prevent negative stock and invalid order quantities.
-   Handle database connection failures and invalid input gracefully.
-   Preserve important transaction records.

### 6.3 Security

-   Authenticate users before allowing protected operations.
-   Enforce role-based authorization in the service layer, not only by
    hiding GUI buttons.
-   Use prepared statements to reduce SQL injection risk.
-   Hash passwords; never store plain-text passwords.
-   Validate all user input.
-   Avoid exposing database credentials in source control.

### 6.4 Maintainability

-   Separate GUI, business logic, and database access.
-   Use meaningful class names and methods.
-   Avoid putting SQL queries directly inside Swing event handlers.
-   Keep database connection settings in a configuration file excluded
    from public source control.

### 6.5 Performance

-   Use indexes for frequently searched columns.
-   Retrieve only the data needed by each screen.
-   Use pagination or limited result sets if product or order records
    grow.
-   Avoid unnecessary repeated database queries.

------------------------------------------------------------------------

## 7. Application Workflow

### 7.1 Farmer workflow

1.  Farmer registers.
2.  Administrator verifies the farmer account.
3.  Farmer logs in.
4.  Farmer creates a product listing.
5.  The listing becomes visible to buyers when active and approved.
6.  Farmer receives an order containing the listed product.
7.  Farmer accepts or rejects the order.
8.  Farmer updates fulfilment status as the order progresses.
9.  Farmer views completed sales and reports.

### 7.2 Buyer workflow

1.  Buyer registers and logs in.
2.  Buyer searches and filters agricultural products.
3.  Buyer views product and farmer details.
4.  Buyer selects a quantity.
5.  System checks current stock and calculates the order total.
6.  Buyer confirms the order.
7.  System creates the order, order items, and initial payment record in
    a transaction.
8.  Buyer views order progress.
9.  Buyer views completed or cancelled orders in order history.

### 7.3 Suggested order lifecycle

Use a controlled set of statuses:

`PLACED → CONFIRMED → PACKED → DISPATCHED → DELIVERED`

Other possible transitions:

-   `PLACED → REJECTED`
-   `PLACED → CANCELLED`
-   `CONFIRMED → CANCELLED` only if the business rule allows it before
    dispatch.
-   `DISPATCHED → DELIVERY_FAILED` if this optional state is
    implemented.

Do not allow arbitrary status changes. Implement a transition-validation
method in the service layer.

### 7.4 Suggested payment lifecycle

`PENDING → PAID`

Other possible statuses: `FAILED`, `REFUNDED`, `CANCELLED`.

For the course prototype, payment can be simulated. Clearly label it as
a demonstration and do not imply that real money is transferred.

------------------------------------------------------------------------

## 8. Proposed System Architecture

Use a layered architecture so the Java OOP design and database design
are clearly visible.

``` text
+----------------------------------------------------+
|                  PRESENTATION LAYER                |
|       Java Swing Frames, Panels, Dialogs           |
| Login | Farmer Dashboard | Buyer Dashboard | Admin |
+---------------------------+------------------------+
                            |
                            v
+----------------------------------------------------+
|                  SERVICE / BUSINESS LAYER          |
| AuthenticationService                              |
| ProductService | OrderService | InventoryService   |
| PaymentService | ReportService | UserService       |
| Validation | Authorization | Business Rules        |
+---------------------------+------------------------+
                            |
                            v
+----------------------------------------------------+
|                  DATA ACCESS LAYER                 |
| UserDAO | ProductDAO | OrderDAO | PaymentDAO       |
| CategoryDAO | DeliveryDAO | ReportDAO              |
| JDBC PreparedStatements and ResultSets             |
+---------------------------+------------------------+
                            |
                            v
+----------------------------------------------------+
|                     MYSQL DBMS                     |
| Users | Farmers | Buyers | Products | Orders       |
| OrderItems | Payments | Deliveries | PriceHistory  |
| Categories | Addresses | AuditLogs                |
+----------------------------------------------------+
```

### Architecture rules

-   Swing classes handle presentation and user interaction.
-   Service classes implement business rules and coordinate operations.
-   DAO classes contain SQL and JDBC operations.
-   Model classes represent application data.
-   Do not mix GUI code, business rules, and SQL in one large class.
-   Use database constraints as a second line of protection in addition
    to Java validation.

------------------------------------------------------------------------

## 9. Java OOP Design

The following class design is a starting point. The team may refine it
while preserving the responsibilities and OOP concepts.

### 9.1 Core classes

  Class            Main responsibility
  ---------------- -----------------------------------------------------
  `User`           Common user data and shared account behaviour
  `Farmer`         Farmer-specific profile and seller operations
  `Buyer`          Buyer-specific profile and buyer operations
  `Admin`          Administrative operations
  `Product`        Product details, price, unit, and stock information
  `Category`       Product category information
  `Address`        Buyer or farmer address information
  `Order`          Order header, buyer, date, status, and total
  `OrderItem`      Product, quantity, and unit price within an order
  `Payment`        Payment method, amount, status, and reference
  `Delivery`       Delivery details and current status
  `PriceHistory`   Historical product price records
  `Cart`           Temporary collection of selected products
  `CartItem`       Product and selected quantity in the cart

### 9.2 Inheritance

Use a common abstract `User` class with subclasses `Farmer`, `Buyer`,
and `Admin`.

``` java
public abstract class User {
    private int userId;
    private String name;
    private String email;
    private String passwordHash;

    public User(int userId, String name, String email,
                String passwordHash) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public abstract String getRole();

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }
}
```

``` java
public class Farmer extends User {
    private String farmName;
    private boolean verified;

    public Farmer(int id, String name, String email,
                  String passwordHash, String farmName) {
        super(id, name, email, passwordHash);
        this.farmName = farmName;
    }

    @Override
    public String getRole() {
        return "FARMER";
    }
}
```

Create corresponding `Buyer` and `Admin` subclasses. Keep constructors
and fields appropriate to each role.

### 9.3 Encapsulation

-   Declare model fields `private`.
-   Expose controlled getters and setters.
-   Validate values before changing important state.
-   Avoid public fields.
-   For example, product stock should not be changed to a negative
    value.

### 9.4 Polymorphism

Demonstrate runtime polymorphism using the `User` reference:

``` java
User user = new Farmer(...);
// or
User user = new Buyer(...);

System.out.println(user.getRole());
```

A service or dashboard factory can select role-specific behaviour using
an overridden method or a well-defined role strategy. Do not use
inheritance only to satisfy a checklist; it should represent genuine
shared and specialized behaviour.

### 9.5 Abstraction and interfaces

Create interfaces for service or repository contracts, for example:

``` java
public interface ProductOperations {
    boolean addProduct(Product product);
    boolean updateProduct(Product product);
    boolean deactivateProduct(int productId);
}
```

``` java
public interface OrderOperations {
    Order createOrder(int buyerId, List<CartItem> items);
    boolean updateOrderStatus(int orderId, String newStatus);
}
```

Use abstract classes where shared state or common implementation is
required, and interfaces where a contract is required. Explain the
difference during viva.

### 9.6 Method overloading and overriding

Examples: - Overload `searchProducts()` to search by name, category, or
a combination of filters. - Overload constructors for appropriate model
classes. - Override `getRole()` in each `User` subclass. - Override
`toString()` where it helps display meaningful object information.

Avoid artificial overloads that do not improve the design.

### 9.7 Static and final

-   Use `static final` constants for application-wide values such as
    supported order status labels.
-   Use `final` for immutable identifiers or references where
    appropriate.
-   Demonstrate a static utility method for input validation or
    formatting.
-   Explain why static methods belong to the class and are not
    dynamically overridden.

### 9.8 Object passing and returning

-   Pass `Product`, `User`, `Order`, and collection objects between
    service and DAO layers.
-   Return model objects or lists of model objects from DAO methods.
-   Avoid returning raw database rows to the GUI.

### 9.9 Collections

Use suitable Java collections: - `ArrayList<Product>` for product
results. - `HashMap<Integer, Product>` for product lookup by ID where
useful. - `HashSet<Integer>` for unique IDs or selected product IDs. -
`Map<String, Object>` only for genuinely flexible data; prefer typed
model classes for normal application data. - `Iterator` or enhanced
`for` loops to traverse collections.

Explain the difference between `List`, `Set`, and `Map`, and demonstrate
`ArrayList`, `Vector` or another relevant collection if required by the
course assessment.

### 9.10 Exception handling

Create and handle exceptions such as: - `InvalidQuantityException` -
`InsufficientStockException` - `InvalidOrderStatusException` -
`UserNotFoundException`

Use `try-catch-finally` or try-with-resources for JDBC resources.
Explain checked and unchecked exceptions, `throw`, and `throws`. Do not
silently catch exceptions or show raw stack traces to end users.

### 9.11 Multithreading

Use a background worker for operations that may take time, such as
loading large product lists or generating reports. In Swing, use
`SwingWorker` so database work does not freeze the Event Dispatch
Thread.

Demonstrate: - Thread life cycle and basic thread concepts. - A
background task for report generation or data loading. - Safe updating
of Swing components on the Event Dispatch Thread. - Appropriate handling
of shared state.

Do not create multiple threads unnecessarily for simple operations.

### 9.12 File handling

Use file processing for: - Exporting a sales report to CSV. - Importing
a small sample product dataset from CSV, if included. - Saving an
application report or receipt. - Demonstrating primitive and object data
processing in a separate, controlled example if the syllabus requires
it.

The database remains the primary persistent store for application
records. Do not use serialized Java objects as a replacement for the
relational database.

### 9.13 Java GUI

Build the desktop interface using Swing: - `JFrame` for main windows. -
`JPanel` for sections and dashboards. - `JLabel`, `JTextField`,
`JPasswordField`, `JButton`. - `JTable` for products, orders, and
reports. - `JComboBox` for categories, units, and statuses. - `JList`
where a list selection is appropriate. - `JOptionPane` for confirmation
and validation messages. - Layout managers such as `FlowLayout`,
`BorderLayout`, `GridLayout`, and `CardLayout`. - Event listeners for
mouse and keyboard actions.

Keep event handlers short. They should call service methods rather than
contain complete business logic.

### 9.14 Java 8 features

Use and demonstrate: - Lambda expressions for event listeners and
collection operations. - Functional interfaces such as
`Predicate<Product>` for in-memory filtering. - Stream API for
filtering, sorting, and summarizing in-memory collections. - `forEach()`
for collection traversal. - `Optional` where a return value may
legitimately be absent. - `StringJoiner` for formatted report text. -
Default and static methods in a small interface demonstration, if
relevant. - Parallel array sorting only in a separate, meaningful
demonstration if the course requires it; do not parallelize database
operations simply to tick a box.

------------------------------------------------------------------------

## 10. DBMS Design Requirements

### 10.1 ER model: proposed entities

The ER diagram should include at least these entities:

1.  `User`
2.  `FarmerProfile`
3.  `BuyerProfile`
4.  `Address`
5.  `Category`
6.  `Product`
7.  `Orders`
8.  `OrderItem`
9.  `Payment`
10. `Delivery`
11. `PriceHistory`
12. `AuditLog`

Possible additional entities: - `ProductImage` - `Review` -
`Notification` - `Unit`

### 10.2 Main relationships and cardinalities

-   One user has one role-specific profile, where applicable.
-   One farmer can list many products.
-   One category can contain many products.
-   One buyer can place many orders.
-   One order contains one or more order items.
-   One product can appear in many order items.
-   One order can have one or more payment records if payment attempts
    or refunds are recorded.
-   One order can have delivery information according to the selected
    delivery model.
-   One product can have many price history records.
-   One user can generate many audit log entries.

The ER diagram must clearly show primary keys, foreign keys,
relationship cardinalities, and participation constraints where
applicable. Do not model a many-to-many relationship between orders and
products without an associative entity; `OrderItem` resolves that
relationship.

### 10.3 Suggested relational schema

The following is a logical starting schema. Final column types,
nullability, unique constraints, and delete/update rules must be decided
and documented by the team.

**Users** - `user_id` --- primary key - `full_name` - `email` ---
unique - `phone` - `password_hash` - `role` --- FARMER, BUYER, ADMIN -
`account_status` - `created_at`

**FarmerProfile** - `farmer_id` --- primary key and foreign key to
Users - `farm_name` - `farm_address` - `verification_status`

**BuyerProfile** - `buyer_id` --- primary key and foreign key to Users -
`buyer_type`

**Address** - `address_id` --- primary key - `user_id` --- foreign key
to Users - `address_line` - `city` - `district` - `state` -
`postal_code` - `is_default`

**Category** - `category_id` --- primary key - `category_name` ---
unique - `description`

**Product** - `product_id` --- primary key - `farmer_id` --- foreign key
to FarmerProfile - `category_id` --- foreign key to Category -
`product_name` - `description` - `unit` - `available_quantity` -
`price_per_unit` - `harvest_date` - `listing_status` - `created_at` -
`updated_at`

**Orders** - `order_id` --- primary key - `buyer_id` --- foreign key to
BuyerProfile - `delivery_address_id` --- foreign key to Address -
`order_date` - `order_status` - `total_amount`

**OrderItem** - `order_item_id` --- primary key - `order_id` --- foreign
key to Orders - `product_id` --- foreign key to Product - `quantity` -
`unit_price_at_order` - `line_total`

**Payment** - `payment_id` --- primary key - `order_id` --- foreign key
to Orders - `payment_method` - `amount` - `payment_status` -
`payment_date` - `transaction_reference`

**Delivery** - `delivery_id` --- primary key - `order_id` --- foreign
key to Orders - `delivery_status` - `dispatch_date` - `delivery_date` -
`delivery_partner` - `tracking_reference`

**PriceHistory** - `price_history_id` --- primary key - `product_id` ---
foreign key to Product - `old_price` - `new_price` - `changed_at` -
`changed_by` --- foreign key to Users

**AuditLog** - `log_id` --- primary key - `user_id` --- foreign key to
Users, nullable for system events if justified - `action` -
`entity_type` - `entity_id` - `action_time` - `details`

### 10.4 Important data integrity rules

-   Email must be unique.
-   Product quantity must be zero or greater.
-   Product price must be greater than zero.
-   Order item quantity must be greater than zero.
-   Order item unit price must be non-negative.
-   Order total must equal the sum of its order item line totals,
    subject to the chosen tax/discount policy.
-   Foreign keys must prevent orphan records.
-   Product listing status and order status must use controlled values.
-   A buyer must not order an inactive product.
-   A farmer must not edit another farmer's product.
-   A product's historical order price must be stored in `OrderItem`; it
    must not change when the current product price changes.

Use primary keys, foreign keys, `NOT NULL`, `UNIQUE`, `CHECK`
constraints where supported, and appropriate referential actions.
Explain any intentional use of soft deletion.

------------------------------------------------------------------------

## 11. Normalization and Functional Dependencies

Normalization must be shown using actual project data, not only
definitions.

### 11.1 Example unnormalized design

A poorly designed order table might store:

`OrderID, BuyerName, BuyerPhone, Product1, Qty1, Product2, Qty2, FarmerName, FarmerPhone`

This design creates repeating groups, duplicate data, and update
anomalies.

### 11.2 Functional dependency examples

Examples to analyse and validate against the final schema:

-   `user_id → full_name, email, phone, password_hash, role`
-   `category_id → category_name, description`
-   `product_id → farmer_id, category_id, product_name, unit, available_quantity, price_per_unit`
-   `order_id → buyer_id, delivery_address_id, order_date, order_status`
-   `order_item_id → order_id, product_id, quantity, unit_price_at_order, line_total`

If the design uses a composite key in `OrderItem`, analyse the
corresponding dependency carefully. A surrogate primary key does not
remove the need to document candidate keys and business dependencies.

### 11.3 Required normalization work

-   Show how repeating groups are removed to reach 1NF.
-   Identify partial dependencies and demonstrate 2NF.
-   Identify transitive dependencies and demonstrate 3NF.
-   Analyse whether any relation violates BCNF.
-   Explain decomposition and whether it is lossless.
-   Explain dependency preservation.
-   Demonstrate a functional dependency closure calculation.
-   Show how to derive a minimal cover for a small, clearly stated set
    of dependencies.
-   Explain multivalued dependencies and 4NF using a relevant example,
    such as independent farmer certifications and languages, if such
    data is included.

Do not claim that the complete database is in BCNF or 4NF without
showing the analysis.

------------------------------------------------------------------------

## 12. SQL Requirements and Query Demonstrations

The project report and demonstration should include all relevant SQL
categories from the syllabus.

### 12.1 DDL --- Data Definition Language

Demonstrate: - `CREATE DATABASE` - `CREATE TABLE` - Primary key and
foreign key constraints - `NOT NULL`, `UNIQUE`, `DEFAULT`, and `CHECK`
constraints where supported - `ALTER TABLE` - `CREATE INDEX` -
`CREATE VIEW` - `DROP` or `TRUNCATE` in a safe test environment

### 12.2 DML --- Data Manipulation Language

Demonstrate: - `INSERT` - `UPDATE` - `DELETE` - `SELECT`

Include realistic sample records for farmers, buyers, categories,
products, orders, and payments.

### 12.3 SELECT queries

Include at least: 1. Display all active products. 2. Search products by
name. 3. Display products within a price range. 4. Display products
belonging to a selected category. 5. Display products listed by a
particular farmer. 6. Display all orders placed by a buyer. 7. Display
order items for a selected order. 8. Display products with low stock. 9.
Display farmers with their total completed sales. 10. Display the most
frequently ordered products.

### 12.4 Set operations

Demonstrate `UNION`, `UNION ALL`, and supported set-equivalent queries.
Where the DBMS does not directly support `INTERSECT` or
`MINUS`/`EXCEPT`, explain and demonstrate equivalent SQL using joins or
subqueries.

### 12.5 String, date, and numerical functions

Include examples such as: - String: `UPPER`, `LOWER`, `CONCAT`,
`LENGTH`, `SUBSTRING`. - Date/time: `CURRENT_DATE`, `CURRENT_TIMESTAMP`,
date extraction or date difference functions supported by the selected
MySQL version. - Numerical: `ROUND`, `ABS`, and other relevant
functions.

### 12.6 Aggregate functions, GROUP BY, and HAVING

Demonstrate: - `COUNT()` --- number of orders. - `SUM()` --- total
sales. - `AVG()` --- average product price. - `MIN()` and `MAX()` ---
lowest and highest price. - `GROUP BY` --- sales grouped by farmer or
category. - `HAVING` --- farmers or products meeting an aggregate
threshold.

### 12.7 Join queries

Demonstrate: - Inner join: products with farmer details. - Left join:
all farmers, including those with no products. - Multi-table join:
order, buyer, order items, and product. - Self join only if a meaningful
relationship exists. - Explain join conditions and the difference
between filtering in `ON` and `WHERE` where relevant.

### 12.8 Nested queries

Examples: - Products priced above the average price in their category. -
Farmers with sales greater than the overall average farmer sales. -
Buyers who have placed at least one order. - Products that have never
been ordered, using `NOT EXISTS`.

### 12.9 DCL --- Data Control Language

Demonstrate database users and permissions using `GRANT` and `REVOKE`
where the lab environment permits. Use least-privilege permissions for
the application account.

### 12.10 TCL --- Transaction Control Language

Demonstrate: - `START TRANSACTION` or `BEGIN` - `COMMIT` - `ROLLBACK` -
`SAVEPOINT` - `ROLLBACK TO SAVEPOINT`

Use an order placement example to show why related changes must be
atomic.

### 12.11 Views

Create views such as: - `ActiveProductView` - `FarmerSalesSummary` -
`OrderSummaryView`

Do not expose password hashes or unnecessary private user details in
reporting views.

### 12.12 Stored procedures, functions, and triggers

Implement a small, meaningful set of database routines: - Procedure to
retrieve a buyer's order history. - Procedure or service-supported
transaction to create an order. - Function to calculate a total or
classify a stock level, if appropriate. - Trigger to record product
price changes in `PriceHistory`. - Trigger or constraint-backed logic to
reject invalid stock values, where appropriate.

Avoid duplicating complex business rules in both triggers and Java
without a clear reason. Document which layer owns each rule.

### 12.13 Relational algebra mapping

For selected application queries, write the equivalent relational
algebra expressions using: - Selection (σ) - Projection (π) - Union
(∪) - Set difference (−) - Cartesian product (×) - Rename (ρ) - Join
(⋈) - Division (÷), if a meaningful query can be constructed.

Include a mapping table from each selected relational algebra expression
to its SQL query. Use a meaningful division example only if the schema
and question support it.

------------------------------------------------------------------------

## 13. Indexing, Storage, and Query Optimization

### 13.1 Indexes to consider

Create and evaluate indexes on: - `Users.email` --- unique index. -
`Product.category_id`. - `Product.farmer_id`. -
`Product.listing_status`. - `Orders.buyer_id`. - `Orders.order_date`. -
`OrderItem.order_id`. - `OrderItem.product_id`.

A composite index may be useful for common product filters, but choose
column order based on actual query patterns.

### 13.2 Required experiments

-   Run a representative search query before and after adding an index.
-   Use MySQL `EXPLAIN` to inspect the query plan.
-   Record whether an index is used.
-   Compare the plan and execution time on the same test dataset.
-   Explain why an index can speed up reads but add storage and write
    overhead.
-   Explain clustered/primary-key and secondary-index concepts as they
    apply to the chosen DBMS.

Do not claim a performance improvement based on a tiny dataset without
qualification.

### 13.3 File and storage concepts

Document how the selected DBMS stores tables and indexes at a conceptual
level. Discuss file organization, indexed files, single-level and
multi-level indexing, and query-processing stages in the DBMS report.
These are DBMS concepts; they do not require implementing a storage
engine.

### 13.4 Parquet

Parquet is a columnar file format, not a replacement for the
transactional MySQL database. If included, export a sample sales dataset
to Parquet using a suitable external tool/library and explain when
columnar storage is useful for analytics. This is an optional extension,
not a core dependency.

------------------------------------------------------------------------

## 14. Transaction Management and Concurrency

This section is essential because inventory and order placement can
produce inconsistent data if implemented incorrectly.

### 14.1 ACID properties in this project

-   **Atomicity:** Creating an order, creating its items, and reducing
    stock must succeed together or roll back together.
-   **Consistency:** Constraints and business rules must remain valid
    before and after a transaction.
-   **Isolation:** Concurrent orders must not both consume the same
    available stock.
-   **Durability:** A committed order must remain stored after a normal
    application restart.

### 14.2 Example order transaction

1.  Begin a transaction.
2.  Read and lock the relevant product stock rows, or use an appropriate
    conditional update.
3.  Verify that every requested quantity is available.
4.  Create the order header.
5.  Create all order item records with the current unit price captured
    at purchase time.
6.  Reduce stock for each product.
7.  Create the initial payment record.
8.  Commit the transaction.
9.  If any step fails, roll back all changes.

Use JDBC transaction control (`setAutoCommit(false)`, `commit()`, and
`rollback()`). Use row-level locking or safe conditional updates
according to the chosen MySQL storage engine and isolation level.

### 14.3 Schedules and serializability

Prepare a small example with two buyers attempting to purchase the last
available units of the same product.

-   Draw a schedule for the two transactions.
-   Identify conflicting read/write or write/write operations.
-   Construct a precedence graph.
-   Explain conflict serializability and view serializability at the
    syllabus level.
-   Demonstrate how the chosen locking/isolation approach prevents
    overselling.

### 14.4 Concurrency control

Study and document: - Shared and exclusive locks. - Two-phase locking. -
Deadlock and how the DBMS may detect or resolve it. - Isolation
levels. - Dirty reads, non-repeatable reads, and phantom reads. -
Optimistic or pessimistic concurrency control.

Implement a small concurrency test if time permits. Do not claim that
the GUI's single-user demonstration proves concurrency correctness.

### 14.5 Recovery

-   Explain transaction logs and checkpoints conceptually.
-   Demonstrate a rollback after a deliberately failed order operation.
-   Take a database backup and restore it in a test environment.
-   Document how committed and uncommitted work should behave after a
    failure.
-   Never test recovery on the only copy of the project database.

------------------------------------------------------------------------

## 15. Parallel, Distributed, NoSQL, and Big Data Topics

The core application should use a relational MySQL database. The
following syllabus topics should be covered honestly through a
documented study or small optional demonstration.

### 15.1 Parallel database concepts

Explain: - Parallel database architecture. - I/O parallelism. -
Inter-query parallelism. - Intra-query parallelism. - Intra-operation
and inter-operation parallelism.

Relate these concepts to possible large-scale marketplace analytics, but
do not claim the desktop application implements parallel DBMS execution.

### 15.2 Distributed database concepts

Explain: - Types of distributed database systems. - Distributed data
storage. - Distributed query processing. - Replication and fragmentation
at a conceptual level. - Benefits and challenges of distributed data.

A possible future architecture could separate regional marketplace data
while providing a central analytics layer. This is a design discussion
unless a real distributed setup is built.

### 15.3 Elastic search index

An optional search extension may index product name, description,
category, and location in Elasticsearch or another search engine. The
MySQL database remains the authoritative transactional store. This
should be implemented only if time and infrastructure allow.

### 15.4 NoSQL databases

Explain the major NoSQL categories: - Key-value. - Document. -
Column-family. - Graph.

Discuss BASE properties and the CAP theorem. If a NoSQL extension is
built, use a separate, clearly justified dataset such as notification
documents or search metadata. Do not duplicate all transactional records
without a reason.

### 15.5 Big Data and Hadoop

Explain: - Big Data characteristics and use cases. - Hadoop
architecture. - HDFS. - MapReduce.

A possible future use case is analysing large volumes of historical crop
prices and regional demand. A Hadoop cluster is not required for the
core course project.

### 15.6 Data warehouse, OLAP, and OLTP

The operational marketplace is an **OLTP** system: it handles user
accounts, product updates, orders, payments, and stock transactions.

For the DBMS report, propose a small analytical warehouse: - Fact table:
`FactSales`. - Dimensions: `DimDate`, `DimFarmer`, `DimProduct`,
`DimBuyer`, and optionally `DimRegion`. - Explain a star schema and
compare it with a snowflake schema. - Demonstrate sample OLAP-style
queries such as sales by month, category, farmer, and region.

The warehouse can be a documented design or a small separate schema. Do
not mix analytical aggregates into transactional tables without
explaining the design.

### 15.7 Cloud databases and DBaaS

Explain database-as-a-service, managed database hosting, scalability,
availability, backup, security, and cost considerations. Cloud
deployment is optional and should not be required for the core local
demonstration.

------------------------------------------------------------------------

## 16. Database Security and Data Governance

-   Use a dedicated database account for the application.
-   Grant only the permissions the application needs.
-   Keep administrator database credentials separate from application
    credentials.
-   Use prepared statements for values supplied by users.
-   Validate role and ownership before updates or deletions.
-   Hash passwords using a suitable password-hashing algorithm and
    library.
-   Avoid storing unnecessary personal information.
-   Protect exported reports and backup files.
-   Keep a basic audit trail for important actions.
-   Use sample or synthetic data in the project demonstration.

------------------------------------------------------------------------

## 17. Suggested Java Project Folder Structure

``` text
DigitalFarmerMarketplace/
|
|-- src/
|   |-- main/
|       |-- java/
|       |   |-- app/
|       |   |   `-- Main.java
|       |   |
|       |   |-- config/
|       |   |   `-- DatabaseConfig.java
|       |   |
|       |   |-- model/
|       |   |   |-- User.java
|       |   |   |-- Farmer.java
|       |   |   |-- Buyer.java
|       |   |   |-- Admin.java
|       |   |   |-- Product.java
|       |   |   |-- Category.java
|       |   |   |-- Order.java
|       |   |   |-- OrderItem.java
|       |   |   |-- Payment.java
|       |   |   `-- Delivery.java
|       |   |
|       |   |-- dao/
|       |   |   |-- UserDAO.java
|       |   |   |-- ProductDAO.java
|       |   |   |-- OrderDAO.java
|       |   |   `-- PaymentDAO.java
|       |   |
|       |   |-- service/
|       |   |   |-- AuthenticationService.java
|       |   |   |-- ProductService.java
|       |   |   |-- OrderService.java
|       |   |   |-- InventoryService.java
|       |   |   `-- ReportService.java
|       |   |
|       |   |-- exception/
|       |   |   |-- InsufficientStockException.java
|       |   |   `-- InvalidOrderStatusException.java
|       |   |
|       |   |-- interfaces/
|       |   |   |-- ProductOperations.java
|       |   |   `-- OrderOperations.java
|       |   |
|       |   `-- ui/
|       |       |-- LoginFrame.java
|       |       |-- FarmerDashboard.java
|       |       |-- BuyerDashboard.java
|       |       |-- AdminDashboard.java
|       |       |-- ProductPanel.java
|       |       |-- OrderPanel.java
|       |       `-- ReportPanel.java
|       |
|       `-- resources/
|           |-- db.properties.example
|           `-- images/
|
|-- database/
|   |-- 01_create_schema.sql
|   |-- 02_constraints_indexes.sql
|   |-- 03_sample_data.sql
|   |-- 04_views.sql
|   |-- 05_procedures_functions.sql
|   |-- 06_triggers.sql
|   `-- 07_demo_queries.sql
|
|-- reports/
|   |-- er_diagram.png
|   |-- relational_schema.md
|   |-- normalization.md
|   |-- query_optimization.md
|   `-- transaction_demo.md
|
|-- exports/
|-- README.md
`-- .gitignore
```

This is a suggested structure, not a requirement to create every file
immediately. Keep credentials and local exports out of public Git
commits.

------------------------------------------------------------------------

## 18. Testing Requirements

Prepare test cases with input, expected result, actual result, and
pass/fail status.

  -----------------------------------------------------------------------
  Test ID                 Scenario                Expected result
  ----------------------- ----------------------- -----------------------
  T01                     Register with a new     Account is created
                          valid email             

  T02                     Register with an        Duplicate account is
                          existing email          rejected

  T03                     Log in with invalid     Access is denied with a
                          credentials             clear message

  T04                     Farmer adds a valid     Product is saved and
                          product                 visible to eligible
                                                  buyers

  T05                     Farmer enters negative  Validation rejects the
                          stock                   value

  T06                     Buyer searches for an   Matching active
                          existing product        products are shown

  T07                     Buyer orders a quantity Order is created
                          within stock            successfully

  T08                     Buyer orders more than  Order is rejected;
                          available stock         stock remains unchanged

  T09                     Order creation fails    Transaction rolls back
                          midway                  all partial changes

  T10                     Farmer attempts to edit Operation is denied
                          another farmer's        
                          product                 

  T11                     Buyer attempts to view  Access is denied
                          another buyer's order   

  T12                     Cancel an eligible      Status changes and
                          order                   stock is restored
                                                  according to policy

  T13                     Change a product price  Price history is
                                                  recorded

  T14                     Generate a sales report Correct grouped totals
                                                  are displayed

  T15                     Run a report while the  GUI remains responsive
                          GUI is active           

  T16                     Back up and restore the Test data is restored
                          database                successfully
  -----------------------------------------------------------------------

Also test database constraints directly, not only through the GUI.

------------------------------------------------------------------------

## 19. Syllabus Coverage Matrix

The project should be accompanied by a report and demonstrations that
explicitly map course topics to implementation or evidence.

### 19.1 OOP with Java

  -----------------------------------------------------------------------
  Syllabus unit/topic                 Project implementation or
                                      demonstration
  ----------------------------------- -----------------------------------
  Unit 1: OOP need and                Explain why the marketplace is
  characteristics                     divided into objects and classes

  Classes and objects                 User, Farmer, Buyer, Product,
                                      Order, Payment

  Java object storage                 Explain object references and
                                      object lifecycle in the application

  Access modifiers                    Private fields, public methods,
                                      protected members where justified

  `this` reference                    Constructors and methods in model
                                      classes

  Main method                         Application entry point

  Static vs instance block/method     Constants, utility methods,
                                      instance initialization where
                                      appropriate

  Constructors                        Default/parameterized constructor
                                      examples where suitable

  Unit 2: Input/output streams        CSV import/export or file
                                      processing demonstration

  Scanner                             Input demonstration or a small
                                      console utility

  Arrays and multidimensional arrays  Suitable report/data-processing
                                      example

  `java.util.Arrays`                  Array sorting/searching
                                      demonstration

  String, StringBuffer, StringBuilder Search text, report generation,
                                      mutable string examples

  Methods and parameter passing       Service methods and object passing

  Returning multiple values           Return a result object or
                                      collection

  Unit 3: Inheritance                 `User` → `Farmer`, `Buyer`, `Admin`

  Constructor in inheritance          `super()` in subclass constructors

  `final` with inheritance            Demonstrate final
                                      class/method/field where justified

  Superclass members                  Access inherited user information

  Same data member in parent/child    Optional small demonstration of
                                      field hiding; explain why it is
                                      usually avoided

  Base vs derived reference           Use a `User` reference to refer to
                                      a `Farmer` or `Buyer`

  Method overloading                  Search and constructor overloads

  Overloading `main()`                Separate language demonstration if
                                      required

  Static vs dynamic binding           Static method hiding vs overridden
                                      instance methods

  Method hiding                       Small isolated demonstration

  Private and final methods           Explain inheritance/access
                                      restrictions

  Passing and returning objects       Service and DAO method
                                      parameters/results

  Unit 4: Interfaces                  ProductOperations and
                                      OrderOperations

  Abstract class                      Abstract User class

  Abstract class vs interface         Explain design choice

  Nested interface/class              Optional focused demonstration

  Inner class                         Optional listener/helper
                                      demonstration

  Anonymous class                     Event listener example where used

  Unit 5: Exceptions                  Validation and database exception
                                      handling

  Checked vs unchecked                JDBC exceptions and application
                                      validation exceptions

  Throw and throws                    Custom exceptions and service
                                      methods

  User-defined exceptions             InsufficientStockException

  Collections                         ArrayList, Vector, Map, Set

  Iterators vs foreach                Traverse product/order collections

  Multithreading                      SwingWorker for background
                                      report/data loading

  Thread life cycle/priority/methods  Explain and demonstrate a
                                      controlled background task

  Unit 6: File handling               CSV report export/import

  Primitive/object data processing    Focused file processing
                                      demonstration

  JDBC/ODBC                           JDBC connection to MySQL

  Swing and components                Frames, panels, labels, buttons,
                                      tables, lists, choices

  Layout managers                     Flow, Border, Grid, Card

  Event handling                      Mouse/key/button events

  Java 8 lambda expressions           Event listeners and collection
                                      processing

  Stream API                          Filter, sort, aggregate in-memory
                                      collections

  Functional interfaces               Predicate/Consumer examples

  Default/static interface methods    Small interface demonstration

  `forEach()`                         Collection traversal

  Optional                            Optional result from a lookup

  StringJoiner                        CSV/report text construction

  Parallel array sorting              Separate relevant demonstration if
                                      required
  -----------------------------------------------------------------------

**Important:** Some syllabus topics, such as method hiding, overloaded
`main()`, nested types, and parallel array sorting, are
language-specific concepts rather than natural marketplace features.
Demonstrate them in small, clearly labelled examples or viva exercises
instead of forcing them into the main business design.

### 19.2 DBMS

  -----------------------------------------------------------------------
  Syllabus unit/topic                 Project implementation or evidence
  ----------------------------------- -----------------------------------
  Unit 1: Need/evolution of DBMS      Explain why a centralized database
                                      is needed

  DBMS architecture                   Layered application and DBMS
                                      architecture explanation

  Database design process             Requirements → ER model →
                                      relational schema → normalization →
                                      SQL

  ER model                            Marketplace ER diagram

  Extended ER model                   User specialization into Farmer,
                                      Buyer, Admin

  Relational model                    Relational tables, keys,
                                      constraints

  Codd's rules                        Discuss relevant rules and evaluate
                                      the relational DBMS

  Unit 2: Need for normalization      Show anomalies in an unnormalized
                                      order table

  Functional dependencies             Dependencies for User, Product,
                                      Order, etc.

  Inference rules                     Armstrong's axioms examples

  Closure                             Attribute closure calculations

  Minimal cover                       Derive for a selected FD set

  Decomposition properties            Lossless join and dependency
                                      preservation

  1NF, 2NF, 3NF, BCNF                 Normalize selected relations step
                                      by step

  Multivalued dependency and 4NF      Separate independent multi-valued
                                      attributes in a sample relation

  Relational synthesis                Show a synthesis example from a
                                      minimal cover

  Unit 3: Relational algebra          Expressions for product search and
                                      order reports

  DDL and DML                         SQL schema and data scripts

  Select queries                      Product, buyer, farmer, and order
                                      queries

  Set operations                      UNION and equivalent set queries

  String/date/numeric functions       Report and search queries

  Aggregate functions                 Sales and inventory summaries

  GROUP BY/HAVING                     Farmer-wise and category-wise sales

  Joins                               Product-farmer and order-item
                                      queries

  Nested queries                      Average-price and sales comparisons

  DCL and TCL                         GRANT/REVOKE and transaction
                                      examples

  PL/SQL / database routines          MySQL procedures, functions, and
                                      triggers; note dialect difference

  Relational algebra to SQL           Query mapping table

  Unit 4: Storage/file structures     DBMS storage concepts in report

  Indexed files and index levels      Index design and explanation

  Query processing                    Parsing, optimization, execution
                                      concepts

  Query optimization                  EXPLAIN and index comparison

  Parquet                             Optional analytical export and
                                      explanation

  Transactions and ACID               Atomic order placement

  State diagram and schedules         Transaction state diagram and
                                      sample schedules

  Conflict/view serializability       Precedence graph and analysis

  Concurrency control                 Locks, isolation levels,
                                      overselling test

  Recovery                            Rollback, backup, restore
                                      demonstration

  Unit 5: Parallel DB                 Architecture and parallelism
                                      concepts in report

  Distributed DB                      Types, storage, query processing
                                      discussion

  Elastic search index                Optional search extension

  Cloud databases and DBaaS           Architecture and deployment
                                      discussion

  Unit 6: NoSQL                       Types and suitable use cases

  BASE and CAP                        Explain trade-offs using a
                                      marketplace scenario

  Big Data and Hadoop                 HDFS and MapReduce concepts/use
                                      case

  Data warehouse                      Proposed sales warehouse schema

  Warehouse schemas                   Star and snowflake comparison

  OLAP and OLTP                       Explain operational vs analytical
                                      workloads
  -----------------------------------------------------------------------

**DBMS dialect note:** The syllabus mentions PL/SQL, which is Oracle's
procedural SQL language. The proposed implementation uses MySQL, whose
stored-program syntax is not Oracle PL/SQL. If the instructor expects
literal PL/SQL syntax, prepare a small separate Oracle PL/SQL
demonstration in the institution's lab or confirm whether MySQL
procedures/functions/triggers are accepted as the practical equivalent.
Do not label MySQL stored routines as Oracle PL/SQL.

------------------------------------------------------------------------

## 20. Recommended Demonstration Screens

Build the following screens in priority order:

1.  Welcome / role-aware login screen.
2.  Registration screen.
3.  Farmer dashboard.
4.  Add/edit product screen.
5.  Farmer product management table.
6.  Buyer dashboard and product catalogue.
7.  Product details screen.
8.  Cart/order confirmation screen.
9.  Buyer order history and tracking screen.
10. Farmer order management screen.
11. Administrator dashboard.
12. User/farmer verification screen.
13. Category management screen.
14. Sales and inventory reports.

A polished, consistent interface is useful, but correctness of OOP
design, SQL, transactions, and database constraints is more important
than visual complexity.

------------------------------------------------------------------------

## 21. Development Plan

Build the project in phases. Do not begin by coding all screens at once.

### Phase 1 --- Requirements and design

-   Confirm team size, project rules, and instructor expectations.
-   Finalize user roles and core workflows.
-   Draw use-case and activity diagrams.
-   Create the ER diagram.
-   Define relational tables, keys, and constraints.
-   Prepare the OOP class diagram.

**Deliverables:** Problem statement, objectives, scope, use-case
diagram, ER diagram, class diagram, and schema draft.

### Phase 2 --- Database implementation

-   Create the MySQL database and tables.
-   Add constraints and sample records.
-   Test foreign keys and validation constraints.
-   Perform normalization and FD analysis.
-   Write core SQL queries.

**Deliverables:** SQL scripts, sample data, ER diagram, normalization
report, query file.

### Phase 3 --- Java foundation

-   Create the project structure.
-   Configure JDBC.
-   Implement model classes.
-   Implement DAO classes.
-   Implement service interfaces and service classes.
-   Test database connectivity and CRUD operations.

**Deliverables:** Working Java-MySQL connection and tested data access
layer.

### Phase 4 --- Core application

-   Implement registration and login.
-   Implement role-based dashboards.
-   Implement farmer product management.
-   Implement buyer catalogue and search.
-   Implement order creation and inventory transaction.
-   Implement order status and simulated payment records.

**Deliverables:** End-to-end working farmer-to-buyer workflow.

### Phase 5 --- Advanced DBMS and Java demonstrations

-   Add reports and aggregate queries.
-   Add stored routines and price-history trigger.
-   Add indexes and EXPLAIN comparisons.
-   Add background report generation.
-   Add CSV export.
-   Demonstrate rollback and backup/restore.
-   Prepare concurrency and serializability examples.

**Deliverables:** Advanced SQL demonstrations and OOP topic evidence.

### Phase 6 --- Testing and documentation

-   Run functional and database constraint tests.
-   Test invalid input and unauthorized actions.
-   Test transaction rollback.
-   Capture screenshots.
-   Prepare the final report and presentation.
-   Rehearse the demonstration and viva.

**Deliverables:** Final source code, SQL scripts, report, presentation,
test evidence, and demo database.

------------------------------------------------------------------------

## 22. Suggested Team Responsibilities

If the team has four members, divide responsibilities by modules while
keeping shared design decisions consistent.

  -----------------------------------------------------------------------
  Member                              Suggested responsibility
  ----------------------------------- -----------------------------------
  Member 1                            Database design, ER diagram,
                                      normalization, schema, constraints,
                                      SQL scripts

  Member 2                            Java models, inheritance,
                                      interfaces, service layer,
                                      exception handling

  Member 3                            Swing GUI, registration/login,
                                      farmer and buyer screens

  Member 4                            Order/inventory transaction,
                                      reports, testing, documentation and
                                      integration
  -----------------------------------------------------------------------

All members should understand: - The overall architecture. - The ER
diagram and relational schema. - The order transaction. - The main OOP
relationships. - At least a few important SQL queries.

These responsibilities are suggestions. Each member should contribute to
both implementation and understanding rather than treating one subject
as belonging to only one person.

------------------------------------------------------------------------

## 23. Final Project Deliverables

The final submission should include:

### Application

-   Java source code.
-   Runnable application or clear run instructions.
-   MySQL database setup scripts.
-   Sample data.
-   Configuration example without real credentials.
-   Required libraries and setup instructions.

### OOP documentation

-   Class diagram.
-   Explanation of inheritance, encapsulation, polymorphism, and
    abstraction.
-   Interfaces and exception-handling examples.
-   Collections and multithreading examples.
-   GUI screenshots.
-   File handling and Java 8 feature examples.

### DBMS documentation

-   Problem statement and requirements.
-   ER diagram and relational schema.
-   Keys, constraints, and cardinalities.
-   Functional dependencies and normalization steps.
-   SQL query collection.
-   Views, procedures, functions, and triggers.
-   Indexing and query optimization evidence.
-   ACID transaction example.
-   Schedules, serializability, and concurrency-control explanation.
-   Recovery and backup/restore evidence.
-   Distributed, NoSQL, Big Data, warehouse, OLAP/OLTP, and cloud topic
    discussion.

### Presentation and testing

-   Project presentation.
-   Test cases and results.
-   Screenshots of major workflows.
-   Demonstration dataset.
-   Known limitations and future scope.

------------------------------------------------------------------------

## 24. Future Scope

The system can be extended with: - A responsive web and mobile
application. - Regional language support and voice-based navigation. -
Verified quality grades and digital quality certificates. - AI-based
crop price forecasting using historical market data. - Demand
forecasting and recommendations. - Integration with real payment
gateways. - Live delivery partner and GPS tracking integrations. -
Weather and market-price information integrations. - Search indexing for
large product catalogues. - Cloud-hosted database infrastructure. -
Regional data distribution and analytical data warehouse.

These are future enhancements and should not be presented as completed
features in the current project.

------------------------------------------------------------------------

## 25. Project Success Criteria

The project will be considered successful when:

1.  A farmer can register, manage a product listing, and receive an
    order.
2.  A buyer can register, discover products, and place an order.
3.  An administrator can manage accounts and monitor activity.
4.  Product quantities remain correct after order placement and
    cancellation.
5.  Order, item, and payment records are stored consistently.
6.  Role and ownership checks prevent unauthorized operations.
7.  The Java implementation clearly demonstrates the core OOP
    principles.
8.  JDBC is used correctly with prepared statements and resource
    management.
9.  The database has a documented ER model, relational schema, keys,
    constraints, and normalization analysis.
10. SQL demonstrations cover joins, nested queries, aggregation, set
    operations, and database routines.
11. The team can explain indexing, query optimization, ACID,
    concurrency, and recovery using the project's examples.
12. Advanced syllabus topics are documented or demonstrated honestly,
    with no unsupported implementation claims.

------------------------------------------------------------------------

## 26. Final Recommendation

Keep the first release focused on the complete transaction journey:

**Farmer registration → Product listing → Buyer discovery → Order
placement → Stock update → Order fulfilment → Payment status → Sales
report.**

This single workflow provides a strong foundation for both subjects:

-   For **OOP**, it naturally requires objects, inheritance, interfaces,
    encapsulation, polymorphism, collections, exception handling, GUI
    events, JDBC, and background processing.
-   For **DBMS**, it naturally requires relational modelling,
    normalization, SQL, joins, aggregation, stored routines, indexes,
    transactions, concurrency control, and recovery.

Do not try to build every advanced syllabus topic as a production
feature. Build a reliable core application, then prepare technically
correct demonstrations and documentation for the advanced DBMS and Java
topics. This will make the project more complete, explainable, and
achievable.

**Core project principle:** A farmer's product, a buyer's order, and the
resulting inventory and payment records must remain connected, accurate,
and traceable throughout the entire transaction.
