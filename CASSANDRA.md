
This document compares Quill to the [Datastax Java](https://github.com/datastax/java-driver) driver and the [Phantom](http://websudos.github.io/phantom/) library. This is an incomplete comparison, additions and corrections are welcome.

All examples have been properly tested, and they should work out of the box.

## Prerequisites ##

The keyspace and column family needed for all examples are defined in this CQL script:

```
CREATE KEYSPACE IF NOT EXISTS db
WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE IF NOT EXISTS db.weather_station (
  country TEXT,
  city TEXT,
  station_id TEXT,
  entry INT,
  value INT,
  PRIMARY KEY (country, city, station_id)
);
```

In order to make all Quill examples to work you will need to create this config file, named `application.conf` in your resources folder:

```
db.keyspace=db
db.preparedStatementCacheSize=100
db.session.contactPoint=127.0.0.1
db.session.queryOptions.consistencyLevel=ONE
```

## Abstraction level ##

The Datastax Java driver provides simple abstractions that let you either write you queries as plain strings or to use a declarative Query Builder. It also provides a higher level [Object Mapper](https://github.com/datastax/java-driver/tree/2.1/manual/object_mapper). For this comparison we will only use the Query Builder.

Although both Quill and Phantom represent column family rows as flat immutable structures (case classes without nested data) and provide a type-safe composable query DSL, they work at a different abstraction level. 

Phantom provides an embedded DSL that help you write CQL queries in a type-safe manner. Quill is referred as a Language Integrated Query library to match the available publications on the subject. The paper ["Language-integrated query using comprehension syntax: state of the art, open problems, and work in progress"](http://research.microsoft.com/en-us/events/dcp2014/cheney.pdf) has an overview with some of the available implementations of language integrated queries.

## Simple query ##

This example would allow us to compare how the different libraries let us query a column family to obtain one element.

**Java Driver (v3.0.0)**
```scala
import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.google.common.cache.{ CacheBuilder, CacheLoader, LoadingCache }

import scala.collection.JavaConverters._

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

  case class WeatherStation(country: String, city: String, stationId: String, entry: Int, value: Int)

  object WeatherStation {

    def getAllByCountry(cache: LoadingCache[String, PreparedStatement], session: Session)(country: String): List[WeatherStation] = {
      val query: Statement =
        QueryBuilder.select().
          all().
          from("db", "weather_station").
          where(QueryBuilder.eq("country", QueryBuilder.bindMarker()))

      session.execute(cache.get(query.toString).bind(country)).
        all().asScala.
        map(
          row => WeatherStation(row.getString("country"), row.getString("city"), row.getString("station_id"), row.getInt("entry"), row.getInt("value"))
        ).
          to[List]
    }
  }

  val getAllByCountry: String => List[WeatherStation] = WeatherStation.getAllByCountry(cache, session)_

  getAllByCountry("UK")

  session.close()
  cluster.close()
}
```

The Java driver requires explicit handling of a `PreparedStatement`s cache to avoid preparing the same statement more that once, that could affect performance.

**Phantom (v1.22.0)**
```scala
import com.websudos.phantom.connectors.RootConnector
import com.websudos.phantom.db._
import com.websudos.phantom.dsl._

import scala.concurrent.Future

object Phantom extends App {

  case class WeatherStation(country: String, city: String, stationId: String, entry: Int, value: Int)

  abstract class WeatherStationCF(override val tableName: String) extends CassandraTable[WeatherStationCF, WeatherStation] with RootConnector {

    object country extends StringColumn(this) with PartitionKey[String]
    object city extends StringColumn(this) with PrimaryKey[String]
    object stationId extends StringColumn(this) with PrimaryKey[String] {
      override lazy val name: String = "station_id"
    }
    object entry extends IntColumn(this) with PrimaryKey[Int]
    object value extends IntColumn(this)

    override def fromRow(r: Row): WeatherStation =
      WeatherStation(country(r), city(r), stationId(r), entry(r), value(r))
  }

  abstract class WeatherStationQueries extends WeatherStationCF("weather_station") {

    def getAllByCountry(country: String): Future[List[WeatherStation]] =
      select.where(_.country eqs country).fetch()
  }

  class DB(ks: KeySpaceDef) extends DatabaseImpl(ks) {

    object stations extends WeatherStationQueries with connector.Connector
  }

  val db = new DB(ContactPoint.local.keySpace("db"))

  val result = db.stations.getAllByCountry("UK")

  result.onComplete { case _ => db.shutdown() }
}
```

Phantom requires explicit definition of the database model. The query definition also requires special equality operators.

**Quill (v0.3.2-SNAPSHOT)**
```scala
import io.getquill._
import io.getquill.naming._

import scala.concurrent.ExecutionContext.Implicits.global

object Quill extends App {

  val db = source(new CassandraAsyncSourceConfig[SnakeCase]("DB") with NoQueryProbing)

  case class WeatherStation(country: String, city: String, stationId: String, entry: Int, value: Int)

  object WeatherStation {

    val getAllByCountry = quote {
      (country: String) =>
        query[WeatherStation].filter(_.country == country)
    }
  }

  val result = db.run(WeatherStation.getAllByCountry)("UK").map(_.headOption)

  result.onComplete(_ => db.close())
}
```

During the compilation of this example, as the quotation is known statically, Quill will emit the CQL string as an info message, e.g. `SELECT country, city, station_id, entry, value FROM weather_station WHERE country = ?`.

## Composable queries ##

This example would allow us to compare how the different libraries let us compose querie, if possible.

**Java Driver (v3.0.0)** 

The Query Builder allows you to partially construct queries and add filters later:

```scala
    val selectAllFromPeople: Select =
      QueryBuilder.select().
      all().
      from("db", "people")
```

But that's a very limited composition capability.

**Phantom (v1.22.0)**
```scala
import com.websudos.phantom.connectors.RootConnector
import com.websudos.phantom.db._
import com.websudos.phantom.dsl._

import scala.concurrent.Future

object Phantom extends App {

  case class WeatherStation(country: String, city: String, stationId: String, entry: Int, value: Int)

  abstract class WeatherStationCF(override val tableName: String) extends CassandraTable[WeatherStationCF, WeatherStation] with RootConnector {

    object country extends StringColumn(this) with PartitionKey[String]
    object city extends StringColumn(this) with PrimaryKey[String]
    object stationId extends StringColumn(this) with PrimaryKey[String]
    object entry extends IntColumn(this) with PrimaryKey[Int]
    object value extends IntColumn(this)

    override def fromRow(r: Row): WeatherStation =
      WeatherStation(country(r), city(r), stationId(r), entry(r), value(r))
  }

  abstract class WeatherStationQueries extends WeatherStationCF("weather_station") {

    def getAllByCountry(country: String): Future[List[WeatherStation]] =
      findAllByCountry(country).fetch()

    def getAllByCountryAndCity(country: String, city: String): Future[List[WeatherStation]] =
      findAllByCountryAndCity(country, city).fetch()

    def getAllByCountryCityAndId(country: String, city: String, stationId: String): Future[List[WeatherStation]] =
      findAllByCountryCityAndId(country, city, stationId).fetch()

    private def findAllByCountry(country: String) =
      select.where(_.country eqs country)

    private def findAllByCountryAndCity(country: String, city: String) =
      findAllByCountry(country).and(_.city eqs city)

    private def findAllByCountryCityAndId(country: String, city: String, stationId: String) =
      findAllByCountryAndCity(country, city).and(_.stationId eqs stationId)
  }

  class DB(ks: KeySpaceDef) extends DatabaseImpl(ks) {

    object stations extends WeatherStationQueries with connector.Connector
  }

  val db = new DB(ContactPoint.local.keySpace("db"))

  val result = db.stations.getAllByCountryCityAndId("UK", "London", "SW27")

  result.onComplete { case _ => db.shutdown() }
}
```

Phantom allows you certain level of composability, but it gets a bit verbose due to the nature of the DSL. 

**Quill (v0.3.2-SNAPSHOT)**
```scala
import io.getquill._
import io.getquill.naming._

import scala.concurrent.ExecutionContext.Implicits.global

object Quill extends App {

  val db = source(new CassandraAsyncSourceConfig[SnakeCase]("DB") with NoQueryProbing)

  case class WeatherStation(country: String, city: String, stationId: String, entry: Int, value: Int)

  object WeatherStation {

    val getAllByCountry = quote {
      (country: String) =>
        query[WeatherStation].filter(_.country == country)
    }

    val getAllByCountryAndCity = quote {
      (country: String, city: String) =>
        getAllByCountry(country).filter(_.city == city)
    }

    val getAllByCountryCityAndId = quote {
      (country: String, city: String, stationId: String) =>
        getAllByCountryAndCity(country, city).filter(_.stationId == stationId)
    }
  }

  val result = db.run(WeatherStation.getAllByCountryCityAndId)("UK", "London", "SW27")

  result.onComplete(_ => db.close())
}
```

Quill offers more advanced composability, but CQL being a much simpler query language than SQL can't benefit much from it. During the compilation of this example, as the quotation is known statically, Quill will emit the CQL string as an info message, e.g. `SELECT country, city, station_id, entry, value FROM weather_station WHERE country = ? AND city = ? AND station_id = ?`.
