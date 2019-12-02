---
id: MySQL
title: MySQL
---

#### MySQL

Ignore any conflict, e.g. `insert ignore`
```scala
val a = quote {
  query[Product].insert(_.id -> 1, _.sku -> 10).onConflictIgnore
}

// INSERT IGNORE INTO Product (id,sku) VALUES (1, 10)
```

Ignore duplicate key conflict by explicitly setting it
```scala
val a = quote {
  query[Product].insert(_.id -> 1, _.sku -> 10).onConflictIgnore(_.id)
}

// INSERT INTO Product (id,sku) VALUES (1, 10) ON DUPLICATE KEY UPDATE id=id
```

Resolve duplicate key by updating existing row if needed. In `onConflictUpdate((t, e) => assignment)`: `t` refers to
existing row and `e` - to values, e.g. values proposed for insert.
```scala
val a = quote {
  query[Product]
    .insert(_.id -> 1, _.sku -> 10)
    .onConflictUpdate((t, e) => t.sku -> (t.sku + e.sku))
}

// INSERT INTO Product (id,sku) VALUES (1, 10) ON DUPLICATE KEY UPDATE sku = (sku + VALUES(sku))
```