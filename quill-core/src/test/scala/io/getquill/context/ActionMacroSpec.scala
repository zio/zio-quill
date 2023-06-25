package io.getquill.context

import io.getquill.ReturnAction.{ReturnColumns, ReturnRecord}
import io.getquill.base.Spec
import io.getquill.context.mirror.Row
import io.getquill.MirrorContexts.testContext._
import io.getquill.{MirrorIdiomReturningMulti, MirrorIdiomReturningSingle}
import io.getquill.MirrorContexts.testContext

class ActionMacroSpec extends Spec {

  "runs non-batched action" - {
    "normal" in {
      val q = quote {
        qr1.delete
      }
      val r = testContext.run(q)
      r.string mustEqual """querySchema("TestEntity").delete"""
      r.prepareRow mustEqual Row()
    }
    "scalar lifting" in {
      val q = quote {
        qr1.insert(t => t.i -> lift(1))
      }
      val r = testContext.run(q)
      r.string mustEqual """querySchema("TestEntity").insert(t => t.i -> ?)"""
      r.prepareRow mustEqual Row(1)
    }
    "case class lifting" in {
      val q = quote {
        qr1.insertValue(lift(TestEntity("s", 1, 2L, None, true)))
      }
      val r = testContext.run(q)
      r.string mustEqual """querySchema("TestEntity").insert(v => v.s -> ?, v => v.i -> ?, v => v.l -> ?, v => v.o -> ?, v => v.b -> ?)"""
      r.prepareRow mustEqual Row("s", 1, 2L, None, true)
    }
    "nested case class lifting" in {
      val q = quote { t: TestEntity =>
        qr1.insertValue(t)
      }
      val r = testContext.run(q(lift(TestEntity("s", 1, 2L, None, true))))
      r.string mustEqual """querySchema("TestEntity").insert(v => v.s -> ?, v => v.i -> ?, v => v.l -> ?, v => v.o -> ?, v => v.b -> ?)"""
      r.prepareRow mustEqual Row("s", 1, 2L, None, true)
    }

    "insert returning" - {
      "returning value" in {
        val q = quote {
          qr1.insert(t => t.i -> 1).returning(t => t.l)
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").insert(t => t.i -> 1).returning((t) => t.l)"""
        r.prepareRow mustEqual Row()
        r.returningBehavior mustEqual ReturnRecord
      }
      "returning value - with single - should not compile" in testContext.withDialect(MirrorIdiomReturningSingle) {
        ctx =>
          "import ctx._; ctx.run(qr1.insert(t => t.i -> 1).returning(t => t.l))" mustNot compile
      }
      "returning value - with multi" in testContext.withDialect(MirrorIdiomReturningMulti) { ctx =>
        import ctx._
        val q = quote {
          qr1.insert(t => t.i -> 1).returning(t => t.l)
        }
        val r = ctx.run(q)
        r.string mustEqual """querySchema("TestEntity").insert(t => t.i -> 1).returning((t) => t.l)"""
        r.prepareRow mustEqual Row()
        r.returningBehavior mustEqual ReturnColumns(List("l"))
      }
      "returning generated value" in {
        val q = quote {
          qr1.insert(t => t.i -> 1).returningGenerated(t => t.l)
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").insert(t => t.i -> 1).returningGenerated((t) => t.l)"""
        r.prepareRow mustEqual Row()
        r.returningBehavior mustEqual ReturnRecord
      }
      "returning generated value - with single" in testContext.withDialect(MirrorIdiomReturningSingle) { ctx =>
        import ctx._
        val q = quote {
          qr1.insert(t => t.i -> 1).returningGenerated(t => t.l)
        }
        val r = ctx.run(q)
        r.string mustEqual """querySchema("TestEntity").insert(t => t.i -> 1).returningGenerated((t) => t.l)"""
        r.prepareRow mustEqual Row()
        r.returningBehavior mustEqual ReturnColumns(List("l"))
      }
      "returning generated value - with single - multi should not compile" in testContext.withDialect(
        MirrorIdiomReturningSingle
      ) { ctx =>
        "import ctx._; ctx.run(qr1.insert(t => t.i -> 1).returningGenerated(t => (t.l, t.i))" mustNot compile
      }
      "returning generated value - with multi" in testContext.withDialect(MirrorIdiomReturningMulti) { ctx =>
        import ctx._
        val q = quote {
          qr1.insert(t => t.i -> 1).returningGenerated(t => (t.l, t.s))
        }
        val r = ctx.run(q)
        r.string mustEqual """querySchema("TestEntity").insert(t => t.i -> 1).returningGenerated((t) => (t.l, t.s))"""
        r.prepareRow mustEqual Row()
        r.returningBehavior mustEqual ReturnColumns(List("l", "s"))
      }
      "returning generated value - with multi - operation in clause should not compile" in testContext.withDialect(
        MirrorIdiomReturningMulti
      ) { ctx =>
        "import ctx._; ctx.run(qr1.insert(t => t.i -> 1).returningGenerated(t => (t.l, t.i + 1)))" mustNot compile
      }
      "returning generated value - with multi - single" in testContext.withDialect(MirrorIdiomReturningMulti) { ctx =>
        import ctx._
        val q = quote {
          qr1.insert(t => t.i -> 1).returningGenerated(t => t.l)
        }
        val r = ctx.run(q)
        r.string mustEqual """querySchema("TestEntity").insert(t => t.i -> 1).returningGenerated((t) => t.l)"""
        r.prepareRow mustEqual Row()
        r.returningBehavior mustEqual ReturnColumns(List("l"))
      }
      "scalar lifting + returning value" in {
        val q = quote {
          qr1.insert(t => t.i -> lift(1)).returning(t => t.l)
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").insert(t => t.i -> ?).returning((t) => t.l)"""
        r.prepareRow mustEqual Row(1)
        r.returningBehavior mustEqual ReturnRecord
      }
      "case class lifting + returning value" in {
        val q = quote {
          qr1.insertValue(lift(TestEntity("s", 1, 2L, None, true))).returning(t => t.l)
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").insert(v => v.s -> ?, v => v.i -> ?, v => v.l -> ?, v => v.o -> ?, v => v.b -> ?).returning((t) => t.l)"""
        r.prepareRow mustEqual Row("s", 1, 2, None, true)
        r.returningBehavior mustEqual ReturnRecord
      }
      "case class lifting + returning generated value" in {
        val q = quote {
          qr1.insertValue(lift(TestEntity("s", 1, 2L, None, true))).returningGenerated(t => t.l)
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").insert(v => v.s -> ?, v => v.i -> ?, v => v.o -> ?, v => v.b -> ?).returningGenerated((t) => t.l)"""
        r.prepareRow mustEqual Row("s", 1, None, true)
        r.returningBehavior mustEqual ReturnRecord
      }
      "case class lifting + returning multi value" in {
        val q = quote {
          qr1.insertValue(lift(TestEntity("s", 1, 2L, None, true))).returning(t => (t.l, t.i, t.b))
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").insert(v => v.s -> ?, v => v.i -> ?, v => v.l -> ?, v => v.o -> ?, v => v.b -> ?).returning((t) => (t.l, t.i, t.b))"""
        r.prepareRow mustEqual Row("s", 1, 2, None, true)
        r.returningBehavior mustEqual ReturnRecord
      }
      "case class lifting + returning generated multi value" in {
        val q = quote {
          qr1.insertValue(lift(TestEntity("s", 1, 2L, None, true))).returningGenerated(t => (t.l, t.i, t.b))
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").insert(v => v.s -> ?, v => v.o -> ?).returningGenerated((t) => (t.l, t.i, t.b))"""
        r.prepareRow mustEqual Row("s", None)
        r.returningBehavior mustEqual ReturnRecord
      }
    }

    "update returning" - {
      "returning value" in {
        val q = quote {
          qr1.update(t => t.i -> 1).returning(t => t.l)
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").update(t => t.i -> 1).returning((t) => t.l)"""
        r.prepareRow mustEqual Row()
        r.returningBehavior mustEqual ReturnRecord
      }
      "returning value - with single - should not compile" in testContext.withDialect(MirrorIdiomReturningSingle) {
        ctx =>
          "import ctx._; ctx.run(qr1.update(t => t.i -> 1).returning(t => t.l))" mustNot compile
      }
      "returning value - with multi" in testContext.withDialect(MirrorIdiomReturningMulti) { ctx =>
        import ctx._
        val q = quote {
          qr1.update(t => t.i -> 1).returning(t => t.l)
        }
        val r = ctx.run(q)
        r.string mustEqual """querySchema("TestEntity").update(t => t.i -> 1).returning((t) => t.l)"""
        r.prepareRow mustEqual Row()
        r.returningBehavior mustEqual ReturnColumns(List("l"))
      }
      "scalar lifting + returning value" in {
        val q = quote {
          qr1.update(t => t.i -> lift(1)).returning(t => t.l)
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").update(t => t.i -> ?).returning((t) => t.l)"""
        r.prepareRow mustEqual Row(1)
        r.returningBehavior mustEqual ReturnRecord
      }
      "case class lifting + returning value" in {
        val q = quote {
          qr1.updateValue(lift(TestEntity("s", 1, 2L, None, true))).returning(t => t.l)
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").update(v => v.s -> ?, v => v.i -> ?, v => v.l -> ?, v => v.o -> ?, v => v.b -> ?).returning((t) => t.l)"""
        r.prepareRow mustEqual Row("s", 1, 2, None, true)
        r.returningBehavior mustEqual ReturnRecord
      }
      "case class lifting + returning multi value" in {
        val q = quote {
          qr1.updateValue(lift(TestEntity("s", 1, 2L, None, true))).returning(t => (t.l, t.i))
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").update(v => v.s -> ?, v => v.i -> ?, v => v.l -> ?, v => v.o -> ?, v => v.b -> ?).returning((t) => (t.l, t.i))"""
        r.prepareRow mustEqual Row("s", 1, 2, None, true)
        r.returningBehavior mustEqual ReturnRecord
      }
    }

    "delete returning" - {
      "returning value" in {
        val q = quote {
          qr1.delete.returning(t => t.l)
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").delete.returning((t) => t.l)"""
        r.prepareRow mustEqual Row()
        r.returningBehavior mustEqual ReturnRecord
      }
      "returning value - with single - should not compile" in testContext.withDialect(MirrorIdiomReturningSingle) {
        ctx =>
          "import ctx._; ctx.delete.returning(t => t.l))" mustNot compile
      }
      "returning value - with multi" in testContext.withDialect(MirrorIdiomReturningMulti) { ctx =>
        import ctx._
        val q = quote {
          qr1.delete.returning(t => t.l)
        }
        val r = ctx.run(q)
        r.string mustEqual """querySchema("TestEntity").delete.returning((t) => t.l)"""
        r.prepareRow mustEqual Row()
        r.returningBehavior mustEqual ReturnColumns(List("l"))
      }
    }
  }

  "runs batched action" - { //
    val entities = List(
      TestEntity("s1", 2, 3L, Some(4), true),
      TestEntity("s5", 6, 7L, Some(8), false)
    )

    "scalar" in {
      val insert = quote { p: Int =>
        qr1.insert(t => t.i -> p)
      }
      val q = quote {
        liftQuery(List(1, 2)).foreach((p: Int) => insert(p))
      }
      val r = testContext.run(q)
      r.groups mustEqual List(
        """querySchema("TestEntity").insert(t => t.i -> ?)""" -> List(Row(1), Row(2))
      )
    }
    "case class" in {
      val q = quote {
        liftQuery(entities).foreach(p => qr1.insertValue(p))
      }
      val r = testContext.run(q)
      r.groups mustEqual List(
        """querySchema("TestEntity").insert(v => v.s -> ?, v => v.i -> ?, v => v.l -> ?, v => v.o -> ?, v => v.b -> ?)""" ->
          List(Row("s1", 2, 3L, Some(4), true), Row("s5", 6, 7L, Some(8), false))
      )
    }
    "case class + nested action" in {
      val nested = quote { p: TestEntity =>
        qr1.insertValue(p)
      }
      val q = quote {
        liftQuery(entities).foreach(p => nested(p))
      }
      val r = testContext.run(q)
      r.groups mustEqual List(
        """querySchema("TestEntity").insert(v => v.s -> ?, v => v.i -> ?, v => v.l -> ?, v => v.o -> ?, v => v.b -> ?)""" ->
          List(Row("s1", 2, 3L, Some(4), true), Row("s5", 6, 7L, Some(8), false))
      )
    }
    "tuple + case class + nested action" in {
      val nested = quote { (s: String, p: TestEntity) =>
        qr1.filter(t => t.s == s).updateValue(p)
      }
      val q = quote {
        liftQuery(entities).foreach(p => nested(lift("s"), p))
      }
      val r = testContext.run(q)
      r.groups mustEqual List(
        """querySchema("TestEntity").filter(t1 => t1.s == ?).update(v => v.s -> ?, v => v.i -> ?, v => v.l -> ?, v => v.o -> ?, v => v.b -> ?)""" ->
          List(Row("s", "s1", 2, 3L, Some(4), true), Row("s", "s5", 6, 7L, Some(8), false))
      )
    }
    "zipWithIndex" in {
      val nested = quote { (e: TestEntity, i: Int) =>
        qr1.filter(t => t.i == i).updateValue(e)
      }
      val q = quote {
        liftQuery(entities.zipWithIndex).foreach(p => nested(p._1, p._2))
      }
      val r = testContext.run(q)
      r.groups mustEqual List(
        """querySchema("TestEntity").filter(t1 => t1.i == ?).update(v => v.s -> ?, v => v.i -> ?, v => v.l -> ?, v => v.o -> ?, v => v.b -> ?)""" ->
          List(Row(0, "s1", 2, 3, Some(4), true), Row(1, "s5", 6, 7, Some(8), false))
      )
    }
    "scalar + returning" in {
      val insert = quote { p: Int =>
        qr1.insert(t => t.i -> p).returning(t => t.l)
      }
      val q = quote {
        liftQuery(List(1, 2)).foreach((p: Int) => insert(p))
      }
      val r = testContext.run(q)
      r.groups mustEqual List(
        (
          """querySchema("TestEntity").insert(t => t.i -> ?).returning((t) => t.l)""",
          ReturnRecord,
          List(Row(1), Row(2))
        )
      )
    }
    "case class + returning" in {
      val q = quote {
        liftQuery(entities).foreach(p => qr1.insertValue(p).returning(t => t.l))
      }
      val r = testContext.run(q)
      r.groups mustEqual List(
        (
          """querySchema("TestEntity").insert(v => v.s -> ?, v => v.i -> ?, v => v.l -> ?, v => v.o -> ?, v => v.b -> ?).returning((t) => t.l)""",
          ReturnRecord,
          List(Row("s1", 2, 3, Some(4), true), Row("s5", 6, 7, Some(8), false))
        )
      )
    }
    "case class + returning generated" in {
      val q = quote {
        liftQuery(entities).foreach(p => qr1.insertValue(p).returningGenerated(t => t.l))
      }
      val r = testContext.run(q)
      r.groups mustEqual List(
        (
          """querySchema("TestEntity").insert(v => v.s -> ?, v => v.i -> ?, v => v.o -> ?, v => v.b -> ?).returningGenerated((t) => t.l)""",
          ReturnRecord,
          List(Row("s1", 2, Some(4), true), Row("s5", 6, Some(8), false))
        )
      )
    }
    "case class + returning + nested action" in {
      val insert = quote { p: TestEntity =>
        qr1.insertValue(p).returning(t => t.l)
      }
      val r = testContext.run(liftQuery(entities).foreach(p => insert(p)))
      r.groups mustEqual List(
        (
          """querySchema("TestEntity").insert(v => v.s -> ?, v => v.i -> ?, v => v.l -> ?, v => v.o -> ?, v => v.b -> ?).returning((t) => t.l)""",
          ReturnRecord,
          List(Row("s1", 2, 3, Some(4), true), Row("s5", 6, 7, Some(8), false))
        )
      )
    }
    "case class + returning generated + nested action" in {
      val insert = quote { (p: TestEntity) =>
        qr1.insertValue(p).returningGenerated(t => t.l)
      }
      val r = testContext.run(liftQuery(entities).foreach(p => insert(p)))
      r.groups mustEqual List(
        (
          """querySchema("TestEntity").insert(v => v.s -> ?, v => v.i -> ?, v => v.o -> ?, v => v.b -> ?).returningGenerated((t) => t.l)""",
          ReturnRecord,
          List(Row("s1", 2, Some(4), true), Row("s5", 6, Some(8), false))
        )
      )
    }
  }

  "translate non-batched action" - {
    "normal" in {
      val q = quote {
        qr1.delete
      }
      testContext.translate(q) mustEqual
        """querySchema("TestEntity").delete"""
    }
    "scalar lifting" in {
      val q = quote {
        qr1.insert(t => t.i -> lift(1))
      }
      testContext.translate(q) mustEqual
        """querySchema("TestEntity").insert(t => t.i -> 1)"""
    }
    "case class lifting" in {
      val q = quote {
        qr1.insertValue(lift(TestEntity("s", 1, 2L, None, true)))
      }
      testContext.translate(q) mustEqual
        """querySchema("TestEntity").insert(v => v.s -> 's', v => v.i -> 1, v => v.l -> 2, v => v.o -> null, v => v.b -> true)"""
    }
    "nested case class lifting" in {
      val q = quote { t: TestEntity =>
        qr1.insertValue(t)
      }
      testContext.translate(q(lift(TestEntity("s", 1, 2L, None, true)))) mustEqual
        """querySchema("TestEntity").insert(v => v.s -> 's', v => v.i -> 1, v => v.l -> 2, v => v.o -> null, v => v.b -> true)"""
    }
    "returning value" in {
      val q = quote {
        qr1.insert(t => t.i -> 1).returning(t => t.l)
      }
      testContext.translate(q) mustEqual
        """querySchema("TestEntity").insert(t => t.i -> 1).returning((t) => t.l)"""
    }
    "scalar lifting + returning value" in {
      val q = quote {
        qr1.insert(t => t.i -> lift(1)).returning(t => t.l)
      }
      testContext.translate(q) mustEqual
        """querySchema("TestEntity").insert(t => t.i -> 1).returning((t) => t.l)"""
    }
    "case class lifting + returning value" in {
      val q = quote {
        qr1.insertValue(lift(TestEntity("s", 1, 2L, None, true))).returning(t => t.l)
      }
      testContext.translate(q) mustEqual
        """querySchema("TestEntity").insert(v => v.s -> 's', v => v.i -> 1, v => v.l -> 2, v => v.o -> null, v => v.b -> true).returning((t) => t.l)"""
    }
    "case class lifting + returning generated value" in {
      val q = quote {
        qr1.insertValue(lift(TestEntity("s", 1, 2L, None, true))).returningGenerated(t => t.l)
      }
      testContext.translate(q) mustEqual
        """querySchema("TestEntity").insert(v => v.s -> 's', v => v.i -> 1, v => v.o -> null, v => v.b -> true).returningGenerated((t) => t.l)"""
    }
  }

  "translate batched action" - {

    val entities = List(
      TestEntity("s1", 2, 3L, Some(4), true),
      TestEntity("s5", 6, 7L, Some(8), false)
    )

    "scalar" in {
      val insert = quote { p: Int =>
        qr1.insert(t => t.i -> p)
      }
      val q = quote {
        liftQuery(List(1, 2)).foreach((p: Int) => insert(p))
      }
      testContext.translate(q) mustEqual List(
        """querySchema("TestEntity").insert(t => t.i -> 1)""",
        """querySchema("TestEntity").insert(t => t.i -> 2)"""
      )
    }
    "case class" in {
      val q = quote {
        liftQuery(entities).foreach(p => qr1.insertValue(p))
      }
      testContext.translate(q) mustEqual List(
        """querySchema("TestEntity").insert(v => v.s -> 's1', v => v.i -> 2, v => v.l -> 3, v => v.o -> 4, v => v.b -> true)""",
        """querySchema("TestEntity").insert(v => v.s -> 's5', v => v.i -> 6, v => v.l -> 7, v => v.o -> 8, v => v.b -> false)"""
      )
    }
    "case class + nested action" in {
      val nested = quote { p: TestEntity =>
        qr1.insertValue(p)
      }
      val q = quote {
        liftQuery(entities).foreach(p => nested(p))
      }
      testContext.translate(q) mustEqual List(
        """querySchema("TestEntity").insert(v => v.s -> 's1', v => v.i -> 2, v => v.l -> 3, v => v.o -> 4, v => v.b -> true)""",
        """querySchema("TestEntity").insert(v => v.s -> 's5', v => v.i -> 6, v => v.l -> 7, v => v.o -> 8, v => v.b -> false)"""
      )
    }
    "tuple + case class + nested action" in {
      val nested = quote { (s: String, p: TestEntity) =>
        qr1.filter(t => t.s == s).updateValue(p)
      }
      val q = quote {
        liftQuery(entities).foreach(p => nested(lift("s"), p))
      }
      testContext.translate(q) mustEqual List(
        """querySchema("TestEntity").filter(t1 => t1.s == 's').update(v => v.s -> 's1', v => v.i -> 2, v => v.l -> 3, v => v.o -> 4, v => v.b -> true)""",
        """querySchema("TestEntity").filter(t1 => t1.s == 's').update(v => v.s -> 's5', v => v.i -> 6, v => v.l -> 7, v => v.o -> 8, v => v.b -> false)"""
      )
    }
    "zipWithIndex" in {
      val nested = quote { (e: TestEntity, i: Int) =>
        qr1.filter(t => t.i == i).updateValue(e)
      }
      val q = quote {
        liftQuery(entities.zipWithIndex).foreach(p => nested(p._1, p._2))
      }
      testContext.translate(q) mustEqual List(
        """querySchema("TestEntity").filter(t1 => t1.i == 0).update(v => v.s -> 's1', v => v.i -> 2, v => v.l -> 3, v => v.o -> 4, v => v.b -> true)""",
        """querySchema("TestEntity").filter(t1 => t1.i == 1).update(v => v.s -> 's5', v => v.i -> 6, v => v.l -> 7, v => v.o -> 8, v => v.b -> false)"""
      )
    }
    "scalar + returning" in {
      val insert = quote { p: Int =>
        qr1.insert(t => t.i -> p).returning(t => t.l)
      }
      val q = quote {
        liftQuery(List(1, 2)).foreach((p: Int) => insert(p))
      }
      testContext.translate(q) mustEqual List(
        """querySchema("TestEntity").insert(t => t.i -> 1).returning((t) => t.l)""",
        """querySchema("TestEntity").insert(t => t.i -> 2).returning((t) => t.l)"""
      )
    }
    "case class + returning" in {
      val q = quote {
        liftQuery(entities).foreach(p => qr1.insertValue(p).returning(t => t.l))
      }
      testContext.translate(q) mustEqual List(
        """querySchema("TestEntity").insert(v => v.s -> 's1', v => v.i -> 2, v => v.l -> 3, v => v.o -> 4, v => v.b -> true).returning((t) => t.l)""",
        """querySchema("TestEntity").insert(v => v.s -> 's5', v => v.i -> 6, v => v.l -> 7, v => v.o -> 8, v => v.b -> false).returning((t) => t.l)"""
      )
    }
    "case class + returning + nested action" in {
      val insert = quote { p: TestEntity =>
        qr1.insertValue(p).returning(t => t.l)
      }
      testContext.translate(liftQuery(entities).foreach(p => insert(p))) mustEqual List(
        """querySchema("TestEntity").insert(v => v.s -> 's1', v => v.i -> 2, v => v.l -> 3, v => v.o -> 4, v => v.b -> true).returning((t) => t.l)""",
        """querySchema("TestEntity").insert(v => v.s -> 's5', v => v.i -> 6, v => v.l -> 7, v => v.o -> 8, v => v.b -> false).returning((t) => t.l)"""
      )
    }
    "case class + returning generated" in {
      val q = quote {
        liftQuery(entities).foreach(p => qr1.insertValue(p).returningGenerated(t => t.l))
      }
      testContext.translate(q) mustEqual List(
        """querySchema("TestEntity").insert(v => v.s -> 's1', v => v.i -> 2, v => v.o -> 4, v => v.b -> true).returningGenerated((t) => t.l)""",
        """querySchema("TestEntity").insert(v => v.s -> 's5', v => v.i -> 6, v => v.o -> 8, v => v.b -> false).returningGenerated((t) => t.l)"""
      )
    }
    "case class + returning generated + nested action" in {
      val insert = quote { p: TestEntity =>
        qr1.insertValue(p).returningGenerated(t => t.l)
      }
      testContext.translate(liftQuery(entities).foreach(p => insert(p))) mustEqual List(
        """querySchema("TestEntity").insert(v => v.s -> 's1', v => v.i -> 2, v => v.o -> 4, v => v.b -> true).returningGenerated((t) => t.l)""",
        """querySchema("TestEntity").insert(v => v.s -> 's5', v => v.i -> 6, v => v.o -> 8, v => v.b -> false).returningGenerated((t) => t.l)"""
      )
    }
  }
}
