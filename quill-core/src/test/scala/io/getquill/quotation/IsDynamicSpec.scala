package io.getquill.quotation

import io.getquill.Spec
import io.getquill.testSource
import io.getquill.ast._

class IsDynamicSpec extends Spec {

  import testSource._

  "detects if the quotation has dynamic parts" - {
    "true" - {
      "fully dynamic" in {
        IsDynamic(Dynamic(1)) mustEqual true
      }
      "partially dynamic" in {
        IsDynamic(Property(Dynamic(1), "a")) mustEqual true
      }
    }
    "false" in {
      IsDynamic(qr1.ast) mustEqual false
    }
  }
}
