package zio.quill.context.cassandra

import zio.quill.MappedEncoding
import zio.quill.base.Spec
import org.scalatest.BeforeAndAfterEach

trait CollectionsSpec extends Spec with BeforeAndAfterEach {
  case class StrWrap(x: String)
  implicit val encodeStrWrap = MappedEncoding[StrWrap, String](_.x)
  implicit val decodeStrWrap = MappedEncoding[String, StrWrap](StrWrap.apply)

  case class IntWrap(x: Int)
  implicit val encodeIntWrap = MappedEncoding[IntWrap, Int](_.x)
  implicit val decodeIntWrap = MappedEncoding[Int, IntWrap](IntWrap.apply)
}
