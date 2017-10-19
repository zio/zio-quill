package io.getquill.context.orientdb

import io.getquill.Spec

class OrientDBContextSpec extends Spec {

  "run non-batched select" - {
    "sync" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      case class TestEntity(id: Int, s: String, i: Int, l: Long, o: Int)
      val select = quote {
        query[TestEntity].filter(_.id == lift(1))
      }
      ctx.run(select) mustEqual List()
    }
  }

  "run non-batched action" in {
    val ctx = orientdb.testSyncDB
    import ctx._
    case class TestEntity(id: Int, s: String, i: Int, l: Long, o: Int)
    val update = quote {
      query[TestEntity].filter(_.id == lift(1)).update(_.i -> lift(1))
    }
    ctx.run(update) mustEqual (())
  }

  "performIO" in {
    val ctx = orientdb.testSyncDB
    import ctx._
    case class TestEntity(id: Int)
    performIO(runIO(quote(query[TestEntity].filter(_.id == lift(1)))).transactional)
  }

  "fail on returning" in {
    val ctx = orientdb.testSyncDB
    import ctx._
    val e: Extractor[Int] = (_) => 1
    intercept[IllegalStateException](executeActionReturning("", (x) => (Nil, x), e, "")).getMessage mustBe
      intercept[IllegalStateException](executeBatchActionReturning(Nil, e)).getMessage
  }

  "probe" in {
    orientdb.testSyncDB.probe("").toOption mustBe defined
  }
}