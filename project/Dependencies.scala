import sbt.*
import sbt.Keys.*

object Version {
  val zio = "2.0.22"
}

sealed trait ExcludeTests
object ExcludeTests {
  case object Exclude                extends ExcludeTests
  case object Include                extends ExcludeTests
  case class KeepSome(regex: String) extends ExcludeTests
}
