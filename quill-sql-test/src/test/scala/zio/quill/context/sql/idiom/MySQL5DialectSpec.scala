package zio.quill.context.sql.idiom

import zio.quill._
import zio.quill.idiom.StringToken

class MySQL5DialectSpec extends AbstractMySQLDialectSpec {
  lazy val ctx = new SqlMirrorContext(MySQL5Dialect, Literal) with TestEntities

  import ctx._

  "delete is without table alias" in {
    val q = quote {
      qr1.filter(t => t.i == 999).delete
    }
    ctx.run(q).string mustEqual
      "DELETE FROM TestEntity WHERE i = 999"
  }
}
