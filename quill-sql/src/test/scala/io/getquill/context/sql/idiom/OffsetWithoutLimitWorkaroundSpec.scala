package io.getquill.context.sql.idiom

import io.getquill.naming.Literal
import io.getquill.context.sql.mirrorContext._
import io.getquill.util.Show._
import io.getquill.context.sql.SqlQuery
import io.getquill.context.sql.SqlSpec

class OffsetWithoutLimitWorkaroundSpec extends SqlSpec {

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
