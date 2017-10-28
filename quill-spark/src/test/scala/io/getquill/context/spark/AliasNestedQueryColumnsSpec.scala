package io.getquill.context.spark

import io.getquill.Spec

case class Test(i: Int, j: Int, s: String)

class AliasNestedQueryColumnsSpec extends Spec {

  import testContext._
  import sqlContext.implicits._

  val entities = Seq(Test(1, 2, "3"))

  val qr1 = liftQuery(entities.toDS)
  val qr2 = liftQuery(entities.toDS)

  "adds tuple alias" - {
    "flatten query" in {
      val q = quote {
        qr1.map(e => (e.i, e.j))
      }
      testContext.run(q).collect.toList mustEqual
        entities.map(e => (e.i, e.j))
    }
    "set operation" in {
      val q = quote {
        qr1 ++ qr1
      }
      testContext.run(q).collect.toList mustEqual
        entities ++ entities
    }
    "unary operation" in {
      val q = quote {
        qr1.filter(t => qr2.nested.nonEmpty)
      }
      testContext.run(q).collect.toList mustEqual
        entities
    }
    "nested" - {
      "query" in {
        val q = quote {
          qr1.nested
        }
        testContext.run(q).collect.toList mustEqual
          entities
      }
      "join" in {
        val q = quote {
          qr1.join(qr2).on((a, b) => a.i == b.i).map {
            case (a, b) => (a.i, b.i)
          }
        }
        testContext.run(q).collect.toList mustEqual
          List((1, 1))
      }
      "flatJoin" in {
        val q = quote {
          for {
            a <- qr1
            b <- qr2.join(b => b.i == a.i)
          } yield (a.i, b.i)
        }
        testContext.run(q).collect.toList mustEqual
          List((1, 1))
      }
    }
  }
}