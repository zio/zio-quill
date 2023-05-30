import sbt._
import sbt.Keys._

object Version {
  val zio = "2.0.12"
}

sealed trait ExcludeTests
object ExcludeTests {
  case object Exclude extends ExcludeTests
  case object Include extends ExcludeTests
  case class KeepSome(regex: String) extends ExcludeTests
}
