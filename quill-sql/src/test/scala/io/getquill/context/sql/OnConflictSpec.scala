package io.getquill.context.sql

import io.getquill.{ Spec, TestEntities }

trait OnConflictSpec extends Spec {
  val ctx: SqlContext[_, _] with TestEntities
  import ctx._

  object `onConflictIgnore` {
    val testQuery1, testQuery2 = quote {
      qr1.insert(lift(TestEntity("", 1, 0, None))).onConflictIgnore
    }
    val res1 = 1
    val res2 = 0

    val testQuery3 = quote {
      qr1.filter(_.i == 1)
    }
    val res3 = List(TestEntity("", 1, 0, None))
  }

  object `onConflictIgnore(_.i)` {
    val name = "ON CONFLICT (...) DO NOTHING"
    val testQuery1, testQuery2 = quote {
      qr1.insert(lift(TestEntity("s", 2, 0, None))).onConflictIgnore(_.i)
    }
    val res1 = 1
    val res2 = 0

    val testQuery3 = quote {
      qr1.filter(_.i == 2)
    }
    val res3 = List(TestEntity("s", 2, 0, None))
  }

  abstract class onConflictUpdate(id: Int) {
    val e1 = TestEntity("r1", id, 0, None)
    val e2 = TestEntity("r2", id, 0, None)
    val e3 = TestEntity("r3", id, 0, None)

    val res1, res2, res3 = 1

    val testQuery4 = quote {
      qr1.filter(_.i == lift(id))
    }
    val res4 = List(TestEntity("r1-r2-r3", id, 2, None))
  }

  object `onConflictUpdate((t, e) => ...)` extends onConflictUpdate(3) {
    def testQuery(e: TestEntity) = quote {
      qr1
        .insert(lift(e))
        .onConflictUpdate((t, e) => t.s -> (t.s + "-" + e.s), (t, _) => t.l -> (t.l + 1))
    }
  }

  object `onConflictUpdate(_.i)((t, e) => ...)` extends onConflictUpdate(4) {
    def testQuery(e: TestEntity) = quote {
      qr1
        .insert(lift(e))
        .onConflictUpdate(_.i)((t, e) => t.s -> (t.s + "-" + e.s), (t, _) => t.l -> (t.l + 1))
    }
  }

}
