package io.getquill.source.sql.idiom

import io.getquill.util.Show._
import io.getquill._
import io.getquill.ast._
import io.getquill.source.sql.naming.Literal

class MySQLDialectSpec extends Spec {

  import MySQLDialect._

  implicit val naming = new Literal {}

  "mixes the workaround for offset without limit" in {
    MySQLDialect.isInstanceOf[OffsetWithoutLimitWorkaround] mustEqual true
  }

  "uses CONCAT instead of ||" in {
    val q = quote {
      qr1.map(t => t.s + t.s)
    }
    (q.ast: Ast).show mustEqual
      "SELECT CONCAT(t.s, t.s) FROM TestEntity t"
  }

  "supports the `prepare` statement" in {
    val sql = "test"
    MySQLDialect.prepare(sql) mustEqual
      Some(s"PREPARE p${sql.hashCode.abs} FROM '$sql'")
  }
}
