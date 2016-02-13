package io.getquill.sources.sql.idiom

import io.getquill.naming.Literal
import io.getquill.util.Show._
import io.getquill._
import io.getquill.sources.sql.mirrorSource
import io.getquill.sources.sql.SqlQuery

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
