
This document compares Quill to the [Phantom](http://websudos.github.io/phantom/) library. This is an incomplete comparison, additions and corrections are welcome.

## Abstraction level ##

Although both Quill and Phantom represent column family rows as flat immutable structures (case classes without nested data) and provide a type-safe composable query DSL, they work at a different abstraction level. 

Phantom provides an embedded DSL that help you write CQL queries in a type-safe manner. Quill is referred as a Language Integrated Query library to match the available publications on the subject. The paper ["Language-integrated query using comprehension syntax: state of the art, open problems, and work in progress"](http://research.microsoft.com/en-us/events/dcp2014/cheney.pdf) has an overview with some of the available implementations of language integrated queries.

## QDSL versus EDSL ##

Quill's DSL is a macro-based quotation mechanism, allowing usage of Scala types and operators directly. Please refer to the paper ["Everything old is new again: Quoted Domain Specific Languages"](http://homepages.inf.ed.ac.uk/wadler/papers/qdsl/qdsl.pdf) for more details. On the other hand, Phantom provides a DSL to create queries, that requires the usage of DSL provided operations. Example:

**quill**
```scala
import io.getquill._
import io.getquill.naming._

import scala.concurrent.ExecutionContext.Implicits.global

val source = source(new CassandraAsyncSourceConfig[SnakeCase]("db") with NoQueryProbing)

case class Person(name: String, age: Int)

val getByName = quote {
  (n: String) =>
    query[Person].filter(_.name == name)
}

source.run(getByName)("Jane Doe")
```

**phantom**
```scala
import com.websudos.phantom.connectors._
import com.websudos.phantom.dsl._

import scala.concurrent.Future

val ks = ContactPoint.local.keySpace("db")

case class Person(name: String, age: Int)

sealed class PersonCF(override val tableName: String) extends CassandraTable[PersonCF, Person] with Connector {
  object name extends StringColumn(this) with PartitionKey[String]
  object age extends IntColumn(this)

  override def fromRow(r: Row): Person = Person(name(r), age(r))
}

object PersonCF extends PersonCF("person") extends ks.Connector {
  def getByName(name: String): Future[Option[Person]] =
    select.where(_.name eqs name).one()
}

PersonCF.getByname("Jane Doe")
```

Phantom requires explicit definition of the database model. The query definition also requires special equality operators.

## Compile-time versus Runtime ##

Quill's quoted DSL opens a new path to query generation. For the quotations that are known statically, the query normalization and translation to SQL happen at compile-time. The user receives feedback during compilation, knows the SQL string that will be used and, as an experimental feature disabled by default, if it will succeed when executed against the database.

Phantom creates the queries at runtime, but the nature of the DSL allows for some simple compile time validations, e.g. checking the absence of primary keys in the database model when querying.

## Non-blocking IO ##

Both Phantom and Quill are built on top of the Datastax java driver and provide tools to query Cassandra in an asynchronous and/or reactive, non-blocking manner.

Quill uses the [Monix.io](https://github.com/monixio/monix) library, compatible with the [reactive-streams](http://www.reactive-streams.org/) protocol, to provide an `Observable` of the result set. Phantom implements `Publisher/Subscriber`s on top of the [reactive-streams](http://www.reactive-streams.org/) specification.
