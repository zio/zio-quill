package zio.quill.context.sql

import zio.quill.{Literal, MirrorSqlDialect, SqlMirrorContext}
import zio.quill.base.Spec

class LiftWidenedLiteralSpec extends Spec {

  val context = new SqlMirrorContext(MirrorSqlDialect, Literal)
  import context._

  case class Product(id: Long)

  val byId = quote((id: Long) => query[Product].filter(_.id == id))

  "a lifted Int literal passed as a Long is encoded as a Long" in {
    context.run(byId(lift(10000))).prepareRow.data.map(_.getClass) mustEqual List(classOf[java.lang.Long])
  }

  final val constantId = 10000

  "a lifted Int constant passed as a Long is encoded as a Long" in {
    context.run(byId(lift(constantId))).prepareRow.data.map(_.getClass) mustEqual List(classOf[java.lang.Long])
  }
}
