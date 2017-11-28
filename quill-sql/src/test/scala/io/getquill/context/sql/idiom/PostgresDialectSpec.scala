package io.getquill.context.sql.idiom

import io.getquill.Spec
import io.getquill.PostgresDialect
import io.getquill.SqlMirrorContext
import io.getquill.Literal
import io.getquill.TestEntities

class PostgresDialectSpec extends Spec {

  val context = new SqlMirrorContext(PostgresDialect, Literal) with TestEntities
  import context._

  "applies explicit casts" - {
    "toLong" in {
      val q = quote {
        qr1.map(t => t.s.toLong)
      }
      context.run(q).string mustEqual "SELECT t.s::bigint FROM TestEntity t"
    }
    "toInt" in {
      val q = quote {
        qr1.map(t => t.s.toInt)
      }
      context.run(q).string mustEqual "SELECT t.s::integer FROM TestEntity t"
    }
  }

  "Array Operations" - {
    case class ArrayOps(id: Int, numbers: Vector[Int])
    "contains" in {
      context.run(query[ArrayOps].filter(_.numbers.contains(10))).string mustEqual
        "SELECT x1.id, x1.numbers FROM ArrayOps x1 WHERE 10 = ANY(x1.numbers)"
    }
  }

  "prepareForProbing" in {
    import PostgresDialect._
    val id = preparedStatementId.get()

    prepareForProbing("SELECT t.x1, t.x2 FROM tb t WHERE (t.x1 = ?) AND (t.x2 = ?)") mustEqual
      s"PREPARE p${id + 1} AS SELECT t.x1, t.x2 FROM tb t WHERE (t.x1 = $$1) AND (t.x2 = $$2)"

    prepareForProbing("INSERT INTO tb (x1,x2,x3) VALUES (?,?,?)") mustEqual
      s"PREPARE p${id + 2} AS INSERT INTO tb (x1,x2,x3) VALUES ($$1,$$2,$$3)"
  }
}
