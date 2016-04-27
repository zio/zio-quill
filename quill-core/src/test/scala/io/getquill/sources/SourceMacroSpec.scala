package io.getquill.sources

import io.getquill.quotation.Quoted
import io.getquill._
import io.getquill.TestSource.mirrorSource
import mirror.Row

class SourceMacroSpec extends Spec {

  "runs actions" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.delete
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
      "infix" in {
        val q = quote {
          infix"STRING".as[Action[TestEntity]]
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
      "dynamic" in {
        val q: Quoted[Action[TestEntity]] = quote {
          qr1.delete
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          (a: String) => qr1.filter(t => t.s == a).delete
        }
        val r = mirrorSource.run(q)(List("a"))
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.s == p1).delete"
        r.bindList mustEqual List(Row("a"))
      }
      "infix" in {
        val q = quote {
          (p1: String) => infix"t = $p1".as[Action[TestEntity]]
        }
        val r = mirrorSource.run(q)(List("a"))
        r.ast.toString mustEqual """infix"t = $p1""""
        r.bindList mustEqual List(Row("a"))
      }
      "dynamic" in {
        val q: Quoted[String => Action[TestEntity]] = quote {
          (p1: String) => infix"t = $p1".as[Action[TestEntity]]
        }
        val r = mirrorSource.run(q)(List("a"))
        r.ast.toString mustEqual """infix"t = $p1""""
        r.bindList mustEqual List(Row("a"))
      }
    }
  }

  "runs queries" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
      "infix" in {
        val q = quote {
          infix"STRING".as[Query[TestEntity]].map(t => t.s)
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
      "dynamic" in {
        val q: Quoted[Query[String]] = quote {
          qr1.map(t => t.s)
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          (p1: String) => qr1.filter(t => t.s == p1)
        }
        val r = mirrorSource.run(q)("a")
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.s == p1).map(t => (t.s, t.i, t.l, t.o))"
        r.binds mustEqual Row("a")
      }

      "wrapped" in {
        case class Entity(x: WrappedEncodable)
        val q = quote {
          (p1: WrappedEncodable) => query[Entity].filter(_.x == p1)
        }
        val r = mirrorSource.run(q)(WrappedEncodable(1))
        r.ast.toString mustEqual "query[Entity].filter(x1 => x1.x == p1).map(x1 => x1.x.value)"
        r.binds mustEqual Row(1)
      }
      "infix" in {
        val q = quote {
          (p1: String) => infix"SELECT $p1".as[Query[String]]
        }
        val r = mirrorSource.run(q)("a")
        r.ast.toString mustEqual """infix"SELECT $p1".map(x => x)"""
        r.binds mustEqual Row("a")
      }
      "dynamic" in {
        val q: Quoted[String => Query[TestEntity]] = quote {
          (p1: String) => qr1.filter(t => t.s == p1)
        }
        val r = mirrorSource.run(q)("a")
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.s == p1).map(t => (t.s, t.i, t.l, t.o))"
        r.binds mustEqual Row("a")
      }
      "free variable" in {
        val i = 1
        val q = quote {
          qr1.filter(x => x.i == lift(i))
        }
        val r = mirrorSource.run(q)
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
        val r = mirrorSource.run(q2)
        r.ast.toString mustEqual "query[TestEntity].filter(x => (x.i == lift(q1.i)) && (x.l == lift(l))).map(x => (x.s, x.i, x.l, x.o))"
        r.binds mustEqual Row(i, l)
      }

    }
    "aggregated" in {
      val q = quote {
        qr1.map(t => t.i).max
      }
      mirrorSource.run(q).ast mustEqual q.ast
    }
  }

  "can be used as a var" in {
    var db = source(new MirrorSourceConfig(""))
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
    "mirrorSource.run(q)" mustNot compile
  }
}
