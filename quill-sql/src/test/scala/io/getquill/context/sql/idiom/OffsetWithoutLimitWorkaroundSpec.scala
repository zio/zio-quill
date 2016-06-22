package io.getquill.context.sql.idiom

import io.getquill.Spec
import io.getquill.context.sql.SqlQuery
import io.getquill.context.sql.testContext.qr1
import io.getquill.context.sql.testContext.quote
import io.getquill.context.sql.testContext.unquote
import io.getquill.naming.Literal
import io.getquill.util.Show.Shower

class OffsetWithoutLimitWorkaroundSpec extends Spec {

  val subject = new SqlIdiom with OffsetWithoutLimitWorkaround {
    def prepare(sql: String) = sql
  }

  implicit val naming = new Literal {}

  import subject._

  "creates a synthectic limit" in {
    val q = quote {
      qr1.drop(1)
    }
    SqlQuery(q.ast).show mustEqual
      "SELECT x.* FROM TestEntity x LIMIT 18446744073709551610 OFFSET 1"
  }

}
