---
id: Using_Quill_in_Modules
title: Using Quill in Modules
---

### Context Traits

One way to compose applications with this kind of context is to use traits with an abstract context variable:

```scala
class MyContext extends SqlMirrorContext(MirrorSqlDialect, Literal)

trait MySchema {

  val c: MyContext
  import c._

  val people = quote {
    querySchema[Person]("people")
  }
}

case class MyDao(c: MyContext) extends MySchema {
  import c._

  def allPeople = 
    c.run(people)
}
```

### Modular Contexts

Another simple way to modularize Quill code is by extending `Context` as a self-type and applying mixins. Using this strategy,
it is possible to create functionality that is fully portable across databases and even different types of databases
(e.g. creating common queries for both Postgres and Spark).

For example, create the following abstract context:

```scala
trait ModularContext[I <: Idiom, N <: NamingStrategy] { this: Context[I, N] =>
  def peopleOlderThan = quote {
    (age:Int, q:Query[Person]) => q.filter(p => p.age > age)
  }
}
```
 
Let's see how this can be used across different kinds of databases and Quill contexts.
 
#### Use `ModularContext` in a mirror context:

```scala
// Note: In some cases need to explicitly specify [MirrorSqlDialect, Literal].
val ctx = 
  new SqlMirrorContext[MirrorSqlDialect, Literal](MirrorSqlDialect, Literal) 
    with ModularContext[MirrorSqlDialect, Literal]
  
import ctx._ 
println( run(peopleOlderThan(22, query[Person])).string )
```

#### Use `ModularContext` to query a Postgres Database

```scala
val ctx = 
  new PostgresJdbcContext[Literal](Literal, ds) 
    with ModularContext[PostgresDialect, Literal]
  
import ctx._ 
val results = run(peopleOlderThan(22, query[Person]))
```

#### Use `ModularContext` to query a Spark Dataset

```scala
object CustomQuillSparkContext extends QuillSparkContext 
  with ModularContext[SparkDialect, Literal]
 
val results = run(peopleOlderThan(22, liftQuery(dataset)))
```
