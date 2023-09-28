package io.getquill.context.sql.base

import io.getquill.context.sql.SqlContext
import io.getquill.TestEntities
import io.getquill.base.Spec
import io.getquill.{ EntityQuery, Quoted }

trait OnConflictSpec extends Spec {
  val ctx: SqlContext[_, _] with TestEntities

  import ctx._

  object `onConflictIgnore` {
    val testQuery1, testQuery2 = quote {
      qr1.insertValue(lift(TestEntity("", 1, 0, None, true))).onConflictIgnore
    }
    val res1 = 1
    val res2 = 0

    val testQuery3: Quoted[EntityQuery[TestEntity]] = quote {
      qr1.filter(_.i == 1)
    }
    val res3: List[TestEntity] = List(TestEntity("", 1, 0, None, true))
  }

  object `onConflictIgnore(_.i)` {
    val name = "ON CONFLICT (...) DO NOTHING"
    val testQuery1, testQuery2 = quote {
      qr1.insertValue(lift(TestEntity("s", 2, 0, None, true))).onConflictIgnore(_.i)
    }
    val res1 = 1
    val res2 = 0

    val testQuery3 = quote {
      qr1.filter(_.i == 2)
    }
    val res3 = List(TestEntity("s", 2, 0, None, true))
  }

  abstract class onConflictUpdate(id: Int) {
    val e1: TestEntity = TestEntity("r1", id, 0, None, true)
    val e2: TestEntity = TestEntity("r2", id, 0, None, true)
    val e3: TestEntity = TestEntity("r3", id, 0, None, true)

    val res1, res2, res3 = 1

    val testQuery4 = quote {
      qr1.filter(_.i == lift(id))
    }
    val res4: List[TestEntity] = List(TestEntity("r1-r2-r3", id, 2, None, true))
  }

  object `onConflictUpdate((t, e) => ...)` extends onConflictUpdate(3) {
    def testQuery(e: TestEntity) = quote {
      qr1
        .insertValue(lift(e))
        .onConflictUpdate((t, e) => t.s -> (t.s + "-" + e.s), (t, _) => t.l -> (t.l + 1))
    }
  }

  object `onConflictUpdate(_.i)((t, e) => ...)` extends onConflictUpdate(4) {
    def testQuery(e: TestEntity) = quote {
      qr1
        .insertValue(lift(e))
        .onConflictUpdate(_.i)((t, e) => t.s -> (t.s + "-" + e.s), (t, _) => t.l -> (t.l + 1))
    }
  }

}
