---
id: Introduction
title: Introduction
---

## Introduction

The QDSL allows the user to write plain Scala code, leveraging Scala's syntax and type system. Quotations are created using the `quote` method and can contain any excerpt of code that uses supported operations. To create quotations, first create a context instance. Please see the [context](#contexts) section for more details on the different context available.

For this documentation, a special type of context that acts as a [mirror](#mirror-context) is used:

```scala
import io.getquill._

val ctx = new SqlMirrorContext(MirrorSqlDialect, Literal)
```

> ### **Note:** [Scastie](https://scastie.scala-lang.org/) is a great tool to try out Quill without having to prepare a local environment. It works with [mirror contexts](#mirror-context), see [this](https://scastie.scala-lang.org/QwOewNEiR3mFlKIM7v900A) snippet as an example.

The context instance provides all the types, methods, and encoders/decoders needed for quotations:

```scala
import ctx._
```

A quotation can be a simple value:

```scala
val pi = quote(3.14159)
```

And be used within another quotation:

```scala
case class Circle(radius: Float)

val areas = quote {
  query[Circle].map(c => pi * c.radius * c.radius)
}
```

Quotations can also contain high-order functions and inline values:

```scala
val area = quote {
  (c: Circle) => {
    val r2 = c.radius * c.radius
    pi * r2
  }
}
```

```scala
val areas = quote {
  query[Circle].map(c => area(c))
}
```

Quill's normalization engine applies reduction steps before translating the quotation to the target language. The correspondent normalized quotation for both versions of the `areas` query is:

```scala
val areas = quote {
  query[Circle].map(c => 3.14159 * c.radius * c.radius)
}
```

Scala doesn't have support for high-order functions with type parameters. It's possible to use a method type parameter for this purpose:

```scala
def existsAny[T] = quote {
  (xs: Query[T]) => (p: T => Boolean) =>
    	xs.filter(p(_)).nonEmpty
}

val q = quote {
  query[Circle].filter { c1 =>
    existsAny(query[Circle])(c2 => c2.radius > c1.radius)
  }
}
```

## Compile-time quotations

Quotations are both compile-time and runtime values. Quill uses a type refinement to store the quotation's AST as an annotation available at compile-time and the `q.ast` method exposes the AST as runtime value.

It is important to avoid giving explicit types to quotations when possible. For instance, this quotation can't be read at compile-time as the type refinement is lost:

```scala
// Avoid type widening (Quoted[Query[Circle]]), or else the quotation will be dynamic.
val q: Quoted[Query[Circle]] = quote {
  query[Circle].filter(c => c.radius > 10)
}

ctx.run(q) // Dynamic query
```

Quill falls back to runtime normalization and query generation if the quotation's AST can't be read at compile-time. Please refer to [dynamic queries](#dynamic-queries) for more information.

#### Inline queries

Quoting is implicit when writing a query in a `run` statement.

```scala
ctx.run(query[Circle].map(_.radius))
// SELECT r.radius FROM Circle r
```

## Bindings

Quotations are designed to be self-contained, without references to runtime values outside their scope. There are two mechanisms to explicitly bind runtime values to a quotation execution.

### Lifted values

A runtime value can be lifted to a quotation through the method `lift`:

```scala
def biggerThan(i: Float) = quote {
  query[Circle].filter(r => r.radius > lift(i))
}
ctx.run(biggerThan(10)) // SELECT r.radius FROM Circle r WHERE r.radius > ?
```

### Lifted queries

A `Iterable` instance can be lifted as a `Query`. There are two main usages for lifted queries:

#### contains

```scala
def find(radiusList: List[Float]) = quote {
  query[Circle].filter(r => liftQuery(radiusList).contains(r.radius))
}
ctx.run(find(List(1.1F, 1.2F))) 
// SELECT r.radius FROM Circle r WHERE r.radius IN (?)
```

#### batch action
```scala
def insert(circles: List[Circle]) = quote {
  liftQuery(circles).foreach(c => query[Circle].insert(c))
}
ctx.run(insert(List(Circle(1.1F), Circle(1.2F)))) 
// INSERT INTO Circle (radius) VALUES (?)
```

## Schema

The database schema is represented by case classes. By default, quill uses the class and field names as the database identifiers:

```scala
case class Circle(radius: Float)

val q = quote {
  query[Circle].filter(c => c.radius > 1)
}

ctx.run(q) // SELECT c.radius FROM Circle c WHERE c.radius > 1
```

### Schema customization

Alternatively, the identifiers can be customized:

```scala
val circles = quote {
  querySchema[Circle]("circle_table", _.radius -> "radius_column")
}

val q = quote {
  circles.filter(c => c.radius > 1)
}

ctx.run(q)
// SELECT c.radius_column FROM circle_table c WHERE c.radius_column > 1
```

If multiple tables require custom identifiers, it is good practice to define a `schema` object with all table queries to be reused across multiple queries:

```scala
case class Circle(radius: Int)
case class Rectangle(length: Int, width: Int)
object schema {
  val circles = quote {
    querySchema[Circle](
        "circle_table",
        _.radius -> "radius_column")
  }
  val rectangles = quote {
    querySchema[Rectangle](
        "rectangle_table",
        _.length -> "length_column",
        _.width -> "width_column")
  }
}
```

### Database-generated values

#### returningGenerated

Database generated values can be returned from an insert query by using `.returningGenerated`. These properties
will also be excluded from the insertion since they are database generated.

```scala
case class Product(id: Int, description: String, sku: Long)

val q = quote {
  query[Product].insert(lift(Product(0, "My Product", 1011L))).returningGenerated(_.id)
}

val returnedIds = ctx.run(q) //: List[Int]
// INSERT INTO Product (description,sku) VALUES (?, ?) -- NOTE that 'id' is not being inserted.
```

Multiple properties can be returned in a Tuple or Case Class and all of them will be excluded from insertion.

> NOTE: Using multiple properties is currently supported by Postgres, Oracle and SQL Server

```scala
// Assuming sku is generated by the database.
val q = quote {
  query[Product].insert(lift(Product(0, "My Product", 1011L))).returningGenerated(r => (id, sku))
}

val returnedIds = ctx.run(q) //: List[(Int, Long)]
// INSERT INTO Product (description) VALUES (?) RETURNING id, sku -- NOTE that 'id' and 'sku' are not being inserted.
```

#### returning

In certain situations, we might want to return fields that are not auto generated as well. In this case we do not want 
the fields to be automatically excluded from the insertion. The `returning` method is used for that.
 
```scala
val q = quote {
  query[Product].insert(lift(Product(0, "My Product", 1011L))).returning(r => (id, description))
}

val returnedIds = ctx.run(q) //: List[(Int, String)]
// INSERT INTO Product (id, description, sku) VALUES (?, ?, ?) RETURNING id, description
```

Wait a second! Why did we just insert `id` into the database? That is because `returning` does not exclude values
from the insertion! We can fix this situation by manually specifying the columns to insert:

```scala
val q = quote {
  query[Product].insert(_.description -> "My Product", _.sku -> 1011L))).returning(r => (id, description))
}

val returnedIds = ctx.run(q) //: List[(Int, String)]
// INSERT INTO Product (description, sku) VALUES (?, ?) RETURNING id, description
```
 
 We can also fix this situation by using an insert-meta.

```scala
implicit val productInsertMeta = insertMeta[Product](_.id)
val q = quote {
  query[Product].insert(lift(Product(0L, "My Product", 1011L))).returning(r => (id, description))
}

val returnedIds = ctx.run(q) //: List[(Int, String)]
// INSERT INTO Product (description, sku) VALUES (?, ?) RETURNING id, description
```

#### Customization

##### Postgres

The `returning` and `returningGenerated` methods also support arithmetic operations, SQL UDFs and 
even entire queries. These are inserted directly into the SQL `RETURNING` clause.

Assuming this basic query:
```scala
val q = quote {
  query[Product].insert(_.description -> "My Product", _.sku -> 1011L)
}
```

Add 100 to the value of `id`:
```scala
ctx.run(q.returning(r => id + 100)) //: List[Int]
// INSERT INTO Product (description, sku) VALUES (?, ?) RETURNING id + 100
```

Pass the value of `id` into a UDF:
```scala
val udf = quote { (i: Long) => infix"myUdf($i)".as[Int] }
ctx.run(q.returning(r => udf(id))) //: List[Int]
// INSERT INTO Product (description, sku) VALUES (?, ?) RETURNING myUdf(id)
```

Use the return value of `sku` to issue a query:
```scala
case class Supplier(id: Int, clientSku: Long)
ctx.run { 
  q.returning(r => query[Supplier].filter(s => s.sku == r.sku).map(_.id).max) 
} //: List[Option[Long]]
// INSERT INTO Product (description,sku) VALUES ('My Product', 1011) RETURNING (SELECT MAX(s.id) FROM Supplier s WHERE s.sku = clientSku)
```

As is typically the case with Quill, you can use all of these features together.
```scala
ctx.run {
  q.returning(r => 
    (r.id + 100, udf(r.id), query[Supplier].filter(s => s.sku == r.sku).map(_.id).max)
  ) 
} // List[(Int, Int, Option[Long])]
// INSERT INTO Product (description,sku) VALUES ('My Product', 1011) 
// RETURNING id + 100, myUdf(id), (SELECT MAX(s.id) FROM Supplier s WHERE s.sku = sku)
```

> NOTE: Queries used inside of return clauses can only return a single row per insert.
Otherwise, Postgres will throw:
`ERROR: more than one row returned by a subquery used as an expression`. This is why is it strongly
recommended that you use aggregators such as `max` or `min`inside of quill returning-clause queries.
In the case that this is impossible (e.g. when using Postgres booleans), you can use the `.value` method: 
`q.returning(r => query[Supplier].filter(s => s.sku == r.sku).map(_.id).value)`.

##### SQL Server

The `returning` and `returningGenerated` methods are more restricted when using SQL Server; they only support 
arithmetic operations. These are inserted directly into the SQL `OUTPUT INSERTED.*` clause.

Assuming the query:
```scala
val q = quote {
  query[Product].insert(_.description -> "My Product", _.sku -> 1011L)
}
```

Add 100 to the value of `id`:
```scala
ctx.run(q.returning(r => id + 100)) //: List[Int]
// INSERT INTO Product (description, sku) OUTPUT INSERTED.id + 100 VALUES (?, ?)
```

### Embedded case classes

Quill supports nested `Embedded` case classes:

```scala
case class Contact(phone: String, address: String) extends Embedded
case class Person(id: Int, name: String, contact: Contact)

ctx.run(query[Person])
// SELECT x.id, x.name, x.phone, x.address FROM Person x
```

Note that default naming behavior uses the name of the nested case class properties. It's possible to override this default behavior using a custom `schema`:

```scala
case class Contact(phone: String, address: String) extends Embedded
case class Person(id: Int, name: String, homeContact: Contact, workContact: Option[Contact])

val q = quote {
  querySchema[Person](
    "Person",
    _.homeContact.phone          -> "homePhone",
    _.homeContact.address        -> "homeAddress",
    _.workContact.map(_.phone)   -> "workPhone",
    _.workContact.map(_.address) -> "workAddress"
  )
}

ctx.run(q)
// SELECT x.id, x.name, x.homePhone, x.homeAddress, x.workPhone, x.workAddress FROM Person x
```