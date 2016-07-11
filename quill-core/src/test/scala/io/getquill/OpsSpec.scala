package test

import io.getquill.Spec
import io.getquill.ast.Entity
import io.getquill.ast.Ident
import io.getquill.ast.Infix
import io.getquill.ast.Map
import io.getquill.testContext.EntityQuery
import io.getquill.testContext.InfixInterpolator
import io.getquill.testContext.Query
import io.getquill.testContext.TestEntity
import io.getquill.testContext.qr1
import io.getquill.testContext.query
import io.getquill.testContext.quote
import io.getquill.testContext.unquote
import io.getquill.testContext.Quoted

class OpsSpec extends Spec {

  "quotes asts" - {
    "explicitly" in {
      val q = quote {
        query[TestEntity]
      }
      q.ast mustEqual Entity("TestEntity")
    }
    "implicitly" in {
      val q: Quoted[Query[TestEntity]] =
        query[TestEntity]
    }
  }

  "unquotes asts" - {
    "explicitly" in {
      val q = quote {
        unquote(qr1).map(t => t)
      }
      q.ast mustEqual Map(Entity("TestEntity"), Ident("t"), Ident("t"))
    }
    "implicitly" in {
      val q = quote {
        qr1.map(t => t)
      }
      q.ast mustEqual Map(Entity("TestEntity"), Ident("t"), Ident("t"))
    }
  }

  "provides the infix interpolator" - {
    "with `as`" in {
      val q = quote {
        infix"true".as[Boolean]
      }
      q.ast mustEqual Infix(List("true"), Nil)
    }
    "without `as`" in {
      val q = quote {
        infix"true"
      }
      q.ast mustEqual Infix(List("true"), Nil)
    }
  }

  "unquotes duble quotations" in {
    val q: Quoted[EntityQuery[TestEntity]] = quote {
      quote(query[TestEntity])
    }
    q.toString mustEqual "query[TestEntity]"
  }

  "unquotes quoted function bodies automatically" - {
    implicit class QueryOps[Q <: Query[_]](q: Q) {
      def allowFiltering = quote(infix"$q ALLOW FILTERING".as[Q])
    }
    "one param" in {
      val q: Quoted[Int => EntityQuery[TestEntity]] = quote {
        (i: Int) =>
          query[TestEntity].allowFiltering
      }
      q.toString mustEqual "(i) => infix\"" + "$" + "{query[TestEntity]} ALLOW FILTERING\""
    }
    "multiple params" in {
      val q: Quoted[(Int, Int, Int) => EntityQuery[TestEntity]] = quote {
        (i: Int, j: Int, k: Int) =>
          query[TestEntity].allowFiltering
      }
      q.toString mustEqual "(i, j, k) => infix\"" + "$" + "{query[TestEntity]} ALLOW FILTERING\""
    }
  }
}
