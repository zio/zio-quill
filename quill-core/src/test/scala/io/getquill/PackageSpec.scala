package test

import io.getquill._
import io.getquill.ast.{ Query => _, _ }
import io.getquill.quotation.NonQuotedException
import io.getquill.quotation.Quoted

class PackageSpec extends Spec {

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
}
