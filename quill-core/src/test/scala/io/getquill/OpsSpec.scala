package test

import io.getquill.ast.{ Query => _, _ }
import io.getquill.quotation.NonQuotedException
import io.getquill.testContext._
import io.getquill.Spec

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

  "fails if the infix is used outside of a quotation" in {
    val e = intercept[NonQuotedException] {
      infix"true"
    }
  }

  "fails if a query is used ouside of a quotation" in {
    val e = intercept[NonQuotedException] {
      query[TestEntity]
    }
  }

  "fails if a quotation is unquoted ouside of a quotation" in {
    val e = intercept[NonQuotedException] {
      unquote(qr1)
    }
  }

  "fails if ord is unquoted ouside of a quotation" in {
    val e = intercept[NonQuotedException] {
      Ord.asc[Int]
    }
  }

  "fails if orderingToOrd is unquoted ouside of a quotation" in {
    val e = intercept[NonQuotedException] {
      implicitOrd[Int]
    }
  }
}
