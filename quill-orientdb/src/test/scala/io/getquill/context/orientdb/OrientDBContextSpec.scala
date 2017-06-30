package io.getquill.context.orientdb

import io.getquill.Spec

class OrientDBContextSpec extends Spec {

  "run non-batched select" - {

    "async" in {
      val ctx = orientdb.testAsyncDB
      import ctx._
      case class TestEntity(id: Int, s: String, i: Int, l: Long, o: Int)
      val select = quote {
        query[TestEntity].filter(_.id == lift(1))
      }
      ctx.run(select).get() mustEqual List()
    }
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
}