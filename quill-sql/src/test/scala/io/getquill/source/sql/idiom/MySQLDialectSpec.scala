package io.getquill.source.sql.idiom

import io.getquill.util.Show._
import io.getquill._
import io.getquill.ast._

class MySQLDialectSpec extends Spec {

  import MySQLDialect._

  "mixes the workaround for offset without limit" in {
    MySQLDialect.isInstanceOf[OffsetWithoutLimitWorkaround] mustEqual true
  }

  "uses CONCAT instead if ||" in {
    val q = quote {
      qr1.map(t => t.s + t.s)
    }
    (q.ast: Ast).show mustEqual
      "SELECT CONCAT(t.s, t.s) FROM TestEntity t"
  }
}
