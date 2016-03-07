package io.getquill.sources

import io.getquill.Spec
import io.getquill.ast._

class ExtractEntityAndInsertActionSpec extends Spec {
  "Extract should work" in {
    val ast = Map(Insert(Entity("TestEntity", Some("test"), List(PropertyAlias("i", "'i"), PropertyAlias("o", "'o")), Some("i"))), Ident("o"), Ident("o"))
    val (entity, insert) = ExtractEntityAndInsertAction(ast)
    entity.isDefined mustBe true
    insert.isDefined mustBe true
    entity.get.generated mustBe Some("i")
  }
}
