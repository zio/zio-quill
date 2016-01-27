package io.getquill.sources.sql.idiom

import io.getquill.naming.Literal
import io.getquill.util.Show._
import io.getquill._
import io.getquill.ast._

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
      s"PREPARE p${sql.hashCode.abs} FROM '$sql'"
  }

  "workaround missing nulls ordering feature in mysql" - {
    "asc" in {
      val q = quote {
        qr1.sortBy(t => t.s)(Ord.asc)
      }
      (q.ast: Ast).show mustEqual
        "SELECT t.* FROM TestEntity t ORDER BY t.s ASC"
    }
    "desc" in {
      val q = quote {
        qr1.sortBy(t => t.s)(Ord.desc)
      }
      (q.ast: Ast).show mustEqual
        "SELECT t.* FROM TestEntity t ORDER BY t.s DESC"
    }
    "ascNullsFirst" in {
      val q = quote {
        qr1.sortBy(t => t.s)(Ord.ascNullsFirst)
      }
      (q.ast: Ast).show mustEqual
        "SELECT t.* FROM TestEntity t ORDER BY t.s ASC"
    }
    "descNullsFirst" in {
      val q = quote {
        qr1.sortBy(t => t.s)(Ord.descNullsFirst)
      }
      (q.ast: Ast).show mustEqual
        "SELECT t.* FROM TestEntity t ORDER BY ISNULL(t.s) DESC, t.s DESC"
    }
    "ascNullsLast" in {
      val q = quote {
        qr1.sortBy(t => t.s)(Ord.ascNullsLast)
      }
      (q.ast: Ast).show mustEqual
        "SELECT t.* FROM TestEntity t ORDER BY ISNULL(t.s) ASC, t.s ASC"
    }
    "descNullsLast" in {
      val q = quote {
        qr1.sortBy(t => t.s)(Ord.descNullsLast)
      }
      (q.ast: Ast).show mustEqual
        "SELECT t.* FROM TestEntity t ORDER BY t.s DESC"
    }
  }
}
