package io.getquill.sources

import io.getquill._
import io.getquill.sources.mirror.Row
import io.getquill.TestSource.mirrorSource

class ActionMacroSpec extends Spec {

  "runs non-batched action" in {
    val q = quote {
      qr1.delete
    }
    mirrorSource.run(q).ast mustEqual q.ast
  }

  "runs batched action" - {
    "one param" in {
      val q = quote {
        (p1: Int) => qr1.insert(_.i -> p1)
      }
      val r = mirrorSource.run(q)(List(1, 2))
      r.ast mustEqual q.ast.body
      r.bindList mustEqual List(Row(1), Row(2))
    }
    "two params" in {
      val q = quote {
        (p1: Int, p2: String) => qr1.insert(_.i -> p1, _.s -> p2)
      }
      val r = mirrorSource.run(q)(List((1, "a"), (2, "b")))
      r.ast mustEqual q.ast.body
      r.bindList mustEqual List(Row(1, "a"), Row(2, "b"))
    }
  }

  "expands unassigned actions" in {
    val q = quote(qr1.insert)
    val r = mirrorSource.run(q)(
      List(TestEntity("s", 1, 2L, Some(4)),
        TestEntity("s2", 12, 22L, Some(42))))
    r.ast.toString mustEqual "query[TestEntity].insert(x => x.s -> s, x => x.i -> i, x => x.l -> l, x => x.o -> o)"
    r.bindList mustEqual List(
      Row("s", 1, 2L, Some(4)),
      Row("s2", 12, 22L, Some(42)))
  }
}
