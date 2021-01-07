package io.getquill.context.sql

import io.getquill.Spec

class PrettyPrintingSpec extends Spec {

  import testContext._

  case class Person(name: String, age: Int)

  "pretty print query when enabled" in {
    val prettyString = testContext.run(query[Person]).string(true)
    prettyString mustEqual
      """SELECT
        |  x.name,
        |  x.age
        |FROM
        |  Person x""".stripMargin
  }

  "regular print query when not enabled" in {
    val prettyString = testContext.run(query[Person]).string(false)
    prettyString mustEqual "SELECT x.name, x.age FROM Person x"
  }
}
