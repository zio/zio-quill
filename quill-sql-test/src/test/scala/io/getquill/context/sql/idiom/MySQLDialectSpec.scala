package io.getquill.context.sql.idiom

import io.getquill._
import io.getquill.idiom.StringToken

class MySQLDialectSpec extends AbstractMySQLDialectSpec {
  lazy val ctx = new SqlMirrorContext(MySQLDialect, Literal) with TestEntities

  import ctx._

  "delete is with table alias" in {
    val q = quote {
      qr1.filter(t => t.i == 999).delete
    }
    ctx.run(q).string mustEqual
      "DELETE FROM TestEntity t WHERE t.i = 999"
  }
}
