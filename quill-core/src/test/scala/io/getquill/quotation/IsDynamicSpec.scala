package io.getquill.quotation

import io.getquill.Spec
import io.getquill.ast.Dynamic
import io.getquill.ast.Property
import io.getquill.testContext.qr1
import io.getquill.testContext.qr5

class IsDynamicSpec extends Spec {

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
    "false when using CaseClass" in {
      IsDynamic(qr5.ast) mustEqual false
    }
  }
}
