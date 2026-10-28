package zio.quill.context.cassandra.udt

import zio.quill.Udt
import zio.quill.base.Spec

trait UdtSpec extends Spec {
  case class Name(firstName: String, lastName: Option[String]) extends Udt
  case class Personal(
    number: Int,
    street: String,
    name: Name,
    optName: Option[Name],
    list: List[String],
    sets: Set[Int],
    map: Map[Int, String]
  ) extends Udt
}
