package zio.quill.context.sql

import zio.quill.{Literal, MirrorSqlDialect, SqlMirrorContext}
import zio.quill.base.Spec
import zio.quill.util.UntypecheckLiteral

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

  "c.untypecheck still turns a widened literal back into an Int on Scala 2.13.18+" in {
    val affectedVersion = scala.util.Properties.versionNumberString.split('.').toList match {
      case "2" :: "13" :: patch :: Nil => patch.toInt >= 18
      case _                           => false
    }
    if (affectedVersion)
      withClue(
        "https://github.com/scala/bug/issues/13208 is fixed, so ascribeLiteral in Parsing.scala " +
          "(https://github.com/zio/zio-quill/pull/3479) and this test can be removed:"
      ) {
        UntypecheckLiteral(10000) mustBe an[Integer]
      }
  }
}
