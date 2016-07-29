package io.getquill.norm.select

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.testContext
import io.getquill.testContext._

class SelectFlatteningSpec extends Spec {

  case class Test[T](v: T)
  case class Evil(x: Thread)

  "flattens the final map (select) of queries" - {

    "a simple select doesn't change" in {
      val q = quote {
        qr1.map(t => t.s)
      }
      testContext.run(q).string mustEqual
        "query[TestEntity].map(t => t.s)"
    }

    "flattens a case class select" - {
      "normal" in {
        testContext.run(qr1).string mustEqual
          "query[TestEntity].map(x => (x.s, x.i, x.l, x.o))"
      }
      "with type param" in {
        val q = quote {
          query[Test[Int]]
        }
        testContext.run(q).string mustEqual
          "query[Test].map(x => x.v)"
      }
    }

    "flattens a select with case class and simple values" - {
      "case class in the beginning of the select" in {
        val q = quote {
          qr1.map(x => (x, x.s, x.i, x.l))
        }
        testContext.run(q).string mustEqual
          "query[TestEntity].map(x => (x.s, x.i, x.l, x.o, x.s, x.i, x.l))"
      }
      "case class in the middle of the select" in {
        val q = quote {
          qr1.map(x => (x.s, x, x.i, x.l))
        }
        testContext.run(q).string mustEqual
          "query[TestEntity].map(x => (x.s, x.s, x.i, x.l, x.o, x.i, x.l))"
      }
      "case class in the end of the select" in {
        val q = quote {
          qr1.map(x => (x.s, x.i, x.l, x))
        }
        testContext.run(q).string mustEqual
          "query[TestEntity].map(x => (x.s, x.i, x.l, x.s, x.i, x.l, x.o))"
      }
      "two case classes" in {
        val q = quote {
          qr1.flatMap(x => qr2.map(y => (x, x.s, y)))
        }
        testContext.run(q).string mustEqual
          "query[TestEntity].flatMap(x => query[TestEntity2].map(y => (x.s, x.i, x.l, x.o, x.s, y.s, y.i, y.l, y.o)))"
      }
    }
  }

  "fails if the context doesn't know how to encode the type" - {
    "simple value" in {
      "testContext.run(query[Evil].map(_.x))" mustNot compile
    }
    "case class" in {
      "testContext.run(query[Evil])" mustNot compile
    }
  }

  "uses a custom implicit decoder" in {
    case class Value(s: String)
    case class Test(v: Value)
    implicit val valueDecoder = new Decoder[Value] {
      override def apply(index: Int, row: Row) =
        Value(row[String](index))
    }
    val q = quote(query[Test])
    testContext.run(q).extractor(Row("test")) mustEqual Test(Value("test"))
  }
}
