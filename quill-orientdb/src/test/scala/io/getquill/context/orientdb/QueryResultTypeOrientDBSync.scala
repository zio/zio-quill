package io.getquill.context.orientdb

import io.getquill.Spec

class QueryResultTypeOrientDBSync extends Spec {

  case class OrderTestEntity(id: Int, i: Int)

  val entries = List(
    OrderTestEntity(1, 1),
    OrderTestEntity(2, 2),
    OrderTestEntity(3, 3)
  )

  override protected def beforeAll(): Unit = {
    val ctx = orientdb.testSyncDB
    import ctx._
    ctx.run(quote(query[OrderTestEntity].delete))
    entries.foreach(e => ctx.run(quote { query[OrderTestEntity].insert(lift(e)) }))
  }

  "return list" - {
    "select" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      ctx.run(quote(query[OrderTestEntity])) must contain theSameElementsAs (entries)
      ctx.close()
    }
    "map" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      ctx.run(quote(query[OrderTestEntity].map(_.id))) must contain theSameElementsAs (entries.map(_.id))
      ctx.close()
    }
    "filter" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      ctx.run(quote(query[OrderTestEntity].filter(_.id == 1))) mustEqual entries.take(1)
      ctx.close()
    }
    "withFilter" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      ctx.run(quote(query[OrderTestEntity].withFilter(_.id == 1))) mustEqual entries.take(1)
      ctx.close()
    }
    "sortBy" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      ctx.run(quote(query[OrderTestEntity].filter(_.id == 1).sortBy(_.i)(Ord.asc))) mustEqual entries.take(1)
      ctx.close()
    }
    "take" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      ctx.run(quote(query[OrderTestEntity].take(10))) must contain theSameElementsAs (entries)
      ctx.close()
    }
  }

  "return single result" - {
    "size" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      ctx.run(quote(query[OrderTestEntity].size)) mustEqual entries.size
    }
    "paramlize size" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      ctx.run(quote { query[OrderTestEntity].filter(_.id == lift(0)).size }) mustEqual 0
    }
  }
}