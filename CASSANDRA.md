
This document compares Quill to the [Datastax Java](https://github.com/datastax/java-driver) driver and the [Phantom](http://websudos.github.io/phantom/) library. This is an incomplete comparison, additions and corrections are welcome.

## Abstraction level ##

The Datastax Java driver provides simple abstractions that let you either write you queries as plain strings or to use a declarative Query Builder. It also provides a higher level [Object Mapper](https://github.com/datastax/java-driver/tree/2.1/manual/object_mapper). For this comparison we will only use the Query Builder.

Although both Quill and Phantom represent column family rows as flat immutable structures (case classes without nested data) and provide a type-safe composable query DSL, they work at a different abstraction level. 

Phantom provides an embedded DSL that help you write CQL queries in a type-safe manner. Quill is referred as a Language Integrated Query library to match the available publications on the subject. The paper ["Language-integrated query using comprehension syntax: state of the art, open problems, and work in progress"](http://research.microsoft.com/en-us/events/dcp2014/cheney.pdf) has an overview with some of the available implementations of language integrated queries.

## A simple query ##

This example would allow us to compare how the different libraries let us query a column family to obtain one element. The keyspace and column family needed for this and other examples are defined in this CQL script:

```
CREATE KEYSPACE IF NOT EXISTS db
WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE IF NOT EXISTS db.people (
  name TEXT,
  age INT,
  PRIMARY KEY (name)
);
```

All examples have been properly tested, and they should work out of the box.

**Java Driver**
```scala
import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.google.common.cache.{ CacheBuilder, CacheLoader, LoadingCache }

object JavaDriver extends App {

  val nrOfCacheEntries: Int = 100

  val cluster: Cluster = Cluster.builder().addContactPoints("localhost").build()

  val session: Session = cluster.newSession()

  val cache: LoadingCache[String, PreparedStatement] =
    CacheBuilder.newBuilder().
      maximumSize(nrOfCacheEntries).
      build(
        new CacheLoader[String, PreparedStatement]() {
          def load(key: String): PreparedStatement = session.prepare(key.toString)
        }
      )

  case class People(name: String, age: Int)

  object People {

    def getByName(cache: LoadingCache[String, PreparedStatement], session: Session)(name: String): Option[People] = {
      val query: Statement =
        QueryBuilder.select().
          all().
          from("db", "people").
          where(QueryBuilder.eq("name", QueryBuilder.bindMarker()))

      Option(session.execute(cache.get(query.toString).bind(name)).one()).map(
        row => Person(row.getString("name"), row.getInt("age"))
      )
    }
  }

  val getPersonByName: String => Option[People] = Person.getByName(cache, session)_

  getPersonByName("Jane Doe")

  session.close()
  cluster.close()
}
```

The Java driver requires explicit handling of a `PreparedStatement`s cache to avoid preparing the same statement more that once, that could affect performance.

In order to make the Quill example to work you will need to create this config file, named `application.conf` in your resources folder:

```
db.keyspace=db
db.preparedStatementCacheSize=100
db.session.contactPoint=127.0.0.1
db.session.queryOptions.consistencyLevel=ONE
```

**Quill**
```scala
import io.getquill._
import io.getquill.naming._

import scala.concurrent.ExecutionContext.Implicits.global

object Quill extends App {

  val db = source(new CassandraAsyncSourceConfig[SnakeCase]("DB") with NoQueryProbing)

  case class People(name: String, age: Int)

  object People {
  
    val getByName = quote {
      (name: String) =>
        query[People].filter(_.name == name)
    }
  }

  val result = db.run(People.getByName)("Jane Doe").map(_.headOption)

  result.onComplete(_ => db.close())
}
```

During the compilation of this example, as the quotation is known statically, Quill will emit the CQL string as an info message, e.g. `SELECT name, age FROM person WHERE name = ?`.

**Phantom**
```scala
import com.websudos.phantom.connectors.{ Connector, RootConnector }
import com.websudos.phantom.db._
import com.websudos.phantom.dsl._

import scala.concurrent._

object Phantom extends App {

  case class People(name: String, age: Int)

  abstract class PeopleCF(override val tableName: String) extends CassandraTable[PeopleCF, People] with RootConnector {
  
    object name extends StringColumn(this) with PartitionKey[String]
    object age extends IntColumn(this)

    override def fromRow(r: Row): People = People(name(r), age(r))
  }

  abstract class PeopleQueries extends PeopleCF("people") {
  
    def getByName(name: String): Future[Option[People]] =
      select.where(_.name eqs name).one()
  }

  class DB(ks: KeySpaceDef) extends DatabaseImpl(ks) {
  
    object people extends PeopleQueries with connector.Connector
  }

  val db = new DB(ContactPoint.local.keySpace("db"))

  val result = db.people.getByName("Jane Doe")

  result.onComplete { case _ => db.shutdown() }
}
```

Phantom requires explicit definition of the database model. The query definition also requires special equality operators.
