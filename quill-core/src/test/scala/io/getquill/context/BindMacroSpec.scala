package io.getquill.context

import io.getquill.context.mirror.{ MirrorSession, Row }
import io.getquill.testContext._
import io.getquill.{ Spec, testContext }

class BindMacroSpec extends Spec {

  val session = MirrorSession("test")

  "binds non-batched action" - {
    "normal" in {
      val q = quote {
        qr1.delete
      }
      val r = testContext.prepare(q)
      r(session) mustEqual Row()
    }
    "scalar lifting" in {
      val q = quote {
        qr1.insert(t => t.i -> lift(1))
      }
      val r = testContext.prepare(q)
      r(session) mustEqual Row(1)
    }
    "case class lifting" in {
      val q = quote {
        qr1.insert(lift(TestEntity("s", 1, 2L, None)))
      }
      val r = testContext.prepare(q)
      r(session) mustEqual Row("s", 1, 2L, None)
    }
    "nested case class lifting" in {
      val q = quote {
        (t: TestEntity) => qr1.insert(t)
      }
      val r = testContext.prepare(q(lift(TestEntity("s", 1, 2L, None))))
      r(session) mustEqual Row("s", 1, 2L, None)
    }
    "returning value" in {
      val q = quote {
        qr1.insert(t => t.i -> 1).returning(t => t.l)
      }
      val r = testContext.prepare(q)
      r(session) mustEqual Row()
    }
    "scalar lifting + returning value" in {
      val q = quote {
        qr1.insert(t => t.i -> lift(1)).returning(t => t.l)
      }
      val r = testContext.prepare(q)
      r(session) mustEqual Row(1)
    }
    "case class lifting + returning value" in {
      val q = quote {
        qr1.insert(lift(TestEntity("s", 1, 2L, None))).returning(t => t.l)
      }
      val r = testContext.prepare(q)
      r(session) mustEqual Row("s", 1, 2, None)
    }
    "scalar lifting + returning generated value" in {
      val q = quote {
        qr1.insert(t => t.i -> lift(1)).returningGenerated(t => t.l)
      }
      val r = testContext.prepare(q)
      r(session) mustEqual Row(1)
    }
    "case class lifting + returning generated value" in {
      val q = quote {
        qr1.insert(lift(TestEntity("s", 1, 2L, None))).returningGenerated(t => t.l)
      }
      val r = testContext.prepare(q)
      r(session) mustEqual Row("s", 1, None)
    }
  }

  "binds batched action" - {

    val entities = List(
      TestEntity("s1", 2, 3L, Some(4)),
      TestEntity("s5", 6, 7L, Some(8))
    )

    "scalar" in {
      val insert = quote {
        (p: Int) => qr1.insert(t => t.i -> p)
      }
      val q = quote {
        liftQuery(List(1, 2)).foreach((p: Int) => insert(p))
      }
      val r = testContext.prepare(q)
      r(session) mustEqual List(
        Row(1), Row(2)
      )
    }
    "case class" in {
      val q = quote {
        liftQuery(entities).foreach(p => qr1.insert(p))
      }
      val r = testContext.prepare(q)
      r(session) mustEqual List(Row("s1", 2, 3L, Some(4)), Row("s5", 6, 7L, Some(8)))
    }
    "case class + nested action" in {
      val nested = quote {
        (p: TestEntity) => qr1.insert(p)
      }
      val q = quote {
        liftQuery(entities).foreach(p => nested(p))
      }
      val r = testContext.prepare(q)
      r(session) mustEqual List(
        Row("s1", 2, 3L, Some(4)),
        Row("s5", 6, 7L, Some(8))
      )
    }
    "tuple + case class + nested action" in {
      val nested = quote {
        (s: String, p: TestEntity) => qr1.filter(t => t.s == s).update(p)
      }
      val q = quote {
        liftQuery(entities).foreach(p => nested(lift("s"), p))
      }
      val r = testContext.prepare(q)
      r(session) mustEqual List(
        Row("s", "s1", 2, 3L, Some(4)),
        Row("s", "s5", 6, 7L, Some(8))
      )
    }
    "zipWithIndex" in {
      val nested = quote {
        (e: TestEntity, i: Int) => qr1.filter(t => t.i == i).update(e)
      }
      val q = quote {
        liftQuery(entities.zipWithIndex).foreach(p => nested(p._1, p._2))
      }
      val r = testContext.prepare(q)
      r(session) mustEqual List(Row(0, "s1", 2, 3, Some(4)), Row(1, "s5", 6, 7, Some(8)))
    }
    "scalar + returning" in {
      val insert = quote {
        (p: Int) => qr1.insert(t => t.i -> p).returning(t => t.l)
      }
      val q = quote {
        liftQuery(List(1, 2)).foreach((p: Int) => insert(p))
      }
      val r = testContext.prepare(q)
      r(session) mustEqual List(Row(1), Row(2))
    }
    "case class + returning" in {
      val q = quote {
        liftQuery(entities).foreach(p => qr1.insert(p).returning(t => t.l))
      }
      val r = testContext.prepare(q)
      r(session) mustEqual List(Row("s1", 2, 3L, Some(4)), Row("s5", 6, 7L, Some(8)))
    }
    "case class + returning generated" in {
      val q = quote {
        liftQuery(entities).foreach(p => qr1.insert(p).returningGenerated(t => t.l))
      }
      val r = testContext.prepare(q)
      r(session) mustEqual List(Row("s1", 2, Some(4)), Row("s5", 6, Some(8)))
    }
    "case class + returning + nested action" in {
      val insert = quote {
        (p: TestEntity) => qr1.insert(p).returning(t => t.l)
      }
      val r = testContext.prepare(liftQuery(entities).foreach(p => insert(p)))
      r(session) mustEqual List(Row("s1", 2, 3L, Some(4)), Row("s5", 6, 7L, Some(8)))
    }
    "case class + returning generated + nested action" in {
      val insert = quote {
        (p: TestEntity) => qr1.insert(p).returningGenerated(t => t.l)
      }
      val r = testContext.prepare(liftQuery(entities).foreach(p => insert(p)))
      r(session) mustEqual List(Row("s1", 2, Some(4)), Row("s5", 6, Some(8)))
    }
  }
}
