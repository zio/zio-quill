package io.getquill.norm

import io.getquill.ast.{AscNullsFirst, Constant, Ident, Map, SortBy}
import io.getquill.MirrorContexts.testContext._
import io.getquill.Query
import io.getquill.base.Spec
import io.getquill.Quoted
import io.getquill.ast.Ast

class AttachToEntitySpec extends Spec {

  val attachToEntity: Ast => Ast =
    (AttachToEntity(SortBy(_, _, Constant.auto(1), AscNullsFirst)) _).andThen(replaceTempIdent.apply _)

  "attaches clause to the root of the query (entity)" - {
    "query is the entity" in {
      val n = quote {
        qr1.sortBy(_ => 1)
      }
      attachToEntity(qr1.ast) mustEqual n.ast
    }
    "query is a composition" - {
      "map" in {
        val q = quote {
          qr1.filter(t => t.i == 1).map(t => t.s)
        }
        val n = quote {
          qr1.sortBy(_ => 1).filter(t => t.i == 1).map(t => t.s)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "flatMap" in {
        val q = quote {
          qr1.filter(t => t.i == 1).flatMap(_ => qr2)
        }
        val n = quote {
          qr1.sortBy(_ => 1).filter(t => t.i == 1).flatMap(_ => qr2)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "concatMap" in {
        val q = quote {
          qr1.filter(t => t.i == 1).concatMap(t => t.s.split(" "))
        }
        val n = quote {
          qr1.sortBy(_ => 1).filter(t => t.i == 1).concatMap(t => t.s.split(" "))
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "filter" in {
        val q = quote {
          qr1.filter(t => t.i == 1).filter(t => t.s == "s1")
        }
        val n = quote {
          qr1.sortBy(_ => 1).filter(t => t.i == 1).filter(t => t.s == "s1")
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "sortBy" in {
        val q = quote {
          qr1.sortBy(t => t.s)
        }
        val n = quote {
          qr1.sortBy(_ => 1).sortBy(t => t.s)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "take" in {
        val q = quote {
          qr1.sortBy(b => b.s).take(1)
        }
        val n = quote {
          qr1.sortBy(_ => 1).sortBy(b => b.s).take(1)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "drop" in {
        val q = quote {
          qr1.sortBy(b => b.s).drop(1)
        }
        val n = quote {
          qr1.sortBy(_ => 1).sortBy(b => b.s).drop(1)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "distinct" in {
        val q = quote {
          qr1.sortBy(b => b.s).drop(1).distinct
        }
        val n = quote {
          qr1.sortBy(_ => 1).sortBy(b => b.s).drop(1).distinct
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
    }
  }

  val iqr1: Quoted[Query[TestEntity]] = quote {
    sql"$qr1".as[Query[TestEntity]]
  }

  "attaches clause to the root of the query (infix)" - {
    "query is the entity" in {
      val n = quote {
        iqr1.sortBy(_ => 1)
      }
      attachToEntity(iqr1.ast) mustEqual n.ast
    }
    "query is a composition" - {
      "map" in {
        val q = quote {
          iqr1.filter(t => t.i == 1).map(t => t.s)
        }
        val n = quote {
          iqr1.sortBy(_ => 1).filter(t => t.i == 1).map(t => t.s)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "flatMap" in {
        val q = quote {
          iqr1.filter(t => t.i == 1).flatMap(_ => qr2)
        }
        val n = quote {
          iqr1.sortBy(_ => 1).filter(t => t.i == 1).flatMap(_ => qr2)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "concatMap" in {
        val q = quote {
          iqr1.filter(t => t.i == 1).concatMap(t => t.s.split(" "))
        }
        val n = quote {
          iqr1.sortBy(_ => 1).filter(t => t.i == 1).concatMap(t => t.s.split(" "))
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "filter" in {
        val q = quote {
          iqr1.filter(t => t.i == 1).filter(t => t.s == "s1")
        }
        val n = quote {
          iqr1.sortBy(_ => 1).filter(t => t.i == 1).filter(t => t.s == "s1")
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "sortBy" in {
        val q = quote {
          iqr1.sortBy(t => t.s)
        }
        val n = quote {
          iqr1.sortBy(_ => 1).sortBy(t => t.s)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "take" in {
        val q = quote {
          iqr1.sortBy(b => b.s).take(1)
        }
        val n = quote {
          iqr1.sortBy(_ => 1).sortBy(b => b.s).take(1)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "drop" in {
        val q = quote {
          iqr1.sortBy(b => b.s).drop(1)
        }
        val n = quote {
          iqr1.sortBy(_ => 1).sortBy(b => b.s).drop(1)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "distinct" in {
        val q = quote {
          iqr1.sortBy(b => b.s).drop(1).distinct
        }
        val n = quote {
          iqr1.sortBy(_ => 1).sortBy(b => b.s).drop(1).distinct
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
    }
  }

  "falls back to the query if it's not possible to flatten it" - {
    "union" in {
      val q = quote {
        qr1.union(qr2)
      }
      val n = quote {
        qr1.union(qr2).sortBy(_ => 1)
      }
      attachToEntity(q.ast) mustEqual n.ast
    }
    "unionAll" in {
      val q = quote {
        qr1.unionAll(qr2)
      }
      val n = quote {
        qr1.unionAll(qr2).sortBy(_ => 1)
      }
      attachToEntity(q.ast) mustEqual n.ast
    }
    "outer join" in {
      val q = quote {
        qr1.leftJoin(qr2).on((_, _) => true)
      }
      val n = quote {
        qr1.leftJoin(qr2).on((_, _) => true).sortBy(_ => 1)
      }
      attachToEntity(q.ast) mustEqual n.ast
    }
    "groupBy.map" in {
      val q = quote {
        qr1.groupBy(a => a.i).map(_ => 1)
      }
      val n = quote {
        qr1.groupBy(a => a.i).map(_ => 1).sortBy(_ => 1)
      }
      attachToEntity(q.ast) mustEqual n.ast
    }
  }

  "fails if the entity isn't found" in {
    intercept[IllegalStateException] {
      attachToEntity(Map(Ident("a"), Ident("b"), Ident("c")))
    }
    ()
  }
}
