---
id: Cassandra
title: Cassandra
---

## Cassandra-specific encoding

```scala
val ctx = new CassandraMirrorContext(Literal)
import ctx._
```

### Collections

The Cassandra context provides List, Set, and Map encoding:

```scala

case class Book(id: Int, notes: Set[String], pages: List[Int], history: Map[Int, Boolean])

ctx.run(query[Book])
// SELECT id, notes, pages, history FROM Book
```

### User-Defined Types

The cassandra context provides encoding of UDT (user-defined types).
```scala
import io.getquill.context.cassandra.Udt

case class Name(firstName: String, lastName: String) extends Udt
```

To encode the UDT and bind it into the query (insert/update queries), the context needs to retrieve UDT metadata from
the cluster object. By default, the context looks for UDT metadata within the currently logged keyspace, but it's also possible to specify a
concrete keyspace with `udtMeta`:

```scala
implicit val nameMeta = udtMeta[Name]("keyspace2.my_name")
```
When a keyspace is not set in `udtMeta` then the currently logged one is used.

Since it's possible to create a context without
specifying a keyspace, (e.g. the keyspace parameter is null and the session is not bound to any keyspace), the UDT metadata will be
resolved throughout the entire cluster.

It is also possible to rename UDT columns with `udtMeta`:

```scala
implicit val nameMeta = udtMeta[Name]("name", _.firstName -> "first", _.lastName -> "last")
```

## Cassandra-specific operations

The cassandra context also provides a few additional operations:

### allowFiltering
```scala
val q = quote {
  query[Person].filter(p => p.age > 10).allowFiltering
}
ctx.run(q)
// SELECT id, name, age FROM Person WHERE age > 10 ALLOW FILTERING
```

### ifNotExists
```scala
val q = quote {
  query[Person].insert(_.age -> 10, _.name -> "John").ifNotExists
}
ctx.run(q)
// INSERT INTO Person (age,name) VALUES (10, 'John') IF NOT EXISTS
```

### ifExists
```scala
val q = quote {
  query[Person].filter(p => p.name == "John").delete.ifExists
}
ctx.run(q)
// DELETE FROM Person WHERE name = 'John' IF EXISTS
```

### usingTimestamp
```scala
val q1 = quote {
  query[Person].insert(_.age -> 10, _.name -> "John").usingTimestamp(99)
}
ctx.run(q1)
// INSERT INTO Person (age,name) VALUES (10, 'John') USING TIMESTAMP 99

val q2 = quote {
  query[Person].usingTimestamp(99).update(_.age -> 10)
}
ctx.run(q2)
// UPDATE Person USING TIMESTAMP 99 SET age = 10
```

### usingTtl
```scala
val q1 = quote {
  query[Person].insert(_.age -> 10, _.name -> "John").usingTtl(11)
}
ctx.run(q1)
// INSERT INTO Person (age,name) VALUES (10, 'John') USING TTL 11

val q2 = quote {
  query[Person].usingTtl(11).update(_.age -> 10)
}
ctx.run(q2)
// UPDATE Person USING TTL 11 SET age = 10

val q3 = quote {
  query[Person].usingTtl(11).filter(_.name == "John").delete
}
ctx.run(q3)  
// DELETE FROM Person USING TTL 11 WHERE name = 'John'
```

### using
```scala
val q1 = quote {
  query[Person].insert(_.age -> 10, _.name -> "John").using(ts = 99, ttl = 11)
}
ctx.run(q1)
// INSERT INTO Person (age,name) VALUES (10, 'John') USING TIMESTAMP 99 AND TTL 11

val q2 = quote {
  query[Person].using(ts = 99, ttl = 11).update(_.age -> 10)
}
ctx.run(q2)
// UPDATE Person USING TIMESTAMP 99 AND TTL 11 SET age = 10

val q3 = quote {
  query[Person].using(ts = 99, ttl = 11).filter(_.name == "John").delete
}
ctx.run(q3)
// DELETE FROM Person USING TIMESTAMP 99 AND TTL 11 WHERE name = 'John'
```

### ifCond
```scala
val q1 = quote {
  query[Person].update(_.age -> 10).ifCond(_.name == "John")
}
ctx.run(q1)
// UPDATE Person SET age = 10 IF name = 'John'

val q2 = quote {
  query[Person].filter(_.name == "John").delete.ifCond(_.age == 10)
}
ctx.run(q2)
// DELETE FROM Person WHERE name = 'John' IF age = 10
```

### delete column
```scala
val q = quote {
  query[Person].map(p => p.age).delete
}
ctx.run(q)
// DELETE p.age FROM Person
```

### list.contains / set.contains
requires `allowFiltering`
```scala
val q = quote {
  query[Book].filter(p => p.pages.contains(25)).allowFiltering
}
ctx.run(q)
// SELECT id, notes, pages, history FROM Book WHERE pages CONTAINS 25 ALLOW FILTERING
```

### map.contains
requires `allowFiltering`
```scala
val q = quote {
  query[Book].filter(p => p.history.contains(12)).allowFiltering
}
ctx.run(q)
// SELECT id, notes, pages, history FROM book WHERE history CONTAINS 12 ALLOW FILTERING
```

### map.containsValue
requires `allowFiltering`
```scala
val q = quote {
  query[Book].filter(p => p.history.containsValue(true)).allowFiltering
}
ctx.run(q)
// SELECT id, notes, pages, history FROM book WHERE history CONTAINS true ALLOW FILTERING
```

## Importing

TODO Link to: Importing Quill -> Cassandra
