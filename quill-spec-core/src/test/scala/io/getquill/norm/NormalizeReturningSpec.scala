package io.getquill.norm

import io.getquill.ReturnAction.{ ReturnColumns, ReturnRecord }
import io.getquill.context.mirror.Row
import io.getquill.{ MirrorIdiomReturningSingle, MirrorIdiomReturningMulti, Spec, testContext }
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
      r.string mustEqual """querySchema("Entity").insert(v => v.id -> ?, v => v.emb.id -> ?).returning((p) => p.id)"""
      r.prepareRow mustEqual Row(1, 2)
      r.returningBehavior mustEqual ReturnRecord
    }
    "when returning parent col - single - returning should not compile" in testContext.withDialect(MirrorIdiomReturningSingle) { ctx =>
      "ctx.run(query[Entity].insert(lift(e)).returning(p => p.id))" mustNot compile
    }
    "when returning parent col - single - returning generated" in testContext.withDialect(MirrorIdiomReturningSingle) { ctx =>
      import ctx._
      val r = ctx.run(query[Entity].insert(lift(e)).returningGenerated(p => p.id))
      r.string mustEqual """querySchema("Entity").insert(v => v.emb.id -> ?).returningGenerated((p) => p.id)"""
      r.prepareRow mustEqual Row(2)
      r.returningBehavior mustEqual ReturnColumns(List("id"))
    }
    "when returning parent col - multi - returning (supported)" in testContext.withDialect(MirrorIdiomReturningMulti) { ctx =>
      import ctx._
      val r = ctx.run(query[Entity].insert(lift(e)).returning(p => p.id))
      r.string mustEqual """querySchema("Entity").insert(v => v.id -> ?, v => v.emb.id -> ?).returning((p) => p.id)"""
      r.prepareRow mustEqual Row(1, 2)
      r.returningBehavior mustEqual ReturnColumns(List("id"))
    }
    "when returningGenerated parent col" in {
      val r = testContext.run(q.returningGenerated(p => p.id))
      r.string mustEqual """querySchema("Entity").insert(v => v.emb.id -> ?).returningGenerated((p) => p.id)"""
      r.prepareRow mustEqual Row(2)
      r.returningBehavior mustEqual ReturnRecord
    }

    "when returning embedded col" in {
      val r = testContext.run(q.returning(p => p.emb.id))
      r.string mustEqual """querySchema("Entity").insert(v => v.id -> ?, v => v.emb.id -> ?).returning((p) => p.emb.id)"""
      r.prepareRow mustEqual Row(1, 2)
      r.returningBehavior mustEqual ReturnRecord
    }
    "when returningGenerated embedded col" in {
      val r = testContext.run(q.returningGenerated(p => p.emb.id))
      r.string mustEqual """querySchema("Entity").insert(v => v.id -> ?).returningGenerated((p) => p.emb.id)"""
      r.prepareRow mustEqual Row(1)
      r.returningBehavior mustEqual ReturnRecord
    }

    "when returning embedded col - single" in testContext.withDialect(MirrorIdiomReturningSingle) { ctx =>
      import ctx._
      val r = ctx.run(query[Entity].insert(lift(e)).returningGenerated(p => p.emb.id))
      r.string mustEqual """querySchema("Entity").insert(v => v.id -> ?).returningGenerated((p) => p.emb.id)"""
      r.prepareRow mustEqual Row(1)
      // As of #1489 the Idiom now decides how to tokenize a `returning` clause when for MirrorIdiom is `emb.id`
      // since the mirror idiom specifically does not parse out embedded objects.
      r.returningBehavior mustEqual ReturnColumns(List("emb.id"))
    }
  }

}
