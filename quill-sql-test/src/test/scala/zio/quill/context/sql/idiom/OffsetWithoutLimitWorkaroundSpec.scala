package zio.quill.context.sql.idiom

import zio.quill.Literal
import zio.quill.SqlMirrorContext
import zio.quill.TestEntities
import zio.quill.MySQLDialect
import zio.quill.base.Spec
import scala.util.Try

class OffsetWithoutLimitWorkaroundSpec extends Spec {

  val ctx = new SqlMirrorContext(MySQLDialect, Literal) with TestEntities {
    override def probe(statement: String) =
      Try {
        statement mustEqual
          "PREPARE p603247403 FROM 'SELECT x.s, x.i, x.l, x.o FROM TestEntity x LIMIT 18446744073709551610 OFFSET 1'"
      }
  }
  import ctx._

  "creates a synthetic limit" in {
    val q = quote {
      qr1.drop(1)
    }
    ctx.run(q)
    ()
  }
}
