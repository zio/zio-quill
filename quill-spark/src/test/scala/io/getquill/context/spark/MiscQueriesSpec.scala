package io.getquill.context.spark

import io.getquill.Spec

case class Inner(i: Int)
case class Outer(inner: Inner)
case class Original(id: Long, name: String, age: Long, numeric: Long)
case class MapTo(id: Long, name: String, numeric: Long)

class MiscQueriesSpec extends Spec {

  val context = io.getquill.context.sql.testContext

  import testContext._
  import sqlContext.implicits._

  val tests = liftQuery {
    Seq(
      Test(1, 2, "Th r ee"),
      Test(4, 5, "s"),
      Test(6, 7, "N s in s e")
    ).toDS
  }

  // Also tested in SparkDialectSpec
  "nested structures" - {
    "nested property" in {
      val outers = liftQuery {
        Seq(
          Outer(Inner(1)),
          Outer(Inner(2)),
          Outer(Inner(1))
        ).toDS
      }
      val q = quote(outers.filter(t => t.inner.i == 1))
      testContext.run(q).collect.toList mustEqual
        List(Outer(Inner(1)), Outer(Inner(1)))
    }
    "nested tuple" in {
      val q = quote(tests.map(t => ((t.i, t.j), t.i + 1)))
      testContext.run(q).collect.toList mustEqual
        List(((1, 2), 2), ((4, 5), 5), ((6, 7), 7))
    }
  }

  // Also tested in SparkDialectSpec
  "concatMap with filter" - {
    "single select with filter" in {
      val q = quote(tests.concatMap(t => t.s.split(" ")).filter(s => s == "s"))
      testContext.run(q).collect.toList mustEqual
        List("s", "s", "s")
    }
    "nested select" in {
      val testsR = liftQuery {
        Seq(
          Test(1, 2, "r")
        ).toDS
      }
      val q = quote(
        tests
          .concatMap(t => t.s.split(" "))
          .join(testsR.concatMap(t => t.s.split(" ")))
          .on { case (a, b) => a == b }
      )
      testContext.run(q).collect.toList mustEqual
        List(("r", "r"))
    }
  }
  "map to CC after GroupBy" in {
    val originals = liftQuery {
      Seq(
        Original(1, "Joe", 0, 10),
        Original(1, "Joe", 0, 20),
        Original(2, "Sam", 2, 6),
        Original(3, "Hanna", 4, 8)
      ).toDS
    }
    val q = quote(
      originals
        .groupBy(p => (p.id, p.name))
        .map {
          case ((id, name), items) =>
            MapTo(
              id,
              name,
              items.map(_.numeric).sum.getOrElse(0L)
            )
        }
    )
    testContext.run(q).collect.toList mustEqual
      List(
        MapTo(1, "Joe", 30),
        MapTo(2, "Sam", 6),
        MapTo(3, "Hanna", 8)
      )
  }
}
