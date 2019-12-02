---
id: Queries
title: Queries
---

The overall abstraction of quill queries uses database tables as if they were in-memory collections. Scala for-comprehensions provide syntactic sugar to deal with these kinds of monadic operations:

```scala
case class Person(id: Int, name: String, age: Int)
case class Contact(personId: Int, phone: String)

val q = quote {
  for {
    p <- query[Person] if(p.id == 999)
    c <- query[Contact] if(c.personId == p.id)
  } yield {
    (p.name, c.phone)
  }
}

ctx.run(q)
// SELECT p.name, c.phone FROM Person p, Contact c WHERE (p.id = 999) AND (c.personId = p.id)
```

Quill normalizes the quotation and translates the monadic joins to applicative joins, generating a database-friendly query that avoids nested queries.

Any of the following features can be used together with the others and/or within a for-comprehension:

### filter
```scala
val q = quote {
  query[Person].filter(p => p.age > 18)
}

ctx.run(q)
// SELECT p.id, p.name, p.age FROM Person p WHERE p.age > 18
```

### map
```scala
val q = quote {
  query[Person].map(p => p.name)
}

ctx.run(q)
// SELECT p.name FROM Person p
```

### flatMap
```scala
val q = quote {
  query[Person].filter(p => p.age > 18).flatMap(p => query[Contact].filter(c => c.personId == p.id))
}

ctx.run(q)
// SELECT c.personId, c.phone FROM Person p, Contact c WHERE (p.age > 18) AND (c.personId = p.id)
```

### concatMap
```scala
// similar to `flatMap` but for transformations that return a traversable instead of `Query`

val q = quote {
  query[Person].concatMap(p => p.name.split(" "))
}

ctx.run(q)
// SELECT UNNEST(SPLIT(p.name, " ")) FROM Person p
```

### sortBy
```scala
val q1 = quote {
  query[Person].sortBy(p => p.age)
}

ctx.run(q1)
// SELECT p.id, p.name, p.age FROM Person p ORDER BY p.age ASC NULLS FIRST

val q2 = quote {
  query[Person].sortBy(p => p.age)(Ord.descNullsLast)
}

ctx.run(q2)
// SELECT p.id, p.name, p.age FROM Person p ORDER BY p.age DESC NULLS LAST

val q3 = quote {
  query[Person].sortBy(p => (p.name, p.age))(Ord(Ord.asc, Ord.desc))
}

ctx.run(q3)
// SELECT p.id, p.name, p.age FROM Person p ORDER BY p.name ASC, p.age DESC
```

### drop/take

```scala
val q = quote {
  query[Person].drop(2).take(1)
}

ctx.run(q)
// SELECT x.id, x.name, x.age FROM Person x LIMIT 1 OFFSET 2
```

### groupBy
```scala
val q = quote {
  query[Person].groupBy(p => p.age).map {
    case (age, people) =>
      (age, people.size)
  }
}

ctx.run(q)
// SELECT p.age, COUNT(*) FROM Person p GROUP BY p.age
```

### union
```scala
val q = quote {
  query[Person].filter(p => p.age > 18).union(query[Person].filter(p => p.age > 60))
}

ctx.run(q)
// SELECT x.id, x.name, x.age FROM (SELECT id, name, age FROM Person p WHERE p.age > 18
// UNION SELECT id, name, age FROM Person p1 WHERE p1.age > 60) x
```

### unionAll/++
```scala
val q = quote {
  query[Person].filter(p => p.age > 18).unionAll(query[Person].filter(p => p.age > 60))
}

ctx.run(q)
// SELECT x.id, x.name, x.age FROM (SELECT id, name, age FROM Person p WHERE p.age > 18
// UNION ALL SELECT id, name, age FROM Person p1 WHERE p1.age > 60) x

val q2 = quote {
  query[Person].filter(p => p.age > 18) ++ query[Person].filter(p => p.age > 60)
}

ctx.run(q2)
// SELECT x.id, x.name, x.age FROM (SELECT id, name, age FROM Person p WHERE p.age > 18
// UNION ALL SELECT id, name, age FROM Person p1 WHERE p1.age > 60) x
```

### aggregation
```scala
val r = quote {
  query[Person].map(p => p.age)
}

ctx.run(r.min) // SELECT MIN(p.age) FROM Person p
ctx.run(r.max) // SELECT MAX(p.age) FROM Person p
ctx.run(r.avg) // SELECT AVG(p.age) FROM Person p
ctx.run(r.sum) // SELECT SUM(p.age) FROM Person p
ctx.run(r.size) // SELECT COUNT(p.age) FROM Person p
```

### isEmpty/nonEmpty
```scala
val q = quote {
  query[Person].filter{ p1 =>
    query[Person].filter(p2 => p2.id != p1.id && p2.age == p1.age).isEmpty
  }
}

ctx.run(q)
// SELECT p1.id, p1.name, p1.age FROM Person p1 WHERE
// NOT EXISTS (SELECT * FROM Person p2 WHERE (p2.id <> p1.id) AND (p2.age = p1.age))

val q2 = quote {
  query[Person].filter{ p1 =>
    query[Person].filter(p2 => p2.id != p1.id && p2.age == p1.age).nonEmpty
  }
}

ctx.run(q2)
// SELECT p1.id, p1.name, p1.age FROM Person p1 WHERE
// EXISTS (SELECT * FROM Person p2 WHERE (p2.id <> p1.id) AND (p2.age = p1.age))
```

### contains
```scala
val q = quote {
  query[Person].filter(p => liftQuery(Set(1, 2)).contains(p.id))
}

ctx.run(q)
// SELECT p.id, p.name, p.age FROM Person p WHERE p.id IN (?, ?)

val q1 = quote { (ids: Query[Int]) =>
  query[Person].filter(p => ids.contains(p.id))
}

ctx.run(q1(liftQuery(List(1, 2))))
// SELECT p.id, p.name, p.age FROM Person p WHERE p.id IN (?, ?)

val peopleWithContacts = quote {
  query[Person].filter(p => query[Contact].filter(c => c.personId == p.id).nonEmpty)
}
val q2 = quote {
  query[Person].filter(p => peopleWithContacts.contains(p.id))
}

ctx.run(q2)
// SELECT p.id, p.name, p.age FROM Person p WHERE p.id IN (SELECT p1.* FROM Person p1 WHERE EXISTS (SELECT c.* FROM Contact c WHERE c.personId = p1.id))
```

### distinct
```scala
val q = quote {
  query[Person].map(p => p.age).distinct
}

ctx.run(q)
// SELECT DISTINCT p.age FROM Person p
```

### nested
```scala
val q = quote {
  query[Person].filter(p => p.name == "John").nested.map(p => p.age)
}

ctx.run(q)
// SELECT p.age FROM (SELECT p.age FROM Person p WHERE p.name = 'John') p
```

### joins
Joins are arguably the largest source of complexity in most SQL queries.
Quill offers a few different syntaxes so you can choose the right one for your use-case!

```scala
case class A(id: Int)
case class B(fk: Int)

// Applicative Joins:
quote {
  query[A].join(query[B]).on(_.id == _.fk)
}
 
// Implicit Joins:
quote {
  for {
    a <- query[A]
    b <- query[B] if (a.id == b.fk) 
  } yield (a, b)
}
 
// Flat Joins:
quote {
  for {
    a <- query[A]
    b <- query[B].join(_.fk == a.id)
  } yield (a, b)
}
```

Let's see them one by one assuming the following schema:
```scala
case class Person(id: Int, name: String)
case class Address(street: String, zip: Int, fk: Int)
```
(Note: If your use case involves lots and lots of joins, both inner and outer. Skip right to the flat-joins section!)

#### applicative joins

Applicative joins are useful for joining two tables together,
they are straightforward to understand, and typically look good on one line.
Quill supports inner, left-outer, right-outer, and full-outer (i.e. cross) applicative joins.

```scala
// Inner Join
val q = quote {
  query[Person].join(query[Address]).on(_.id == _.fk)
}
 
ctx.run(q) //: List[(Person, Address)]
// SELECT x1.id, x1.name, x2.street, x2.zip, x2.fk 
// FROM Person x1 INNER JOIN Address x2 ON x1.id = x2.fk
 
// Left (Outer) Join
val q = quote {
  query[Person].leftJoin(query[Address]).on((p, a) => p.id == a.fk)
}
 
ctx.run(q) //: List[(Person, Option[Address])]
// Note that when you use named-variables in your comprehension, Quill does its best to honor them in the query.
// SELECT p.id, p.name, a.street, a.zip, a.fk 
// FROM Person p LEFT JOIN Address a ON p.id = a.fk
 
// Right (Outer) Join
val q = quote {
  query[Person].rightJoin(query[Address]).on((p, a) => p.id == a.fk)
}
 
ctx.run(q) //: List[(Option[Person], Address)]
// SELECT p.id, p.name, a.street, a.zip, a.fk 
// FROM Person p RIGHT JOIN Address a ON p.id = a.fk
 
// Full (Outer) Join
val q = quote {
  query[Person].fullJoin(query[Address]).on((p, a) => p.id == a.fk)
}
 
ctx.run(q) //: List[(Option[Person], Option[Address])]
// SELECT p.id, p.name, a.street, a.zip, a.fk 
// FROM Person p FULL JOIN Address a ON p.id = a.fk
```
 
What about joining more than two tables with the applicative syntax?
Here's how to do that:
```scala
case class Company(zip: Int)

// All is well for two tables but for three or more, the nesting mess begins:
val q = quote {
  query[Person]
    .join(query[Address]).on({case (p, a) => p.id == a.fk}) // Let's use `case` here to stay consistent
    .join(query[Company]).on({case ((p, a), c) => a.zip == c.zip})
}
 
ctx.run(q) //: List[((Person, Address), Company)]
// (Unfortunately when you use `case` statements, Quill can't help you with the variables names either!)
// SELECT x01.id, x01.name, x11.street, x11.zip, x11.fk, x12.name, x12.zip 
// FROM Person x01 INNER JOIN Address x11 ON x01.id = x11.fk INNER JOIN Company x12 ON x11.zip = x12.zip
```
No worries though, implicit joins and flat joins have your other use-cases covered!

#### implicit joins

Quill's implicit joins use a monadic syntax making them pleasant to use for joining many tables together.
They look a lot like Scala collections when used in for-comprehensions
making them familiar to a typical Scala developer. 
What's the catch? They can only do inner-joins.

```scala
val q = quote {
  for {
    p <- query[Person]
    a <- query[Address] if (p.id == a.fk)
  } yield (p, a)
}
 
run(q) //: List[(Person, Address)]
// SELECT p.id, p.name, a.street, a.zip, a.fk 
// FROM Person p, Address a WHERE p.id = a.fk
```
 
Now, this is great because you can keep adding more and more joins
without having to do any pesky nesting.
```scala
val q = quote {
  for {
    p <- query[Person]
    a <- query[Address] if (p.id == a.fk)
    c <- query[Address] if (c.zip == a.zip)
  } yield (p, a, c)
}
 
run(q) //: List[(Person, Address, Company)]
// SELECT p.id, p.name, a.street, a.zip, a.fk, c.name, c.zip 
// FROM Person p, Address a, Company c WHERE p.id = a.fk AND c.zip = a.zip
```
Well that looks nice but wait! What If I need to inner, **and** outer join lots of tables nicely?
No worries, flat-joins are here to help!

### flat joins

Flat Joins give you the best of both worlds! In the monadic syntax, you can use both inner joins,
and left-outer joins together without any of that pesky nesting.

```scala
// Inner Join
val q = quote {
  for { 
    p <- query[Person]
    a <- query[Address].join(a => a.fk == p.id)
  } yield (p,a)
}
 
ctx.run(q) //: List[(Person, Address)]
// SELECT p.id, p.name, a.street, a.zip, a.fk
// FROM Person p INNER JOIN Address a ON a.fk = p.id
 
// Left (Outer) Join
val q = quote {
  for { 
    p <- query[Person]
    a <- query[Address].leftJoin(a => a.fk == p.id)
  } yield (p,a)
}
 
ctx.run(q) //: List[(Person, Option[Address])]
// SELECT p.id, p.name, a.street, a.zip, a.fk 
// FROM Person p LEFT JOIN Address a ON a.fk = p.id
```
 
Now you can keep adding both right and left joins without nesting!
```scala
val q = quote {
  for { 
    p <- query[Person]
    a <- query[Address].join(a => a.fk == p.id)
    c <- query[Company].leftJoin(c => c.zip == a.zip)
  } yield (p,a,c)
}
 
ctx.run(q) //: List[(Person, Address, Option[Company])]
// SELECT p.id, p.name, a.street, a.zip, a.fk, c.name, c.zip 
// FROM Person p 
// INNER JOIN Address a ON a.fk = p.id 
// LEFT JOIN Company c ON c.zip = a.zip
```

Can't figure out what kind of join you want to use? Who says you have to choose?

With Quill the following multi-join queries are equivalent, use them according to preference:

```scala

case class Employer(id: Int, personId: Int, name: String)

val qFlat = quote {
  for{
    (p,e) <- query[Person].join(query[Employer]).on(_.id == _.personId)
       c  <- query[Contact].leftJoin(_.personId == p.id)
  } yield(p, e, c)
}

val qNested = quote {
  for{
    ((p,e),c) <-
      query[Person].join(query[Employer]).on(_.id == _.personId)
      .leftJoin(query[Contact]).on(
        _._1.id == _.personId
      )
  } yield(p, e, c)
}

ctx.run(qFlat)
ctx.run(qNested)
// SELECT p.id, p.name, p.age, e.id, e.personId, e.name, c.id, c.phone
// FROM Person p INNER JOIN Employer e ON p.id = e.personId LEFT JOIN Contact c ON c.personId = p.id
```

Note that in some cases implicit and flat joins cannot be used together, for example, the following
query will fail.
```scala
val q = quote {
  for {
    p <- query[Person]
    p1 <- query[Person] if (p1.name == p.name)
    c <- query[Contact].leftJoin(_.personId == p.id)
  } yield (p, c)
}
 
// ctx.run(q)
// java.lang.IllegalArgumentException: requirement failed: Found an `ON` table reference of a table that is 
// not available: Set(p). The `ON` condition can only use tables defined through explicit joins.
```
This happens because an explicit join typically cannot be done after an implicit join in the same query.
 
A good guideline is in any query or subquery, choose one of the following:
 * Use flat-joins + applicative joins or
 * Use implicit joins
 
Also, note that not all Option operations are available on outer-joined tables (i.e. tables wrapped in an `Option` object),
only a specific subset. This is mostly due to the inherent limitations of SQL itself. For more information, see the
'Optional Tables' section.

### Optionals / Nullable Fields

> Note that the behavior of Optionals has recently changed to include stricter null-checks. See the [orNull / getOrNull](#ornull--getornull) section for more details.

Option objects are used to encode nullable fields.
Say you have the following schema:
```sql
CREATE TABLE Person(
  id INT NOT NULL PRIMARY KEY,
  name VARCHAR(255) -- This is nullable!
);
CREATE TABLE Address(
  fk INT, -- This is nullable!
  street VARCHAR(255) NOT NULL,
  zip INT NOT NULL,
  CONSTRAINT a_to_p FOREIGN KEY (fk) REFERENCES Person(id)
);
CREATE TABLE Company(
  name VARCHAR(255) NOT NULL,
  zip INT NOT NULL
)
```
This would encode to the following:
```scala
case class Person(id:Int, name:Option[String])
case class Address(fk:Option[Int], street:String, zip:Int)
case class Company(name:String, zip:Int)
```

Some important notes regarding Optionals and nullable fields.

> In many cases, Quill tries to rely on the null-fallthrough behavior that is ANSI standard:
>  * `null == null := false`
>  * `null == [true | false] := false`
> 
> This allows the generated SQL for most optional operations to be simple. For example, the expression
> `Option[String].map(v => v + "foo")` can be expressed as the SQL `v || 'foo'` as opposed to 
> `CASE IF (v is not null) v || 'foo' ELSE null END` so long as the concatenation operator `||`
> "falls-through" and returns `null` when the input is null. This is not true of all databases (e.g. [Oracle](https://community.oracle.com/ideas/19866)),
> forcing Quill to return the longer expression with explicit null-checking. Also, if there are conditionals inside
> of an Option operation (e.g. `o.map(v => if (v == "x") "y" else "z")`) this creates SQL with case statements,
> which will never fall-through when the input value is null. This forces Quill to explicitly null-check such statements in every
> SQL dialect.

Let's go through the typical operations of optionals.

#### isDefined / isEmpty

The `isDefined` method is generally a good way to null-check a nullable field:
```scala
val q = quote {
  query[Address].filter(a => a.fk.isDefined)
}
ctx.run(q)
// SELECT a.fk, a.street, a.zip FROM Address a WHERE a.fk IS NOT NULL
```
The `isEmpty` method works the same way:
```scala
val q = quote {
  query[Address].filter(a => a.fk.isEmpty)
}
ctx.run(q)
// SELECT a.fk, a.street, a.zip FROM Address a WHERE a.fk IS NULL
```

 
#### exists

This method is typically used for inspecting nullable fields inside of boolean conditions, most notably joining!
```scala
val q = quote {
  query[Person].join(query[Address]).on((p, a)=> a.fk.exists(_ == p.id))
}
ctx.run(q)
// SELECT p.id, p.name, a.fk, a.street, a.zip FROM Person p INNER JOIN Address a ON a.fk = p.id
```

Note that in the example above, the `exists` method does not cause the generated
SQL to do an explicit null-check in order to express the `False` case. This is because Quill relies on the
typical database behavior of immediately falsifying a statement that has `null` on one side of the equation.

#### forall

Use this method in boolean conditions that should succeed in the null case.
```scala
val q = quote {
  query[Person].join(query[Address]).on((p, a) => a.fk.forall(_ == p.id))
}
ctx.run(q)
// SELECT p.id, p.name, a.fk, a.street, a.zip FROM Person p INNER JOIN Address a ON a.fk IS NULL OR a.fk = p.id
```
Typically this is useful when doing negative conditions, e.g. when a field is **not** some specified value (e.g. `"Joe"`).
Being `null` in this case is typically a matching result.
```scala
val q = quote {
  query[Person].filter(p => p.name.forall(_ != "Joe"))
}
 
ctx.run(q)
// SELECT p.id, p.name FROM Person p WHERE p.name IS NULL OR p.name <> 'Joe'
```

#### map
As in regular Scala code, performing any operation on an optional value typically requires using the `map` function.
```scala
val q = quote {
 for {
    p <- query[Person]
  } yield (p.id, p.name.map("Dear " + _))
}
 
ctx.run(q)
// SELECT p.id, 'Dear ' || p.name FROM Person p
// * In Dialects where `||` does not fall-through for nulls (e.g. Oracle):
// * SELECT p.id, CASE WHEN p.name IS NOT NULL THEN 'Dear ' || p.name ELSE null END FROM Person p
```

Additionally, this method is useful when you want to get a non-optional field out of an outer-joined table
(i.e. a table wrapped in an `Option` object).

```scala
val q = quote {
  query[Company].leftJoin(query[Address])
    .on((c, a) => c.zip == a.zip)
    .map {case(c,a) =>                          // Row type is (Company, Option[Address])
      (c.name, a.map(_.street), a.map(_.zip))   // Use `Option.map` to get `street` and `zip` fields
    }
}
 
run(q)
// SELECT c.name, a.street, a.zip FROM Company c LEFT JOIN Address a ON c.zip = a.zip
```

For more details about this operation (and some caveats), see the 'Optional Tables' section.

#### flatMap and flatten

Use these when the `Option.map` functionality is not sufficient. This typically happens when you need to manipulate
multiple nullable fields in a way which would otherwise result in `Option[Option[T]]`.
```scala
val q = quote {
  for {
    a <- query[Person]
    b <- query[Person] if (a.id > b.id)
  } yield (
    // If this was `a.name.map`, resulting record type would be Option[Option[String]]
    a.name.flatMap(an =>
      b.name.map(bn => 
        an+" comes after "+bn)))
}
 
ctx.run(q) //: List[Option[String]]
// SELECT (a.name || ' comes after ') || b.name FROM Person a, Person b WHERE a.id > b.id
// * In Dialects where `||` does not fall-through for nulls (e.g. Oracle):
// * SELECT CASE WHEN a.name IS NOT NULL AND b.name IS NOT NULL THEN (a.name || ' comes after ') || b.name ELSE null END FROM Person a, Person b WHERE a.id > b.id
 
// Alternatively, you can use `flatten`
val q = quote {
  for {
    a <- query[Person]
    b <- query[Person] if (a.id > b.id)
  } yield (
    a.name.map(an => 
      b.name.map(bn => 
        an + " comes after " + bn)).flatten)
}
 
ctx.run(q) //: List[Option[String]]
// SELECT (a.name || ' comes after ') || b.name FROM Person a, Person b WHERE a.id > b.id
``` 
This is also very useful when selecting from outer-joined tables i.e. where the entire table
is inside of an `Option` object. Note how below we get the `fk` field from `Option[Address]`.

```scala
val q = quote {
  query[Person].leftJoin(query[Address])
    .on((p, a) => a.fk.exists(_ == p.id))
    .map {case (p /*Person*/, a /*Option[Address]*/) => (p.name, a.flatMap(_.fk))}
}
 
ctx.run(q) //: List[(Option[String], Option[Int])]
// SELECT p.name, a.fk FROM Person p LEFT JOIN Address a ON a.fk = p.id
```

#### orNull / getOrNull

The `orNull` method can be used to convert an Option-enclosed row back into a regular row.
Since `Option[T].orNull` does not work for primitive types (e.g. `Int`, `Double`, etc...),
you can use the `getOrNull` method inside of quoted blocks to do the same thing.

> Note that since the presence of null columns can cause queries to break in some data sources (e.g. Spark), so use this operation very carefully.

```scala
val q = quote {
  query[Person].join(query[Address])
    .on((p, a) => a.fk.exists(_ == p.id))
    .filter {case (p /*Person*/, a /*Option[Address]*/) => 
      a.fk.getOrNull != 123 } // Exclude a particular value from the query.
                              // Since we already did an inner-join on this value, we know it is not null.
}
 
ctx.run(q) //: List[(Address, Person)]
// SELECT p.id, p.name, a.fk, a.street, a.zip FROM Person p INNER JOIN Address a ON a.fk IS NOT NULL AND a.fk = p.id WHERE a.fk <> 123
```

In certain situations, you may wish to pretend that a nullable-field is not actually nullable and perform regular operations
(e.g. arithmetic, concatenation, etc...) on the field. You can use a combination of `Option.apply` and `orNull` (or `getOrNull` where needed)
in order to do this.

```scala
val q = quote {
  query[Person].map(p => Option(p.name.orNull + " suffix"))
}
 
ctx.run(q)
// SELECT p.name || ' suffix' FROM Person p 
// i.e. same as the previous behavior
```

In all other situations, since Quill strictly checks nullable values, and `case.. if` conditionals will work correctly in all Optional constructs.
However, since they may introduce behavior changes in your codebase, the following warning has been introduced:

> Conditionals inside of Option.[map | flatMap | exists | forall] will create a `CASE` statement in order to properly null-check the sub-query (...)

```
val q = quote {
  query[Person].map(p => p.name.map(n => if (n == "Joe") "foo" else "bar").getOrElse("baz"))
}
// Information:(16, 15) Conditionals inside of Option.map will create a `CASE` statement in order to properly null-check the sub-query: `p.name.map((n) => if(n == "Joe") "foo" else "bar")`. 
// Expressions like Option(if (v == "foo") else "bar").getOrElse("baz") will now work correctly, but expressions that relied on the broken behavior (where "bar" would be returned instead) need to be modified  (see the "orNull / getOrNull" section of the documentation of more detail).
 
ctx.run(a)
// Used to be this:
// SELECT CASE WHEN CASE WHEN p.name = 'Joe' THEN 'foo' ELSE 'bar' END IS NOT NULL THEN CASE WHEN p.name = 'Joe' THEN 'foo' ELSE 'bar' END ELSE 'baz' END FROM Person p
// Now is this:
// SELECT CASE WHEN p.name IS NOT NULL AND CASE WHEN p.name = 'Joe' THEN 'foo' ELSE 'bar' END IS NOT NULL THEN CASE WHEN p.name = 'Joe' THEN 'foo' ELSE 'bar' END ELSE 'baz' END FROM Person p
```

### equals

The `==`, `!=`, and `.equals` methods can be used to compare regular types as well Option types in a scala-idiomatic way.
That is to say, either `T == T` or `Option[T] == Option[T]` is supported and the following "truth-table" is observed:

Left         | Right        | Equality   | Result
-------------|--------------|------------|----------
`a`          | `b`          | `==`       | `a == b`
`Some[T](a)` | `Some[T](b)` | `==`       | `a == b`
`Some[T](a)` | `None`       | `==`       | `false`
`None      ` | `Some[T](b)` | `==`       | `false`
`None      ` | `None`       | `==`       | `true`
`Some[T]   ` | `Some[R]   ` | `==`       | Exception thrown.
`a`          | `b`          | `!=`       | `a != b`
`Some[T](a)` | `Some[T](b)` | `!=`       | `a != b`
`Some[T](a)` | `None`       | `!=`       | `true`
`None      ` | `Some[T](b)` | `!=`       | `true`
`Some[T]   ` | `Some[R]   ` | `!=`       | Exception thrown.
`None      ` | `None`       | `!=`       | `false`

```scala
case class Node(id:Int, status:Option[String], otherStatus:Option[String])

val q = quote { query[Node].filter(n => n.id == 123) }
ctx.run(q)
// SELECT n.id, n.status, n.otherStatus FROM Node n WHERE p.id = 123

val q = quote { query[Node].filter(r => r.status == r.otherStatus) }
ctx.run(q)
// SELECT r.id, r.status, r.otherStatus FROM Node r WHERE r.status IS NULL AND r.otherStatus IS NULL OR r.status = r.otherStatus

val q = quote { query[Node].filter(n => n.status == Option("RUNNING")) }
ctx.run(q)
// SELECT n.id, n.status, n.otherStatus FROM node n WHERE n.status IS NOT NULL AND n.status = 'RUNNING'

val q = quote { query[Node].filter(n => n.status != Option("RUNNING")) }
ctx.run(q)
// SELECT n.id, n.status, n.otherStatus FROM node n WHERE n.status IS NULL OR n.status <> 'RUNNING'
```

If you would like to use an equality operator that follows that ansi-idiomatic approach, failing
the comparison if either side is null as well as the principle that `null = null := false`, you can import `===` (and `=!=`) 
from `Context.extras`. These operators work across `T` and `Option[T]` allowing comparisons like `T === Option[T]`,
`Option[T] == T` etc... to be made. You can use also `===`
directly in Scala code and it will have the same behavior, returning `false` when other the left-hand
or right-hand side is `None`. This is particularity useful in paradigms like Spark where
you will typically transition inside and outside of Quill code.

> When using `a === b` or `a =!= b` sometimes you will see the extra `a IS NOT NULL AND b IS NOT NULL` comparisons
> and sometimes you will not. This depends on `equalityBehavior` in `SqlIdiom` which determines whether the given SQL
> dialect already does ansi-idiomatic comparison to `a`, and `b` when an `=` operator is used,
> this allows us to omit the extra `a IS NOT NULL AND b IS NOT NULL`.


```scala
import ctx.extras._

// === works the same way inside of a quotation
val q = run( query[Node].filter(n => n.status === "RUNNING") )
// SELECT n.id, n.status FROM node n WHERE n.status IS NOT NULL AND n.status = 'RUNNING'

// as well as outside
(nodes:List[Node]).filter(n => n.status === "RUNNING")
```

#### Optional Tables

As we have seen in the examples above, only the `map` and `flatMap` methods are available on outer-joined tables
(i.e. tables wrapped in an `Option` object).
 
Since you cannot use `Option[Table].isDefined`, if you want to null-check a whole table
(e.g. if a left-join was not matched), you have to `map` to a specific field on which you can do the null-check.

```scala
val q = quote {
  query[Company].leftJoin(query[Address])
    .on((c, a) => c.zip == a.zip)         // Row type is (Company, Option[Address])
    .filter({case(c,a) => a.isDefined})   // You cannot null-check a whole table
}
```
 
Instead, map the row-variable to a specific field and then check that field.
```scala
val q = quote {
  query[Company].leftJoin(query[Address])
    .on((c, a) => c.zip == a.zip)                     // Row type is (Company, Option[Address])
    .filter({case(c,a) => a.map(_.street).isDefined}) // Null-check a non-nullable field instead
}
ctx.run(q)
// SELECT c.name, c.zip, a.fk, a.street, a.zip 
// FROM Company c 
// LEFT JOIN Address a ON c.zip = a.zip 
// WHERE a.street IS NOT NULL
```
 
Finally, it is worth noting that a whole table can be wrapped into an `Option` object. This is particularly
useful when doing a union on table-sets that are both right-joined and left-joined together.
```scala
val aCompanies = quote {
  for {
    c <- query[Company] if (c.name like "A%")
    a <- query[Address].join(_.zip == c.zip)
  } yield (c, Option(a))  // change (Company, Address) to (Company, Option[Address]) 
}
val bCompanies = quote {
  for {
    c <- query[Company] if (c.name like "A%")
    a <- query[Address].leftJoin(_.zip == c.zip)
  } yield (c, a) // (Company, Option[Address])
}
val union = quote {
  aCompanies union bCompanies
}
ctx.run(union)
// SELECT x.name, x.zip, x.fk, x.street, x.zip FROM (
// (SELECT c.name name, c.zip zip, x1.zip zip, x1.fk fk, x1.street street 
// FROM Company c INNER JOIN Address x1 ON x1.zip = c.zip WHERE c.name like 'A%') 
// UNION 
// (SELECT c1.name name, c1.zip zip, x2.zip zip, x2.fk fk, x2.street street 
// FROM Company c1 LEFT JOIN Address x2 ON x2.zip = c1.zip WHERE c1.name like 'A%')
// ) x
```

### Ad-Hoc Case Classes

Case Classes can also be used inside quotations as output values:

```scala
case class Person(id: Int, name: String, age: Int)
case class Contact(personId: Int, phone: String)
case class ReachablePerson(name:String, phone: String)

val q = quote {
  for {
    p <- query[Person] if(p.id == 999)
    c <- query[Contact] if(c.personId == p.id)
  } yield {
    ReachablePerson(p.name, c.phone)
  }
}

ctx.run(q)
// SELECT p.name, c.phone FROM Person p, Contact c WHERE (p.id = 999) AND (c.personId = p.id)
```

As well as in general:

```scala
case class IdFilter(id:Int)

val q = quote {
  val idFilter = new IdFilter(999)
  for {
    p <- query[Person] if(p.id == idFilter.id)
    c <- query[Contact] if(c.personId == p.id)
  } yield {
    ReachablePerson(p.name, c.phone)
  }
}

ctx.run(q)
// SELECT p.name, c.phone FROM Person p, Contact c WHERE (p.id = 999) AND (c.personId = p.id)
```
***Note*** however that this functionality has the following restrictions:
1. The Ad-Hoc Case Class can only have one constructor with one set of parameters.
2. The Ad-Hoc Case Class must be constructed inside the quotation using one of the following methods:
    1. Using the `new` keyword: `new Person("Joe", "Bloggs")`
    2. Using a companion object's apply method:  `Person("Joe", "Bloggs")`
    3. Using a companion object's apply method explicitly: `Person.apply("Joe", "Bloggs")`
4. Any custom logic in a constructor/apply-method of an Ad-Hoc case class will not be invoked when it is 'constructed' inside a quotation. To construct an Ad-Hoc case class with custom logic inside a quotation, you can use a quoted method.

## Query probing

Query probing validates queries against the database at compile time, failing the compilation if it is not valid. The query validation does not alter the database state.

This feature is disabled by default. To enable it, mix the `QueryProbing` trait to the database configuration:

```
object myContext extends YourContextType with QueryProbing
```

The context must be created in a separate compilation unit in order to be loaded at compile time. Please use [this guide](http://www.scala-sbt.org/0.13/docs/Macro-Projects.html) that explains how to create a separate compilation unit for macros, that also serves to the purpose of defining a query-probing-capable context. `context` could be used instead of `macros` as the name of the separate compilation unit.

The configurations correspondent to the config key must be available at compile time. You can achieve it by adding this line to your project settings:

```
unmanagedClasspath in Compile += baseDirectory.value / "src" / "main" / "resources"
```

If your project doesn't have a standard layout, e.g. a play project, you should configure the path to point to the folder that contains your config file.

## Actions

Database actions are defined using quotations as well. These actions don't have a collection-like API but rather a custom DSL to express inserts, deletes, and updates.

### insert

```scala
val a = quote(query[Contact].insert(lift(Contact(999, "+1510488988"))))

ctx.run(a)
// INSERT INTO Contact (personId,phone) VALUES (?, ?)
```

#### It is also possible to insert specific columns:

```scala
val a = quote {
  query[Contact].insert(_.personId -> lift(999), _.phone -> lift("+1510488988"))
}

ctx.run(a)
// INSERT INTO Contact (personId,phone) VALUES (?, ?)
```

### batch insert
```scala
val a = quote {
  liftQuery(List(Person(0, "John", 31))).foreach(e => query[Person].insert(e))
}

ctx.run(a)
// INSERT INTO Person (id,name,age) VALUES (?, ?, ?)
```

### update
```scala
val a = quote {
  query[Person].filter(_.id == 999).update(lift(Person(999, "John", 22)))
}

ctx.run(a)
// UPDATE Person SET id = ?, name = ?, age = ? WHERE id = 999
```

#### Using specific columns:

```scala
val a = quote {
  query[Person].filter(p => p.id == lift(999)).update(_.age -> lift(18))
}

ctx.run(a)
// UPDATE Person SET age = ? WHERE id = ?
```

#### Using columns as part of the update:

```scala
val a = quote {
  query[Person].filter(p => p.id == lift(999)).update(p => p.age -> (p.age + 1))
}

ctx.run(a)
// UPDATE Person SET age = (age + 1) WHERE id = ?
```

### batch update

```scala
val a = quote {
  liftQuery(List(Person(1, "name", 31))).foreach { person =>
     query[Person].filter(_.id == person.id).update(_.name -> person.name, _.age -> person.age)
  }
}

ctx.run(a)
// UPDATE Person SET name = ?, age = ? WHERE id = ?
```

### delete
```scala
val a = quote {
  query[Person].filter(p => p.name == "").delete
}

ctx.run(a)
// DELETE FROM Person WHERE name = ''
```

### insert or update (upsert, conflict)

Upsert is supported by Postgres, SQLite, and MySQL

## Printing Queries

The `translate` method is used to convert a Quill query into a string which can then be printed.

```scala
val str = ctx.translate(query[Person])
println(str)
// SELECT x.id, x.name, x.age FROM Person x
```

Insert queries can also be printed:

```scala
val str = ctx.translate(query[Person].insert(lift(Person(0, "Joe", 45))))
println(str)
// INSERT INTO Person (id,name,age) VALUES (0, 'Joe', 45)
```

As well as batch insertions:

```scala
val q = quote {
  liftQuery(List(Person(0, "Joe",44), Person(1, "Jack",45)))
    .foreach(e => query[Person].insert(e))
}
val strs: List[String] = ctx.translate(q)
strs.map(println)
// INSERT INTO Person (id, name,age) VALUES (0, 'Joe', 44)
// INSERT INTO Person (id, name,age) VALUES (1, 'Jack', 45)
```

The `translate` method is available in every Quill context as well as the Cassandra and OrientDB contexts,
the latter two, however, do not support Insert and Batch Insert query printing.

### Getting a ResultSet

Quill JDBC Contexts allow you to use `prepare` in order to get a low-level `ResultSet` that is useful
for interacting with legacy APIs. This function  returns a `f: (Connection) => (PreparedStatement)` 
closure as opposed to a `PreparedStatement` in order to guarantee that JDBC Exceptions are not
thrown until you can wrap them into the appropriate Exception-handling mechanism (e.g.
`try`/`catch`, `Try` etc...).

```scala
val q = quote {
  query[Product].filter(_.id == 1)
}
val preparer: (Connection) => (PreparedStatement)  = ctx.prepare(q)
// SELECT x1.id, x1.description, x1.sku FROM Product x1 WHERE x1.id = 1

// Use ugly stateful code, bracketed effects, or try-with-resources here:
var preparedStatement: PreparedStatement = _
var resultSet: ResultSet = _

try {
  preparedStatement = preparer(myCustomDataSource.getConnection)
  resultSet = preparedStatement.executeQuery()
} catch {
  case e: Exception =>
    // Close the preparedStatement and catch possible exceptions
    // Close the resultSet and catch possible exceptions
}
```

The `prepare` function can also be used with `insert`, and `update` queries.

```scala
val q = quote {
  query[Product].insert(lift(Product(1, "Desc", 123))
}
val preparer: (Connection) => (PreparedStatement)  = ctx.prepare(q)
// INSERT INTO Product (id,description,sku) VALUES (?, ?, ?)
```

As well as with batch queries.
> Make sure to first quote your batch query and then pass the result into the `prepare` function
(as is done in the example below) or the Scala compiler may not type the output correctly
[#1518](https://github.com/getquill/quill/issues/1518).

```scala
val q = quote {
  liftQuery(products).foreach(e => query[Product].insert(e))
}
val preparers: Connection => List[PreparedStatement] = ctx.prepare(q)
val preparedStatement: List[PreparedStatement] = preparers(jdbcConf.dataSource.getConnection)
```



## Implicit query

Quill provides implicit conversions from case class companion objects to `query[T]` through an additional trait:

```scala
val ctx = new SqlMirrorContext(MirrorSqlDialect, Literal) with ImplicitQuery

import ctx._

val q = quote {
  for {
    p <- Person if(p.id == 999)
    c <- Contact if(c.personId == p.id)
  } yield {
    (p.name, c.phone)
  }
}

ctx.run(q)
// SELECT p.name, c.phone FROM Person p, Contact c WHERE (p.id = 999) AND (c.personId = p.id)
```

Note the usage of `Person` and `Contact` instead of `query[Person]` and `query[Contact]`.

## SQL-specific operations

Some operations are SQL-specific and not provided with the generic quotation mechanism. The SQL contexts provide implicit classes for this kind of operation:

```scala
val ctx = new SqlMirrorContext(MirrorSqlDialect, Literal)
import ctx._
```

### like

```scala
val q = quote {
  query[Person].filter(p => p.name like "%John%")
}
ctx.run(q)
// SELECT p.id, p.name, p.age FROM Person p WHERE p.name like '%John%'
```

## SQL-specific encoding

### Arrays

Quill provides SQL Arrays support. In Scala we represent them as any collection that implements `Seq`:
```scala
import java.util.Date

case class Book(id: Int, notes: List[String], pages: Vector[Int], history: Seq[Date])

ctx.run(query[Book])
// SELECT x.id, x.notes, x.pages, x.history FROM Book x
```
Note that not all drivers/databases provides such feature hence only `PostgresJdbcContext` and
`PostgresAsyncContext` support SQL Arrays.

## Dynamic queries

Quill's default operation mode is compile-time, but there are queries that have their structure defined only at runtime. Quill automatically falls back to runtime normalization and query generation if the query's structure is not static. Example:

```scala
val ctx = new SqlMirrorContext(MirrorSqlDialect, Literal)

import ctx._

sealed trait QueryType
case object Minor extends QueryType
case object Senior extends QueryType

def people(t: QueryType): Quoted[Query[Person]] =
  t match {
    case Minor => quote {
      query[Person].filter(p => p.age < 18)
    }
    case Senior => quote {
      query[Person].filter(p => p.age > 65)
    }
  }

ctx.run(people(Minor))
// SELECT p.id, p.name, p.age FROM Person p WHERE p.age < 18

ctx.run(people(Senior))
// SELECT p.id, p.name, p.age FROM Person p WHERE p.age > 65
```

### Dynamic query API

Additionally, Quill provides a separate query API to facilitate the creation of dynamic queries. This API allows users to easily manipulate quoted values instead of working only with quoted transformations. 

**Important**: A few of the dynamic query methods accept runtime string values. It's important to keep in mind that these methods could be a vector for SQL injection.

Let's use the `filter` transformation as an example. In the regular API, this method has no implementation since it's an abstract member of a trait:

```
def filter(f: T => Boolean): EntityQuery[T]
```

In the dynamic API, `filter` is has a different signature and a body that is executed at runtime:

```
def filter(f: Quoted[T] => Quoted[Boolean]): DynamicQuery[T] =
  transform(f, Filter)
```

It takes a `Quoted[T]` as input and produces a `Quoted[Boolean]`. The user is free to use regular scala code within the transformation:

```scala
def people(onlyMinors: Boolean) =
  dynamicQuery[Person].filter(p => if(onlyMinors) quote(p.age < 18) else quote(true))
```

In order to create a dynamic query, use one of the following methods:

```scala
dynamicQuery[Person]
dynamicQuerySchema[Person]("people", alias(_.name, "pname"))
```

It's also possible to transform a `Quoted` into a dynamic query:

```scala
val q = quote {
  query[Person]
}
q.dynamic.filter(p => quote(p.name == "John"))
```

The dynamic query API is very similar to the regular API but has a few differences:

**Queries**
```scala
// schema queries use `alias` instead of tuples
dynamicQuerySchema[Person]("people", alias(_.name, "pname"))

// this allows users to use a dynamic list of aliases
val aliases = List(alias[Person](_.name, "pname"), alias[Person](_.age, "page"))
dynamicQuerySchema[Person]("people", aliases:_*)

// a few methods have an overload with the `Opt` suffix,
// which apply the transformation only if the option is defined:

def people(minAge: Option[Int]) =
  dynamicQuery[Person].filterOpt(minAge)((person, minAge) => quote(person.age >= minAge))

def people(maxRecords: Option[Int]) =
  dynamicQuery[Person].takeOpt(maxRecords)

def people(dropFirst: Option[Int]) =
  dynamicQuery[Person].dropOpt(dropFirst)
  
// method with `If` suffix, for better chaining  
def people(userIds: Seq[Int]) =
  dynamicQuery[Person].filterIf(userIds.nonEmpty)(person => quote(liftQuery(userIds).contains(person.id)))
```

**Actions**
```scala
// actions use `set` 
dynamicQuery[Person].filter(_.id == 1).update(set(_.name, quote("John")))

// or `setValue` if the value is not quoted
dynamicQuery[Person].insert(setValue(_.name, "John"))

// or `setOpt` that will be applied only the option is defined
dynamicQuery[Person].insert(setOpt(_.name, Some("John")))

// it's also possible to use a runtime string value as the column name
dynamicQuery[Person].filter(_.id == 1).update(set("name", quote("John")))

// to insert or update a case class instance, use `insertValue`/`updateValue`
val p = Person(0, "John", 21)
dynamicQuery[Person].insertValue(p)
dynamicQuery[Person].filter(_.id == 1).updateValue(p)
```

Contexts represent the database and provide an execution interface for queries.

## Mirror context

Quill provides a mirror context for testing purposes. Instead of running the query, the mirror context returns a structure with the information that would be used to run the query. There are three mirror context instances:

- `io.getquill.MirrorContext`: Mirrors the quotation AST
- `io.getquill.SqlMirrorContext`: Mirrors the SQL query
- `io.getquill.CassandraMirrorContext`: Mirrors the CQL query

## Dependent contexts

The context instance provides all methods and types to interact with quotations and the database. Depending on how the context import happens, Scala won't be able to infer that the types are compatible.

For instance, this example **will not** compile:

```
class MyContext extends SqlMirrorContext(MirrorSqlDialect, Literal)

case class MySchema(c: MyContext) {

  import c._
  val people = quote {
    querySchema[Person]("people")
  }
}

case class MyDao(c: MyContext, schema: MySchema) {

  def allPeople = 
    c.run(schema.people)
// ERROR: [T](quoted: MyDao.this.c.Quoted[MyDao.this.c.Query[T]])MyDao.this.c.QueryResult[T]
 cannot be applied to (MyDao.this.schema.c.Quoted[MyDao.this.schema.c.EntityQuery[Person]]{def quoted: io.getquill.ast.ConfiguredEntity; def ast: io.getquill.ast.ConfiguredEntity; def id1854281249(): Unit; val bindings: Object})
}
```





TODO This goes into the front-page section, finish separing out
 
## External content

### Talks

ScalaDays Berlin 2016 - [Scylla, Charybdis, and the mystery of Quill](https://www.youtube.com/watch?v=nqSYccoSeio)

### Blog posts

[quill-spark: A type-safe Scala API for Spark SQL](https://medium.com/@fwbrasil/quill-spark-a-type-safe-scala-api-for-spark-sql-2672e8582b0d)
Scalac.io blog - [Compile-time Queries with Quill](http://blog.scalac.io/2016/07/21/compile-time-queries-with-quill.html)

## Code of Conduct

Please note that this project is released with a Contributor Code of Conduct. By participating in this project you agree to abide by its terms. See [CODE_OF_CONDUCT.md](https://github.com/getquill/quill/blob/master/CODE_OF_CONDUCT.md) for details.

## License

See the [LICENSE](https://github.com/getquill/quill/blob/master/LICENSE.txt) file for details.

# Maintainers

- @deusaquilus (lead maintainer)
- @fwbrasil (creator)
- @jilen
- @juliano
- @mentegy
- @mdedetrich
- @mxl

## Former maintainers:

- @gustavoamigo
- @godenji
- @lvicentesanchez

You can notify all current maintainers using the handle `@getquill/maintainers`.

# Acknowledgments

The project was created having Philip Wadler's talk ["A practical theory of language-integrated query"](http://www.infoq.com/presentations/theory-language-integrated-query) as its initial inspiration. The development was heavily influenced by the following papers:

* [A Practical Theory of Language-Integrated Query](http://homepages.inf.ed.ac.uk/slindley/papers/practical-theory-of-linq.pdf)
* [Everything old is new again: Quoted Domain Specific Languages](http://homepages.inf.ed.ac.uk/wadler/papers/qdsl/qdsl.pdf)
* [The Flatter, the Better](http://db.inf.uni-tuebingen.de/staticfiles/publications/the-flatter-the-better.pdf)
