# Database Design & Scalability Strategy

## Version

1.0

## Purpose

This document outlines the database design strategy for the AI Financial Coach MVP, focusing on simplicity, scalability, and future extensibility.

The goal is to build a schema that is easy to develop with today while allowing future optimization without major architectural changes.

---

# Why a Single Transactions Table is Acceptable

A common concern is that transaction data grows rapidly.

Example:

```text
100 Users
500 Transactions/User

Total = 50,000 Rows
```

Even:

```text
10,000 Users
1,000 Transactions/User

Total = 10 Million Rows
```

is well within the capabilities of PostgreSQL when proper indexing is used.

For the MVP:

* No table partitioning is required.
* No sharding is required.
* No distributed databases are required.

A single transactions table is sufficient.

---

# Design Principles

## Keep It Simple

Avoid:

* Partitioning
* Sharding
* Event Sourcing
* Data Warehouses

The primary objective is validating the product idea rather than solving hypothetical scaling problems.

---

## Design for Future Growth

The schema should support:

* PDF imports
* CSV imports
* Email imports
* Future UPI integrations
* Future Bank API integrations

without requiring major schema changes.

---

# Recommended Database Schema

---

## Users Table

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,

    name VARCHAR(255),

    email VARCHAR(255) UNIQUE,

    password_hash TEXT,

    created_at TIMESTAMP,

    updated_at TIMESTAMP
);
```

---

## Categories Table

Categories should be normalized rather than stored as strings inside transactions.

### Benefits

* Smaller row size
* Faster filtering
* Easier reporting
* Cleaner analytics

```sql
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,

    name VARCHAR(100) UNIQUE
);
```

Example Categories:

```text
Food
Shopping
Travel
Bills
Entertainment
Healthcare
Other
```

---

## Transaction Imports Table

Tracks statement uploads.

### Purpose

Prevents duplicate imports and improves debugging.

Example:

```text
January_Statement.pdf

Imported:
2026-01-05

Transactions:
312
```

```sql
CREATE TABLE transaction_imports (

    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    file_name VARCHAR(255),

    source_type VARCHAR(20),

    transaction_count INTEGER,

    imported_at TIMESTAMP
);
```

---

## Transactions Table

Core financial data table.

```sql
CREATE TABLE transactions (

    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    import_id UUID,

    category_id INTEGER,

    amount DECIMAL(12,2) NOT NULL,

    merchant VARCHAR(255),

    description TEXT,

    transaction_type VARCHAR(20),

    source_type VARCHAR(20),

    transaction_date DATE,

    created_at TIMESTAMP,

    FOREIGN KEY(user_id)
        REFERENCES users(id),

    FOREIGN KEY(import_id)
        REFERENCES transaction_imports(id),

    FOREIGN KEY(category_id)
        REFERENCES categories(id)
);
```

---

# Source Types

The source_type field enables future expansion.

Supported values:

```text
PDF
CSV
EMAIL
UPI
BANK_API
```

This allows multiple ingestion pipelines while maintaining a single transaction model.

---

# Recommended Indexes

Indexes are significantly more important than premature partitioning.

---

## User Lookup

```sql
CREATE INDEX idx_transactions_user
ON transactions(user_id);
```

Used for:

```sql
SELECT *
FROM transactions
WHERE user_id = ?;
```

---

## Dashboard Queries

```sql
CREATE INDEX idx_transactions_user_date
ON transactions(user_id, transaction_date);
```

Used for:

```sql
SELECT SUM(amount)
FROM transactions
WHERE user_id = ?
AND transaction_date BETWEEN ? AND ?;
```

---

## Category Analytics

```sql
CREATE INDEX idx_transactions_user_category
ON transactions(user_id, category_id);
```

Used for:

```sql
SELECT category_id,
       SUM(amount)
FROM transactions
WHERE user_id = ?
GROUP BY category_id;
```

---

## Import Tracking

```sql
CREATE INDEX idx_transactions_import
ON transactions(import_id);
```

Used for:

```sql
SELECT *
FROM transactions
WHERE import_id = ?;
```

---

# Query Design Guidelines

The biggest performance gains come from efficient queries rather than schema complexity.

---

## Good Example

Calculate totals directly in PostgreSQL.

```sql
SELECT SUM(amount)
FROM transactions
WHERE user_id = ?
AND transaction_date BETWEEN ? AND ?;
```

---

## Bad Example

```sql
SELECT *
FROM transactions;
```

Then:

```java
for(Transaction t : transactions){
    ...
}
```

Perform aggregation in SQL whenever possible.

---

# Future Scaling Strategy

## Stage 1 — MVP

```text
PostgreSQL

Single Transactions Table

Indexed Queries
```

Expected Scale:

```text
0 - 100,000 Users
```

No architectural changes required.

---

## Stage 2 — Growth

If transaction volume becomes very large:

```text
10M+
50M+
100M+ Rows
```

Introduce PostgreSQL Partitioning.

---

## Partitioning Strategy

Partition by transaction date.

Example:

```text
transactions_2026

transactions_2027

transactions_2028
```

or

```text
transactions_jan_2026

transactions_feb_2026

transactions_mar_2026
```

Benefits:

* Faster scans
* Smaller indexes
* Better maintenance

Application code remains mostly unchanged.

---

## Stage 3 — Large Scale

Only after substantial growth:

```text
Analytics Database

Data Warehouse

Read Replicas
```

Potential Technologies:

```text
ClickHouse

BigQuery

Snowflake
```

Not required for the MVP.

---

# Recommended MVP Database Structure

```text
users
│
├── transaction_imports
│
└── transactions
      │
      └── categories
```

---

# Final Recommendation

For the MVP:

✅ Single PostgreSQL instance

✅ Single transactions table

✅ Proper indexing

✅ Normalized categories

✅ Import tracking

✅ SQL-based aggregation

Avoid:

❌ Partitioning

❌ Sharding

❌ Distributed Databases

❌ Complex Data Pipelines

The primary engineering challenge is not transaction storage. The most important problems to solve are:

1. Accurate statement parsing
2. Reliable transaction categorization
3. Useful analytics generation
4. High-quality AI financial recommendations

Focus development effort on these areas first and optimize the database only when actual usage patterns justify it.
