package io.getquill.context

import scala.reflect.ClassTag

import io.getquill.Spec
import io.getquill.testContext
import io.getquill.testContext.Action
import io.getquill.testContext.InfixInterpolator
import io.getquill.testContext.Query
import io.getquill.testContext.Quoted
import io.getquill.testContext.TestEntity
import io.getquill.testContext.lift
import io.getquill.testContext.qr1
import io.getquill.testContext.query
import io.getquill.testContext.quote
import io.getquill.testContext.unquote
import mirror.Row

class ContextMacroSpec extends Spec {

  "runs actions" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.delete
        }
        testContext.run(q).ast mustEqual q.ast
      }
      "infix" in {
        val q = quote {
          infix"STRING".as[Action[TestEntity, Long]]
        }
        testContext.run(q).ast mustEqual q.ast
      }
      "dynamic" in {
        val q: Quoted[Action[TestEntity, Long]] = quote {
          qr1.delete
        }
        testContext.run(q).ast mustEqual q.ast
      }
      "dynamic type param" in {
        def test[T: ClassTag] = quote(query[T].delete)
        val r = testContext.run(test[TestEntity])
        r.ast.toString mustEqual "query[TestEntity].delete"
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          (a: String) => qr1.filter(t => t.s == a).delete
        }
        val r = testContext.run(q)(List("a"))
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.s == p1).delete"
        r.bindList mustEqual List(Row("a"))
      }
      "infix" in {
        val q = quote {
          (p1: String) => infix"t = $p1".as[Action[TestEntity, Long]]
        }
        val r = testContext.run(q)(List("a"))
        r.ast.toString mustEqual """infix"t = $p1""""
        r.bindList mustEqual List(Row("a"))
      }
      "dynamic" in {
        val q: Quoted[String => Action[TestEntity, Long]] = quote {
          (p1: String) => infix"t = $p1".as[Action[TestEntity, Long]]
        }
        val r = testContext.run(q)(List("a"))
        r.ast.toString mustEqual """infix"t = $p1""""
        r.bindList mustEqual List(Row("a"))
      }
      "dynamic type param" in {
        import language.reflectiveCalls
        def test[T <: { def i: Int }: ClassTag] = quote {
          (p1: Int) => query[T].filter(t => t.i == p1).delete
        }
        val r = testContext.run(test[TestEntity])(List(1))
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.i == p1).delete"
        r.bindList mustEqual List(Row(1))
      }
    }
  }

  "runs queries" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        testContext.run(q).ast mustEqual q.ast
      }
      "infix" in {
        val q = quote {
          infix"STRING".as[Query[TestEntity]].map(t => t.s)
        }
        testContext.run(q).ast mustEqual q.ast
      }
      "dynamic" in {
        val q: Quoted[Query[String]] = quote {
          qr1.map(t => t.s)
        }
        testContext.run(q).ast mustEqual q.ast
      }
      "dynamic type param" in {
        def test[T: ClassTag] = quote(query[T])
        val r = testContext.run(test[TestEntity])
        r.ast.toString mustEqual "query[TestEntity].map(x => (x.s, x.i, x.l, x.o))"
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          (p1: String) => qr1.filter(t => t.s == p1)
        }
        val r = testContext.run(q)("a")
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.s == p1).map(t => (t.s, t.i, t.l, t.o))"
        r.binds mustEqual Row("a")
      }

      "wrapped" in {
        case class Entity(x: WrappedEncodable)
        val q = quote {
          (p1: WrappedEncodable) => query[Entity].filter(t => t.x == p1)
        }
        val r = testContext.run(q)(WrappedEncodable(1))
        r.ast.toString mustEqual "query[Entity].filter(t => t.x == p1).map(t => t.x)"
        r.binds mustEqual Row(1)
      }
      "infix" in {
        val q = quote {
          (p1: String) => infix"SELECT $p1".as[Query[String]]
        }
        val r = testContext.run(q)("a")
        r.ast.toString mustEqual """infix"SELECT $p1".map(x => x)"""
        r.binds mustEqual Row("a")
      }
      "dynamic" in {
        val q: Quoted[String => Query[TestEntity]] = quote {
          (p1: String) => qr1.filter(t => t.s == p1)
        }
        val r = testContext.run(q)("a")
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.s == p1).map(t => (t.s, t.i, t.l, t.o))"
        r.binds mustEqual Row("a")
      }
      "dynamic type param" in {
        def test[T: ClassTag] = quote {
          (p1: Int) => query[T].map(t => p1)
        }
        val r = testContext.run(test[TestEntity])(1)
        r.ast.toString mustEqual "query[TestEntity].map(t => p1)"
        r.binds mustEqual Row(1)
      }
      "free variable" in {
        val i = 1
        val q = quote {
          qr1.filter(x => x.i == lift(i))
        }
        val r = testContext.run(q)
        r.ast.toString mustEqual "query[TestEntity].filter(x => x.i == lift(i)).map(x => (x.s, x.i, x.l, x.o))"
        r.binds mustEqual Row(1)
      }
      "multiple free variables" in {
        val i = 1
        val l = 1L
        val q1 = quote {
          qr1.filter(x => x.i == lift(i))
        }
        val q2 = quote {
          q1.filter(x => x.l == lift(l))
        }
        val r = testContext.run(q2)
        r.ast.toString mustEqual "query[TestEntity].filter(x => (x.i == lift(q1.i)) && (x.l == lift(l))).map(x => (x.s, x.i, x.l, x.o))"
        r.binds mustEqual Row(i, l)
      }

    }
    "aggregated" in {
      val q = quote {
        qr1.map(t => t.i).max
      }
      testContext.run(q).ast mustEqual q.ast
    }
  }

  "can be used as a var" in {
    var db = testContext
    db.run(qr1)
    ()
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
