---
id: SQL_Dialects
title: SQL Dialects
---

## SQL Contexts

Example:

```scala
lazy val ctx = new MysqlJdbcContext(SnakeCase, "ctx")
```

### Dialect

The SQL dialect parameter defines the specific database dialect to be used. Some context types are specific to a database and thus not require it.

Quill has five built-in dialects:

- `io.getquill.H2Dialect`
- `io.getquill.MySQLDialect`
- `io.getquill.PostgresDialect`
- `io.getquill.SqliteDialect`
- `io.getquill.SQLServerDialect`
- `io.getquill.OracleDialect`

### Naming strategy

The naming strategy parameter defines the behavior when translating identifiers (table and column names) to SQL.

|           strategy                  |          example              |
|-------------------------------------|-------------------------------|
| `io.getquill.naming.Literal`        | some_ident  -> some_ident     |
| `io.getquill.naming.Escape`         | some_ident  -> "some_ident"   |
| `io.getquill.naming.UpperCase`      | some_ident  -> SOME_IDENT     |
| `io.getquill.naming.LowerCase`      | SOME_IDENT  -> some_ident     |
| `io.getquill.naming.SnakeCase`      | someIdent   -> some_ident     |
| `io.getquill.naming.CamelCase`      | some_ident  -> someIdent      |
| `io.getquill.naming.MysqlEscape`    | some_ident  -> \`some_ident\` |
| `io.getquill.naming.PostgresEscape` | $some_ident -> $some_ident    |

Multiple transformations can be defined using `NamingStrategy()`. For instance, the naming strategy

```NamingStrategy(SnakeCase, UpperCase)```

produces the following transformation:

```someIdent -> SOME_IDENT```

The transformations are applied from left to right.
