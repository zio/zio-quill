package io.getquill.norm.select

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.testContext
import io.getquill.testContext.Decoder
import io.getquill.testContext.qr1
import io.getquill.testContext.qr2
import io.getquill.testContext.query
import io.getquill.testContext.quote
import io.getquill.testContext.unquote

class SelectFlatteningSpec extends Spec {

  "flattens the final map (select) of queries" - {

    "a simple select doesn't change" in {
      val q = quote {
        qr1.map(t => t.s)
      }
      testContext.run(q).ast mustEqual q.ast
    }

    "flattens a case class select" - {
      "normal" in {
        val n = quote {
          qr1.map(x => (x.s, x.i, x.l, x.o))
        }
        testContext.run(qr1).ast mustEqual n.ast
      }
      "with type param" in {
        case class Test[T](v: T)
        val q = quote {
          query[Test[Int]]
        }
        val n = quote {
          query[Test[Int]].map(x => x.v)
        }
        testContext.run(q).ast mustEqual n.ast
      }
    }

    "flattens a select with case class and simple values" - {
      "case class in the beginning of the select" in {
        val q = quote {
          qr1.map(x => (x, x.s, x.i, x.l))
        }
        val n = quote {
          qr1.map(x => (x.s, x.i, x.l, x.o, x.s, x.i, x.l))
        }
        testContext.run(q).ast mustEqual n.ast
      }
      "case class in the middle of the select" in {
        val q = quote {
          qr1.map(x => (x.s, x, x.i, x.l))
        }
        val n = quote {
          qr1.map(x => (x.s, x.s, x.i, x.l, x.o, x.i, x.l))
        }
        testContext.run(q).ast mustEqual n.ast
      }
      "case class in the end of the select" in {
        val q = quote {
          qr1.map(x => (x.s, x.i, x.l, x))
        }
        val n = quote {
          qr1.map(x => (x.s, x.i, x.l, x.s, x.i, x.l, x.o))
        }
        testContext.run(q).ast mustEqual n.ast
      }
      "two case classes" in {
        val q = quote {
          qr1.flatMap(x => qr2.map(y => (x, x.s, y)))
        }
        val n = quote {
          qr1.flatMap(x => qr2.map(y => (x.s, x.i, x.l, x.o, x.s, y.s, y.i, y.l, y.o)))
        }
        testContext.run(q).ast mustEqual n.ast
      }
    }
  }

  "fails if the context doesn't know how to encode the type" - {
    case class Evil(x: Thread)
    "simple value" in {
      val q = quote {
        query[Evil].map(_.x)
      }
      "testContext.run(q)" mustNot compile
    }
    "case class" in {
      val q = quote {
        query[Evil]
      }
      "testContext.run(q)" mustNot compile
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
