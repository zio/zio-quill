package io.getquill

import io.getquill.testSource._
import io.getquill.sources.mirror._

class QueryProbingSpec extends Spec {

  "fails if the source can't be resolved at compile time" in {
    object s extends MirrorSource with QueryProbing
    "buggySource.run(qr1)" mustNot compile
  }

  "doesn't warn if query probing is disabled and the source can't be resolved at compile time" in {
    object s extends TestSourceTemplate
    import s._
    s.run(qr1.delete)
    ()
  }

  "fails compilation if the query probing fails" - {
    case class Fail()
    "object source" in {
      "testSource.run(query[Fail].delete)" mustNot compile
    }
    "class source" in {
      def test(s: MirrorSourceTemplateWithQueryProbing) =
        "testSource.run(query[Fail].delete)" mustNot compile
    }
  }
}
