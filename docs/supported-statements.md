# Flink SQL Gateway Supported Statements

Flink SQL Gateway currently supports the following statements. For what the results of each types of statements look like, see the [REST API document](docs/rest-api.md).

|  statement   | comment  |
|  ----  | ----  |
| SHOW CATALOGS | List all registered catalogs |
| SHOW DATABASES | List all databases in the current catalog |
| SHOW TABLES | List all tables and views in the current database of the current catalog |
| SHOW VIEWS | List all views in the current database of the current catalog |
| SHOW FUNCTIONS | List all functions |
| SHOW MODULES | List all modules |
| USE CATALOG catalog_name | Set a catalog with given name as the current catalog |
| USE database_name | Set a database with given name as the current database of the current catalog |
| CREATE TABLE table_name ... | Create a table with a DDL statement |
| DROP TABLE table_name | Drop a table with given name |
| ALTER TABLE table_name | Alter a table with given name |
| CREATE DATABASE database_name ... | Create a database in current catalog with given name |
| DROP DATABASE database_name ... | Drop a database with given name |
| ALTER DATABASE database_name ... | Alter a database with given name |
| CREATE VIEW view_name AS ... | Add a view in current session with SELECT statement |
| DROP VIEW view_name ... | Drop a table with given name |
| SET xx=yy | Set given key's session property to the specific value |
| SET | List all session's properties |
| RESET ALL | Reset all session's properties set by `SET` command |
| DESCRIBE table_name | Show the schema of a table |
| EXPLAIN PLAN FOR ... | Show string-based explanation about AST and execution plan of the given statement |
| SELECT ... | Submit a Flink `SELECT` SQL job |
| INSERT INTO ... | Submit a Flink `INSERT INTO` SQL job |
| INSERT OVERWRITE ... | Submit a Flink `INSERT OVERWRITE` SQL job |