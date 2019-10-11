package io.getquill.context.jdbc.postgres

import io.getquill.Spec

class PrettyPrintingSpec extends Spec {

  val context = testContext
  import testContext._

  case class Person(name: String, age: Int)

  "pretty prints query when enabled" in {
    val q = quote { query[Person] }
    translate(q, true) mustEqual
      """SELECT
        |  x.name,
        |  x.age
        |FROM
        |  Person x""".stripMargin
  }

  "regular print when not enabled" in {
    val q = quote { query[Person] }
    translate(q) mustEqual "SELECT x.name, x.age FROM Person x"
  }
}
