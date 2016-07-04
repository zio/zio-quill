package io.getquill.context

import io.getquill.Spec
import io.getquill.testContext.qr1
import io.getquill.testContext.quote
import io.getquill.testContext.unquote

class ExtractSchmeaAndInsertActionSpec extends Spec {

  "Extract should work" in {
    val q = quote {
      qr1.schema(_.entity("test").columns(_.i -> "'i", _.o -> "'i").generated(_.i)).insert
    }
    val (entity, insert) = ExtractEntityAndInsertAction(q.ast)
    entity.isDefined mustBe true
    insert.isDefined mustBe true
    entity.get.generated mustBe Some("i")
  }
}
