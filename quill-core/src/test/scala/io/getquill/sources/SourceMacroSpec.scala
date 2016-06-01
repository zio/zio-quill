package io.getquill.sources

import io.getquill.Spec
import io.getquill.testSource._
import io.getquill.testSource

import mirror.Row
import scala.reflect.ClassTag

class SourceMacroSpec extends Spec {

  "runs actions" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.delete
        }
        testSource.run(q).ast mustEqual q.ast
      }
      "infix" in {
        val q = quote {
          infix"STRING".as[Action[TestEntity]]
        }
        testSource.run(q).ast mustEqual q.ast
      }
      "dynamic" in {
        val q: Quoted[Action[TestEntity]] = quote {
          qr1.delete
        }
        testSource.run(q).ast mustEqual q.ast
      }
      "dynamic type param" in {
        def test[T: ClassTag] = quote(query[T].delete)
        val r = testSource.run(test[TestEntity])
        r.ast.toString mustEqual "query[TestEntity].delete"
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          (a: String) => qr1.filter(t => t.s == a).delete
        }
        val r = testSource.run(q)(List("a"))
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.s == p1).delete"
        r.bindList mustEqual List(Row("a"))
      }
      "infix" in {
        val q = quote {
          (p1: String) => infix"t = $p1".as[Action[TestEntity]]
        }
        val r = testSource.run(q)(List("a"))
        r.ast.toString mustEqual """infix"t = $p1""""
        r.bindList mustEqual List(Row("a"))
      }
      "dynamic" in {
        val q: Quoted[String => Action[TestEntity]] = quote {
          (p1: String) => infix"t = $p1".as[Action[TestEntity]]
        }
        val r = testSource.run(q)(List("a"))
        r.ast.toString mustEqual """infix"t = $p1""""
        r.bindList mustEqual List(Row("a"))
      }
      "dynamic type param" in {
        import language.reflectiveCalls
        def test[T <: { def i: Int }: ClassTag] = quote {
          (p1: Int) => query[T].filter(t => t.i == p1).delete
        }
        val r = testSource.run(test[TestEntity])(List(1))
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
        testSource.run(q).ast mustEqual q.ast
      }
      "infix" in {
        val q = quote {
          infix"STRING".as[Query[TestEntity]].map(t => t.s)
        }
        testSource.run(q).ast mustEqual q.ast
      }
      "dynamic" in {
        val q: Quoted[Query[String]] = quote {
          qr1.map(t => t.s)
        }
        testSource.run(q).ast mustEqual q.ast
      }
      "dynamic type param" in {
        def test[T: ClassTag] = quote(query[T])
        val r = testSource.run(test[TestEntity])
        r.ast.toString mustEqual "query[TestEntity].map(x => (x.s, x.i, x.l, x.o))"
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          (p1: String) => qr1.filter(t => t.s == p1)
        }
        val r = testSource.run(q)("a")
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.s == p1).map(t => (t.s, t.i, t.l, t.o))"
        r.binds mustEqual Row("a")
      }

      "wrapped" in {
        case class Entity(x: WrappedEncodable)
        val q = quote {
          (p1: WrappedEncodable) => query[Entity].filter(t => t.x == p1)
        }
        val r = testSource.run(q)(WrappedEncodable(1))
        r.ast.toString mustEqual "query[Entity].filter(t => t.x == p1).map(t => t.x)"
        r.binds mustEqual Row(1)
      }
      "infix" in {
        val q = quote {
          (p1: String) => infix"SELECT $p1".as[Query[String]]
        }
        val r = testSource.run(q)("a")
        r.ast.toString mustEqual """infix"SELECT $p1".map(x => x)"""
        r.binds mustEqual Row("a")
      }
      "dynamic" in {
        val q: Quoted[String => Query[TestEntity]] = quote {
          (p1: String) => qr1.filter(t => t.s == p1)
        }
        val r = testSource.run(q)("a")
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.s == p1).map(t => (t.s, t.i, t.l, t.o))"
        r.binds mustEqual Row("a")
      }
      "dynamic type param" in {
        def test[T: ClassTag] = quote {
          (p1: Int) => query[T].map(t => p1)
        }
        val r = testSource.run(test[TestEntity])(1)
        r.ast.toString mustEqual "query[TestEntity].map(t => p1)"
        r.binds mustEqual Row(1)
      }
      "free variable" in {
        val i = 1
        val q = quote {
          qr1.filter(x => x.i == lift(i))
        }
        val r = testSource.run(q)
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
        val r = testSource.run(q2)
        r.ast.toString mustEqual "query[TestEntity].filter(x => (x.i == lift(q1.i)) && (x.l == lift(l))).map(x => (x.s, x.i, x.l, x.o))"
        r.binds mustEqual Row(i, l)
      }

    }
    "aggregated" in {
      val q = quote {
        qr1.map(t => t.i).max
      }
      testSource.run(q).ast mustEqual q.ast
    }
  }

  "can be used as a var" in {
    var db = testSource
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
    "testSource.run(q)" mustNot compile
  }
}
