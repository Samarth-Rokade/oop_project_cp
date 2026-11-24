# ER Model

## 1. Design process

Requirements (§5 of the brief) → entities and relationships (this file) →
relational schema ([relational_schema.md](relational_schema.md)) →
normalization check ([normalization.md](normalization.md)) → SQL
([`01_create_schema.sql`](../database/01_create_schema.sql)).

## 2. ER diagram (crow's-foot notation)

```mermaid
erDiagram
    USERS ||--o| FARMER_PROFILE : "is a (farmer)"
    USERS ||--o| BUYER_PROFILE  : "is a (buyer)"
    USERS ||--o{ ADDRESS        : has
    FARMER_PROFILE ||--o{ PRODUCT : lists
    CATEGORY ||--o{ PRODUCT : classifies
    UNIT     ||--o{ PRODUCT : "is sold in"
    BUYER_PROFILE ||--o{ ORDERS : places
    ADDRESS  ||--o{ ORDERS : "is delivered to"
    ORDERS   ||--|{ ORDER_ITEM : contains
    PRODUCT  ||--o{ ORDER_ITEM : "appears in"
    ORDERS   ||--o{ PAYMENT : "is paid by"
    ORDERS   ||--o| DELIVERY : "is shipped as"
    PRODUCT  ||--o{ PRICE_HISTORY : "has price changes"
    USERS    |o--o{ PRICE_HISTORY : "changed by"
    USERS    |o--o{ AUDIT_LOG : performs

    USERS {
        int user_id PK
        string full_name
        string email UK
        string phone
        string password_hash
        string role "FARMER | BUYER | ADMIN"
        string account_status
        timestamp created_at
    }
    FARMER_PROFILE {
        int farmer_id PK "FK -> users"
        string farm_name
        string farm_address
        string verification_status
    }
    BUYER_PROFILE {
        int buyer_id PK "FK -> users"
        string buyer_type
    }
    ADDRESS {
        int address_id PK
        int user_id FK
        string address_line
        string city
        string district
        string state
        string postal_code
        boolean is_default
    }
    CATEGORY {
        int category_id PK
        string category_name UK
        string description
    }
    UNIT {
        string unit_code PK
        string unit_name
    }
    PRODUCT {
        int product_id PK
        int farmer_id FK
        int category_id FK
        string unit_code FK
        string product_name
        decimal available_quantity
        decimal price_per_unit
        date harvest_date
        string listing_status
    }
    ORDERS {
        int order_id PK
        int buyer_id FK
        int delivery_address_id FK
        timestamp order_date
        string order_status
        decimal total_amount
    }
    ORDER_ITEM {
        int order_item_id PK
        int order_id FK
        int product_id FK
        decimal quantity
        decimal unit_price_at_order
        decimal line_total "generated"
    }
    PAYMENT {
        int payment_id PK
        int order_id FK
        string payment_method
        decimal amount
        string payment_status
        string transaction_reference UK
    }
    DELIVERY {
        int delivery_id PK
        int order_id FK,UK
        string delivery_status
        date dispatch_date
        date delivery_date
        string delivery_partner
    }
    PRICE_HISTORY {
        int price_history_id PK
        int product_id FK
        decimal old_price
        decimal new_price
        timestamp changed_at
        int changed_by FK
    }
    AUDIT_LOG {
        bigint log_id PK
        int user_id FK
        string action
        string entity_type
        int entity_id
        timestamp action_time
        string details
    }
```

## 3. Extended ER: specialization of USERS

```mermaid
flowchart TB
    U[USERS<br/>user_id, full_name, email,<br/>password_hash, role, account_status]
    D{{"d (disjoint)<br/>total"}}
    U === D
    D --- F[FARMER<br/>farm_name, farm_address,<br/>verification_status]
    D --- B[BUYER<br/>buyer_type]
    D --- A[ADMIN<br/>no extra attributes]
```

* **Disjoint:** a user has exactly one role (`role` is the discriminator,
  `chk_users_role` allows one value).
* **Total:** every user is a farmer, buyer or admin.
* **Mapping strategy:** one table for the superclass plus one table per
  subclass *that has its own attributes*, sharing the primary key
  (`farmer_profile.farmer_id` is both PK and FK to `users`). `ADMIN` has no
  extra attributes, so it needs no table.
* The same idea appears in Java: abstract `User` with subclasses
  `Farmer`, `Buyer`, `Admin` ([architecture.md](architecture.md)).
* What the database cannot enforce by itself: that a `role = 'FARMER'` row
  always has a `farmer_profile` row. `UserDAO.insertFarmer` inserts both in
  one transaction, so the pair is created together or not at all. A product
  can still only point to a real farmer — constraint test C07 shows that a
  buyer's id is rejected as `farmer_id`.

## 4. Relationships, cardinality and participation

| Relationship | Cardinality | Participation | Notes |
|---|---|---|---|
| USERS – FARMER_PROFILE | 1 : 0..1 | user partial, profile **total** | specialization (shared PK) |
| USERS – BUYER_PROFILE | 1 : 0..1 | user partial, profile **total** | specialization |
| USERS – ADDRESS | 1 : N | address **total** | `ON DELETE CASCADE` |
| FARMER_PROFILE – PRODUCT | 1 : N | product **total**, farmer partial | a new farmer may have no products |
| CATEGORY – PRODUCT | 1 : N | product **total**, category partial | |
| UNIT – PRODUCT | 1 : N | product **total**, unit partial | |
| BUYER_PROFILE – ORDERS | 1 : N | order **total**, buyer partial | |
| ADDRESS – ORDERS | 1 : N | order **total** | address kept for history (`RESTRICT`) |
| ORDERS – ORDER_ITEM | 1 : 1..N | **both total** | an order needs ≥ 1 item (enforced by `OrderService`) |
| PRODUCT – ORDER_ITEM | 1 : N | item **total**, product partial | |
| ORDERS – PAYMENT | 1 : N | payment **total**, order partial | retries/refunds possible |
| ORDERS – DELIVERY | 1 : 0..1 | delivery **total** | `UNIQUE(order_id)` makes it 1 : 0..1 |
| PRODUCT – PRICE_HISTORY | 1 : N | history **total** | written by trigger |
| USERS – AUDIT_LOG | 0..1 : N | both partial | `user_id` NULL for system events |

**ORDERS ⟷ PRODUCT is many-to-many.** It is resolved by the associative
entity `ORDER_ITEM`, which also stores the attributes that belong to the
pair: quantity and the price *at the time of the order*.

**One order belongs to one farm.** The application splits a cart into one
order per farm (see [transaction_demo.md](transaction_demo.md)). This keeps
accept/reject decisions simple: each farmer handles only their own orders.
