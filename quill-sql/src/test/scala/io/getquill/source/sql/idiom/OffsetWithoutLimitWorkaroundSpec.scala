package io.getquill.source.sql.idiom

import io.getquill.util.Show._
import io.getquill._
import io.getquill.source.sql.mirror.mirrorSource
import io.getquill.source.sql.SqlQuery

class OffsetWithoutLimitWorkaroundSpec extends Spec {

  val subject = new SqlIdiom with OffsetWithoutLimitWorkaround {}

  import subject._

  "creates a synthectic limit" in {
    val q = quote {
      qr1.drop(1)
    }
    SqlQuery(q.ast).show mustEqual
      "SELECT * FROM TestEntity x LIMIT 18446744073709551610 OFFSET 1"
  }

}
