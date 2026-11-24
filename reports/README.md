# Project Reports

Written for the course evaluators and the project team. Every result quoted
in these reports was produced by running the project's own SQL scripts and
Java code against MySQL 8.4.13 (Docker) on 24 Nov 2026. The scripts are in
[`database/demo/`](../database/demo/), so any result can be reproduced.

| Report | Covers (requirements section) |
|---|---|
| [architecture.md](architecture.md) | Layered architecture, class diagram, use cases (§8, §9) |
| [er_diagram.md](er_diagram.md) | ER / EER model, cardinalities, participation (§10.1–10.2) |
| [relational_schema.md](relational_schema.md) | Tables, keys, constraints, referential actions, rule ownership, Codd's rules (§10.3–10.4) |
| [normalization.md](normalization.md) | Anomalies, 1NF→BCNF, FDs, closure, minimal cover, synthesis, lossless join, 4NF (§11) |
| [relational_algebra.md](relational_algebra.md) | σ π ∪ − × ρ ⋈ ÷ with SQL mapping and results (§12.13) |
| [query_optimization.md](query_optimization.md) | Indexes, EXPLAIN experiments on 100,000 rows, storage concepts (§13) |
| [transaction_demo.md](transaction_demo.md) | ACID, order transaction, schedules, precedence graph, locking, recovery (§14) |
| [advanced_topics.md](advanced_topics.md) | Parallel/distributed DB, NoSQL, CAP/BASE, Hadoop, warehouse/OLAP, DBaaS (§15) |
| [security.md](security.md) | Database security and data governance (§16) |
| [test_cases.md](test_cases.md) | Test cases T01–T16 and direct constraint tests (§18) |
| [syllabus_coverage.md](syllabus_coverage.md) | OOP and DBMS syllabus mapping (§19) |

## SQL scripts used as evidence

| Script | Purpose |
|---|---|
| [`database/01…07_*.sql`](../database/) | Schema, indexes, sample data, views, routines, triggers, users (run automatically) |
| [`demo/demo_queries.sql`](../database/demo/demo_queries.sql) | Every SQL category in §12 (DDL, DML, SELECT Q1–Q10, set operations, functions, aggregates, joins, nested, DCL, TCL, views, routines, relational algebra, EXPLAIN) |
| [`demo/constraint_tests.sql`](../database/demo/constraint_tests.sql) | 17 statements that must be rejected by constraints (run with `--force`) |
| [`demo/concurrency_demo.sql`](../database/demo/concurrency_demo.sql) | Two-terminal walkthrough of row locking |
| [`demo/bulk_test_data.sql`](../database/demo/bulk_test_data.sql) | 100,000 synthetic products for index experiments (throwaway copy only) |

## Diagrams

Diagrams are written in [Mermaid](https://mermaid.js.org/). They render
directly on GitHub and in VS Code (with the *Markdown Preview Mermaid
Support* extension). To get a PNG for the printed report, paste the code
block into <https://mermaid.live> and use *Export → PNG*.
