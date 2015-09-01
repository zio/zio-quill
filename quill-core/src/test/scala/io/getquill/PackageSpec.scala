package test

import io.getquill.Spec
import io.getquill.ast.Entity
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.queryable
import io.getquill.quotation.NonQuotedException
import io.getquill.quote
import io.getquill.unquote

class PackageSpec extends Spec {

  "quotes queries" in {
    val q = quote {
      qr1.map(t => t)
    }
    q.ast mustEqual Map(Entity("TestEntity"), Ident("t"), Ident("t"))
  }

  "fails if a queryable is used ouside of a quotation" in {
    val e = intercept[NonQuotedException] {
      queryable[TestEntity]
    }
  }

  "fails if a quotation is unquoted ouside of a quotation" in {
    val e = intercept[NonQuotedException] {
      unquote(qr1)
    }
  }
}
