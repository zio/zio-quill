---
id: Spark
title: Spark
---

Quill provides a fully type-safe way to use Spark's highly-optimized SQL engine. It's an alternative to `Dataset`'s semi-strongly-typed API.

## Impoting Quill Spark
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-spark" % "3.5.1-SNAPSHOT"
)
```

## Setting Up the Context

```scala
import org.apache.spark.sql.SparkSession

// Create your Spark Context
val session =
  SparkSession.builder()
    .master("local")
    .appName("spark test")
    .getOrCreate()

// The Spark SQL Context must be provided by the user through an implicit value:
implicit val sqlContext = session
import sqlContext.implicits._      // Also needed...

// Import the Quill Spark Context
import io.getquill.QuillSparkContext._
```

> Note Unlike the other modules, the Spark context is a companion object. Also, it does not depend on a spark session.

> Also Note: Quill decoders and meta instances are not used by the quill-spark module, Spark's `Encoder`s are used instead.

## Using Quill-Spark

The `run` method returns a `Dataset` transformed by the Quill query using the SQL engine.
```scala
// Typically you start with some type dataset.
val peopleDS: Dataset[Person] = spark.read.parquet("path/to/people")
val addressesDS: Dataset[Address] = spark.read.parquet("path/to/addresses")

// The liftQuery method converts Datasets to Quill queries:
val people: Query[Person] = quote { liftQuery(peopleDS) }
val addresses: Query[Address] = quote { liftQuery(addressesDS) }

val people: Query[(Person] = quote {
  people.join(addresses).on((p, a) => p.id == a.ownerFk)
}

val peopleAndAddressesDS: Dataset[(Person, Address)] = run(people)
```

### Simplify it
Since the `run` method allows for Quill queries to be specified directly, and `liftQuery` can be used inside
of any Quoted block, you can shorten various steps of the above workflow:

```scala
val peopleDS: Dataset[Person] = spark.read.parquet("path/to/people")
val addressesDS: Dataset[Address] = spark.read.parquet("path/to/addresses")

val peopleAndAddressesDS: Dataset[(Person, Address)] = run {
  liftQuery(peopleDS)
    .join(liftQuery(addressesDS))
    .on((p, a) => p.id == a.ownerFk)
}
```

Here is an example of a Dataset being converted into Quill, filtered, and then written back out.

```scala
import org.apache.spark.sql.Dataset

def filter(myDataset: Dataset[Person], name: String): Dataset[Int] =
  run {
    liftQuery(myDataset).filter(_.name == lift(name)).map(_.age)
  }
// SELECT x1.age _1 FROM (?) x1 WHERE x1.name = ?
```

### Workflow

Due to the design of Quill-Spark, it can be used interchangeably throughout your Spark workflow:
 - Lift a Dataset to Query to do some filtering and sub-selecting
(with [Predicate and Filter Pushdown!](https://jaceklaskowski.gitbooks.io/mastering-spark-sql/spark-sql-Optimizer-PushDownPredicate.html)).
 - Then covert it back to a Dataset to do Spark-Specific operations.
 - Then convert it back to a Query to use Quills great Join DSL...
 - Then convert it back to a Datate to write it to a file or do something else with it...

TODO Full query example of this workflow

## Custom Functions

TODO UDFs and UDAFs

## Restrictions

### Top Level Classes
Spark only supports using top-level classes as record types. That means that
when using `quill-spark` you can only use a top-level case class for `T` in `Query[T]`.

TODO Get the specific error

### Lifted Variable Interpolation

The queries printed from `run(myQuery)` during compile time escape question marks via a backslash them in order to
be able to substitute liftings properly. They are then returned back to their original form before running.
```scala
import org.apache.spark.sql.Dataset

def filter(myDataset: Dataset[Person]): Dataset[Int] =
  run {
    liftQuery(myDataset).filter(_.name == "?").map(_.age)
  }
// This is generated during compile time:
// SELECT x1.age _1 FROM (?) x1 WHERE x1.name = '\?'
// It is reverted upon run-time:
// SELECT x1.age _1 FROM (ds1) x1 WHERE x1.name = '?'
```


