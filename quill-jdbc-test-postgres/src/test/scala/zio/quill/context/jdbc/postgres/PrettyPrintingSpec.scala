package zio.quill.context.jdbc.postgres

import zio.quill.base.Spec
import zio.quill.context.TranslateOptions

class PrettyPrintingSpec extends Spec {

  val context = testContext
  import testContext._

  case class Person(name: String, age: Int)

  "pretty prints query when enabled" in {
    val q = quote(query[Person])
    translate(q, TranslateOptions(prettyPrint = true)) mustEqual
      """SELECT
        |  x.name,
        |  x.age
        |FROM
        |  Person x""".stripMargin
  }

  "regular print when not enabled" in {
    val q = quote(query[Person])
    translate(q) mustEqual "SELECT x.name, x.age FROM Person x"
  }
}
