package io.getquill.norm

import io.getquill.context.mirror.Row
import io.getquill.{ Spec, testContext }
import io.getquill.testContext._

class NormalizeReturningSpec extends Spec {

  "do not remove assignment if embedded has columns with the same name" - {

    case class EmbEntity(id: Int) extends Embedded
    case class Entity(id: Int, emb: EmbEntity)

    val e = Entity(1, EmbEntity(2))
    val q = quote {
      query[Entity].insert(lift(e))
    }

    "when returning parent col" in {
      val r = testContext.run(q.returning(p => p.id))
      r.string mustEqual """querySchema("Entity").insert(v => v.emb.id -> ?).returning((p) => p.id)"""
      r.prepareRow mustEqual Row(2)
      r.returningColumn mustEqual "id"
    }

    "when returning embedded col" in {
      val r = testContext.run(q.returning(p => p.emb.id))
      r.string mustEqual """querySchema("Entity").insert(v => v.id -> ?).returning((p) => p.emb.id)"""
      r.prepareRow mustEqual Row(1)
      r.returningColumn mustEqual "id"
    }
  }

}
