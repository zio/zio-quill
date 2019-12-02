---
id: Extending_Quill
title: Extending Quill
---


# Extending quill

## Infix

Infix is a very flexible mechanism to use non-supported features without having to use plain queries in the target language. It allows the insertion of arbitrary strings within quotations.

For instance, quill doesn't support the `FOR UPDATE` SQL feature. It can still be used through infix and implicit classes:

```scala
implicit class ForUpdate[T](q: Query[T]) {
  def forUpdate = quote(infix"$q FOR UPDATE".as[Query[T]])
}

val a = quote {
  query[Person].filter(p => p.age < 18).forUpdate
}

ctx.run(a)
// SELECT p.name, p.age FROM person p WHERE p.age < 18 FOR UPDATE
```

The `forUpdate` quotation can be reused for multiple queries.

Queries that contain `infix` will generally not be flattened since it is not assumed that the contents
of the infix are a pure function.
> Since SQL is typically less performant when there are many nested queries,
be careful with the use of `infix` in queries that have multiple `map`+`filter` clauses.

```scala
case class Data(id: Int)
case class DataAndRandom(id: Int, value: Int)

// This should be alright:
val q = quote {
  query[Data].map(e => DataAndRandom(e.id, infix"RAND()".as[Int])).filter(r => r.value <= 10)
}
run(q)
// SELECT e.id, e.value FROM (SELECT RAND() AS value, e.id AS id FROM Data e) AS e WHERE e.value <= 10

// This might not be:
val q = quote {
  query[Data]
    .map(e => DataAndRandom(e.id, infix"SOME_UDF(${e.id})".as[Int]))
    .filter(r => r.value <= 10)
    .map(e => DataAndRandom(e.id, infix"SOME_OTHER_UDF(${e.value})".as[Int]))
    .filter(r => r.value <= 100)
}
// Produces too many layers of nesting!
run(q)
// SELECT e.id, e.value FROM (
//   SELECT SOME_OTHER_UDF(e.value) AS value, e.id AS id FROM (
//     SELECT SOME_UDF(e.id) AS value, e.id AS id FROM Data e
//   ) AS e WHERE e.value <= 10
// ) AS e WHERE e.value <= 100
```

If you are sure that the the content of your infix is a pure function, you canse use the `pure` method
in order to indicate to Quill that the infix clause can be copied in the query. This gives Quill much
more leeway to flatten your query, possibly improving performance.

```scala
val q = quote {
  query[Data]
    .map(e => DataAndRandom(e.id, infix"SOME_UDF(${e.id})".pure.as[Int]))
    .filter(r => r.value <= 10)
    .map(e => DataAndRandom(e.id, infix"SOME_OTHER_UDF(${e.value})".pure.as[Int]))
    .filter(r => r.value <= 100)
}
// Copying SOME_UDF and SOME_OTHER_UDF allows the query to be completely flattened.
run(q)
// SELECT e.id, SOME_OTHER_UDF(SOME_UDF(e.id)) FROM Data e 
// WHERE SOME_UDF(e.id) <= 10 AND SOME_OTHER_UDF(SOME_UDF(e.id)) <= 100
```

### Dynamic infix

Infix supports runtime string values through the `#$` prefix. Example:

```scala
def test(functionName: String) =
  ctx.run(query[Person].map(p => infix"#$functionName(${p.name})".as[Int]))
```

### Raw SQL queries

You can also use infix to port raw SQL queries to Quill and map it to regular Scala tuples.

```scala
val rawQuery = quote {
  (id: Int) => infix"""SELECT id, name FROM my_entity WHERE id = $id""".as[Query[(Int, String)]]
}
ctx.run(rawQuery(1))
//SELECT x._1, x._2 FROM (SELECT id AS "_1", name AS "_2" FROM my_entity WHERE id = 1) x
```

Note that in this case the result query is nested.
It's required since Quill is not aware of a query tree and cannot safely unnest it.
This is different from the example above because infix starts with the query `infix"$q...` where its tree is already compiled

### Database functions

A custom database function can also be used through infix:

```scala
val myFunction = quote {
  (i: Int) => infix"MY_FUNCTION($i)".as[Int]
}

val q = quote {
  query[Person].map(p => myFunction(p.age))
}

ctx.run(q)
// SELECT MY_FUNCTION(p.age) FROM Person p
```

### Comparison operators

You can implement comparison operators by defining implicit conversion and using infix.

```scala
import java.util.Date

implicit class DateQuotes(left: Date) {
  def >(right: Date) = quote(infix"$left > $right".as[Boolean])

  def <(right: Date) = quote(infix"$left < $right".as[Boolean])
}
```

### batch with infix

```scala
implicit class OnDuplicateKeyIgnore[T](q: Insert[T]) {
  def ignoreDuplicate = quote(infix"$q ON DUPLICATE KEY UPDATE id=id".as[Insert[T]])
}

ctx.run(
  liftQuery(List(
    Person(1, "Test1", 30),
    Person(2, "Test2", 31)
  )).foreach(row => query[Person].insert(row).ignoreDuplicate)
)
```

## Custom encoding

Quill uses `Encoder`s to encode query inputs and `Decoder`s to read values returned by queries. The library provides a few built-in encodings and two mechanisms to define custom encodings: mapped encoding and raw encoding.

### Mapped Encoding

If the correspondent database type is already supported, use `MappedEncoding`. In this example, `String` is already supported by Quill and the `UUID` encoding from/to `String` is defined through mapped encoding:

```scala
import ctx._
import java.util.UUID

implicit val encodeUUID = MappedEncoding[UUID, String](_.toString)
implicit val decodeUUID = MappedEncoding[String, UUID](UUID.fromString(_))
```

A mapped encoding also can be defined without a context instance by importing `io.getquill.MappedEncoding`:

```scala
import io.getquill.MappedEncoding
import java.util.UUID

implicit val encodeUUID = MappedEncoding[UUID, String](_.toString)
implicit val decodeUUID = MappedEncoding[String, UUID](UUID.fromString(_))
```
Note that can it be also used to provide mapping for element types of collection (SQL Arrays or Cassandra Collections).

### Raw Encoding

If the database type is not supported by Quill, it is possible to provide "raw" encoders and decoders:

```scala
trait UUIDEncodingExample {
  val jdbcContext: PostgresJdbcContext[Literal] // your context should go here

  import jdbcContext._

  implicit val uuidDecoder: Decoder[UUID] =
    decoder((index, row) =>
      UUID.fromString(row.getObject(index).toString)) // database-specific implementation
    
  implicit val uuidEncoder: Encoder[UUID] =
    encoder(java.sql.Types.OTHER, (index, value, row) =>
        row.setObject(index, value, java.sql.Types.OTHER)) // database-specific implementation

  // Only for postgres
  implicit def arrayUUIDEncoder[Col <: Seq[UUID]]: Encoder[Col] = arrayRawEncoder[UUID, Col]("uuid")
  implicit def arrayUUIDDecoder[Col <: Seq[UUID]](implicit bf: CBF[UUID, Col]): Decoder[Col] =
    arrayRawDecoder[UUID, Col]
}
```

## `AnyVal`

Quill automatically encodes `AnyVal`s (value classes):

```scala
case class UserId(value: Int) extends AnyVal
case class User(id: UserId, name: String)

val q = quote {
  for {
    u <- query[User] if u.id == lift(UserId(1))
  } yield u
}

ctx.run(q)
// SELECT u.id, u.name FROM User u WHERE (u.id = 1)
```

## Meta DSL

The meta DSL allows the user to customize how Quill handles the expansion and execution of quotations through implicit meta instances.

### Schema meta

By default, quill expands `query[Person]` to `querySchema[Person]("Person")`. It's possible to customize this behavior using an implicit instance of `SchemaMeta`:

```scala
def example = {
  implicit val personSchemaMeta = schemaMeta[Person]("people", _.id -> "person_id")

  ctx.run(query[Person])
  // SELECT x.person_id, x.name, x.age FROM people x
}
```

### Insert meta

`InsertMeta` customizes the expansion of case classes for insert actions (`query[Person].insert(p)`). By default, all columns are expanded and through an implicit `InsertMeta`, it's possible to exclude columns from the expansion:

```scala
implicit val personInsertMeta = insertMeta[Person](_.id)

ctx.run(query[Person].insert(lift(Person(-1, "John", 22))))
// INSERT INTO Person (name,age) VALUES (?, ?)
```

Note that the parameter of `insertMeta` is called `exclude`, but it isn't possible to use named parameters for macro invocations.

### Update meta

`UpdateMeta` customizes the expansion of case classes for update actions (`query[Person].update(p)`). By default, all columns are expanded, and through an implicit `UpdateMeta`, it's possible to exclude columns from the expansion:

```scala
implicit val personUpdateMeta = updateMeta[Person](_.id)

ctx.run(query[Person].filter(_.id == 1).update(lift(Person(1, "John", 22))))
// UPDATE Person SET name = ?, age = ? WHERE id = 1
```

Note that the parameter of `updateMeta` is called `exclude`, but it isn't possible to use named parameters for macro invocations.

### Query meta

This kind of meta instance customizes the expansion of query types and extraction of the final value. For instance, it's possible to use this feature to normalize values before reading them from the database:

```scala
implicit val personQueryMeta = 
  queryMeta(
    (q: Query[Person]) =>
      q.map(p => (p.id, infix"CONVERT(${p.name} USING utf8)".as[String], p.age))
  ) {
    case (id, name, age) =>
      Person(id, name, age)
  }
```

The query meta definition is open and allows the user to even join values from other tables before reading the final value. This kind of usage is not encouraged.

# Contexts