package io.getquill.context

import io.getquill.Spec
import io.getquill.ast.{ AssignedAction, Function, FunctionApply }
import io.getquill.context.mirror.Row
import io.getquill.testContext
import io.getquill.testContext.TestEntity
import io.getquill.testContext.lift
import io.getquill.testContext.qr1
import io.getquill.testContext.quote
import io.getquill.testContext.unquote

class ActionMacroSpec extends Spec {

  "runs non-batched action" - {
    "normal" in {
      val q = quote {
        qr1.delete
      }
      testContext.run(q).ast mustEqual q.ast
    }
    "inline" in {
      def q(i: Int) =
        testContext.run(qr1.filter(_.i == lift(i)).update(_.i -> 0))
      q(1).bind mustEqual Row(1)
    }
  }

  "runs with returning value" - {
    "assinged" in {
      val q = quote {
        qr1.insert(_.i -> 1).returning(_.l)
      }
      val r = testContext.run(q)
      r.ast mustEqual q.ast
      r.returning mustEqual Some("l")

    }
    "unassinged" in {
      val q = quote {
        qr1.insert.returning(_.l)
      }
      val r = testContext.run(q)(List(TestEntity("s", 1, 1L, None)))
      val AssignedAction(FunctionApply(ast, _), _) = r.ast
      ast mustEqual q.ast
      r.returning mustEqual Some("l")
    }
  }

  "runs batched action" - {
    "one param" in {
      val q = quote {
        (p1: Int) => qr1.insert(_.i -> p1)
      }
      val r = testContext.run(q)(List(1, 2))
      r.ast mustEqual q.ast.body
      r.bindList mustEqual List(Row(1), Row(2))
    }
    "two params" in {
      val q = quote {
        (p1: Int, p2: String) => qr1.insert(_.i -> p1, _.s -> p2)
      }
      val r = testContext.run(q)(List((1, "a"), (2, "b")))
      r.ast mustEqual q.ast.body
      r.bindList mustEqual List(Row(1, "a"), Row(2, "b"))
    }
    "with in-place binding" in {
      val q = quote { (i: Int) => (s: Int) => qr1.update(_.i -> i, _.s -> s)
      }
      val v = 1
      val r = testContext.run(q(lift(v)))(List(1, 2))
      q.ast.body match {
        case f: Function => r.ast mustEqual r.ast
        case other       => fail
      }
      r.bindList mustEqual List(Row(1, v), Row(2, v))
    }
  }

  "expands unassigned actions" - {
    "simple" in {
      val q = quote(qr1.insert)
      val r = testContext.run(q)(
        List(
          TestEntity("s", 1, 2L, Some(4)),
          TestEntity("s2", 12, 22L, Some(42))
        )
      )
      r.ast.toString mustEqual "query[TestEntity].insert(x => x.s -> s, x => x.i -> i, x => x.l -> l, x => x.o -> o)"
      r.bindList mustEqual List(
        Row("s", 1, 2L, Some(4)),
        Row("s2", 12, 22L, Some(42))
      )
    }
    "with in-place param" in {
      val q = quote {
        (i: Int) => qr1.filter(t => t.i == i).update
      }
      val v = 1
      val r = testContext.run(q(lift(v)))(
        List(
          TestEntity("s", 1, 2L, Some(4)),
          TestEntity("s2", 12, 22L, Some(42))
        )
      )

      r.ast.toString mustEqual "query[TestEntity].filter(t => t.i == lift(v)).update(x => x.s -> s, x => x.i -> i, x => x.l -> l, x => x.o -> o)"
      r.bindList mustEqual List(
        Row("s", 1, 2L, Some(4), v),
        Row("s2", 12, 22L, Some(42), v)
      )
    }
  }
}
