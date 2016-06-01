package io.getquill.sources

import io.getquill.Spec
import io.getquill.testSource._

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
