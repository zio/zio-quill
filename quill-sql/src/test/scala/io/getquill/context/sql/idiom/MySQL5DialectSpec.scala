package io.getquill.context.sql.idiom

import io.getquill._

class MySQL5DialectSpec extends AbstractMySQLDialectSpec {
  lazy val ctx: SqlMirrorContext[MySQL5Dialect.type, Literal.type] with TestEntities =
    new SqlMirrorContext(MySQL5Dialect, Literal) with TestEntities

  import ctx._

  "delete is without table alias" in {
    val q = quote {
      qr1.filter(t => t.i == 999).delete
    }
    ctx.run(q).string mustEqual
      "DELETE FROM TestEntity WHERE i = 999"
  }
}
