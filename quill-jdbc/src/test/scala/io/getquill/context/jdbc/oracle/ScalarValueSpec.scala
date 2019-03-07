package io.getquill.context.jdbc.oracle

import io.getquill.Spec

class ScalarValueSpec extends Spec {

  val context = testContext
  import testContext._

  "Simple Scalar Select" in {
    context.run(1) mustEqual 1
  }

  "Multi Scalar Select" in {
    context.run(quote(1 + quote(1))) mustEqual 2
  }

  "Multi Scalar Select with Infix" in {
    context.run("foo" + infix"""'bar'""".as[String]) mustEqual "foobar"
  }
}