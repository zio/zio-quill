package test

import io.getquill._
import io.getquill.ast._
import io.getquill.quotation.NonQuotedException
import io.getquill.Spec

class PackageSpec extends Spec {

  "quotes queries" in {
    val q = quote {
      qr1.map(t => t)
    }
    q.ast mustEqual Map(Entity("TestEntity"), Ident("t"), Ident("t"))
  }

  "fails if a queryable is used ouside of a quotation" in {
    intercept[NonQuotedException] {
      queryable[TestEntity]
    }
  }

  "fails if a quotation is unquoted ouside of a quotation" in {
    intercept[NonQuotedException] {
      unquote(qr1)
    }
  }
}
