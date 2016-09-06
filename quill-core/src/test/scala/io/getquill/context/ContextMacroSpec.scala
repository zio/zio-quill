package io.getquill.context

import scala.reflect.ClassTag

import io.getquill.Spec
import io.getquill.testContext
import io.getquill.testContext._
import mirror.Row

class ContextMacroSpec extends Spec {

  "runs actions" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.delete
        }
        testContext.run(q).string mustEqual
          "query[TestEntity].delete"
      }
      "infix" in {
        val q = quote {
          infix"STRING".as[Action[TestEntity]]
        }
        testContext.run(q).string mustEqual
          """infix"STRING""""
      }
      "dynamic" in {
        val q = quote {
          qr1.delete
        }
        testContext.run(q.dynamic).string mustEqual
          "query[TestEntity].delete"
      }
      "dynamic type param" in {
        def test[T: ClassTag] = quote(query[T].delete)
        val r = testContext.run(test[TestEntity])
        r.string mustEqual "query[TestEntity].delete"
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          qr1.filter(t => t.s == lift("a")).delete
        }
        val r = testContext.run(q)
        r.string mustEqual "query[TestEntity].filter(t => t.s == ?).delete"
        r.prepareRow mustEqual Row("a")
      }
      "infix" in {
        val q = quote {
          infix"t = ${lift("a")}".as[Action[TestEntity]]
        }
        val r = testContext.run(q)
        r.string mustEqual s"""infix"t = $${?}""""
        r.prepareRow mustEqual Row("a")
      }
      "dynamic" in {
        val q = quote {
          infix"t = ${lift("a")}".as[Action[TestEntity]]
        }
        val r = testContext.run(q.dynamic)
        r.string mustEqual s"""infix"t = $${?}""""
        r.prepareRow mustEqual Row("a")
      }
      "dynamic type param" in {
        import language.reflectiveCalls
        def test[T <: { def i: Int }: ClassTag] = quote {
          query[T].filter(t => t.i == lift(1)).delete
        }
        val r = testContext.run(test[TestEntity])
        r.string mustEqual "query[TestEntity].filter(t => t.i == ?).delete"
        r.prepareRow mustEqual Row(1)
      }
    }
  }

  "runs queries" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "query[TestEntity].map(t => t.s)"
      }
      "infix" in {
        val q = quote {
          infix"STRING".as[Query[TestEntity]].map(t => t.s)
        }
        testContext.run(q).string mustEqual
          """infix"STRING".map(t => t.s)"""
      }
      "dynamic" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        testContext.run(q.dynamic).string mustEqual
          "query[TestEntity].map(t => t.s)"
      }
      "dynamic type param" in {
        def test[T: ClassTag] = quote(query[T])
        val r = testContext.run(test[TestEntity])
        r.string mustEqual "query[TestEntity].map(x => (x.s, x.i, x.l, x.o))"
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          qr1.filter(t => t.s == lift("a"))
        }
        val r = testContext.run(q)
        r.string mustEqual "query[TestEntity].filter(t => t.s == ?).map(t => (t.s, t.i, t.l, t.o))"
        r.prepareRow mustEqual Row("a")
      }

      "wrapped" in {
        case class Entity(x: WrappedEncodable)
        val q = quote {
          query[Entity].filter(t => t.x == lift(WrappedEncodable(1)))
        }
        val r = testContext.run(q)
        r.string mustEqual "query[Entity].filter(t => t.x == ?).map(t => t.x)"
        r.prepareRow mustEqual Row(1)
      }
      "infix" in {
        val q = quote {
          infix"SELECT ${lift("a")}".as[Query[String]]
        }
        val r = testContext.run(q)
        r.string mustEqual s"""infix"SELECT $${?}".map(x => x)"""
        r.prepareRow mustEqual Row("a")
      }
      "dynamic" in {
        val q = quote {
          qr1.filter(t => t.s == lift("a"))
        }
        val r = testContext.run(q.dynamic)
        r.string mustEqual "query[TestEntity].filter(t => t.s == ?).map(t => (t.s, t.i, t.l, t.o))"
        r.prepareRow mustEqual Row("a")
      }
      "dynamic type param" in {
        def test[T: ClassTag] = quote {
          query[T].map(t => lift(1))
        }
        val r = testContext.run(test[TestEntity])
        r.string mustEqual "query[TestEntity].map(t => ?)"
        r.prepareRow mustEqual Row(1)
      }
    }
    "aggregated" in {
      val q = quote {
        qr1.map(t => t.i).max
      }
      testContext.run(q).string mustEqual
        "query[TestEntity].map(t => t.i).max"
    }
  }

  "can't be used as a var" in {
    var db = testContext
    "db.run(qr1)" mustNot compile
  }

  "fails if there's a free variable" in {
    val q = {
      val i = 1
      quote {
        qr1.filter(_.i == i)
      }
    }
    "testContext.run(q)" mustNot compile
  }
}
