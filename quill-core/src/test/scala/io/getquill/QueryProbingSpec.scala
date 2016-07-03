package io.getquill

import io.getquill.context.mirror.MirrorContext
import io.getquill.context.mirror.MirrorContextTemplateWithQueryProbing

class QueryProbingSpec extends Spec {

  "fails if the context can't be resolved at compile time" in {
    object c extends MirrorContext with QueryProbing
    "c.run(qr1)" mustNot compile
  }

  "doesn't warn if query probing is disabled and the context can't be resolved at compile time" in {
    object c extends TestContextTemplate
    import c._
    c.run(qr1.delete)
    ()
  }

  "fails compilation if the query probing fails" - {
    case class Fail()
    "object context" in {
      "testContext.run(query[Fail].delete)" mustNot compile
    }
    "class context" in {
      def test(s: MirrorContextTemplateWithQueryProbing) =
        "testContext.run(query[Fail].delete)" mustNot compile
    }
  }
}
