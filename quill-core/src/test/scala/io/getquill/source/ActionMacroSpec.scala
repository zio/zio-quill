package io.getquill.source

import io.getquill._
import io.getquill.ast.{ Action => _, _ }
import io.getquill.source.mirror.Row
import io.getquill.source.mirror.mirrorSource

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
        (i: Int) => qr1.insert(_.i -> i)
      }
      val r = mirrorSource.run(q)(List(1, 2))
      r.ast mustEqual bind(q.ast.body, "i")
      r.bindList mustEqual List(Row(1), Row(2))
    }
    "two params" in {
      val q = quote {
        (i: Int, s: String) => qr1.insert(_.i -> i, _.s -> s)
      }
      val r = mirrorSource.run(q)(List((1, "a"), (2, "b")))
      r.ast mustEqual bind(q.ast.body, "i", "s")
      r.bindList mustEqual List(Row(1, "a"), Row(2, "b"))
    }
  }

  "expands unassigned actions" in {
    val q = quote(qr1.insert)
    val r = mirrorSource.run(q)(
      List(TestEntity("s", 1, 2L, Some(4)),
        TestEntity("s2", 12, 22L, Some(42))))
    r.ast.toString mustEqual "query[TestEntity].insert(x => x.s -> ?, x => x.i -> ?, x => x.l -> ?, x => x.o -> ?)"
    r.bindList mustEqual List(
      Row("s", 1, 2L, Some(4)),
      Row("s2", 12, 22L, Some(42)))
  }

  private def bind(ast: Ast, idents: String*) =
    BindVariables(ast, idents.map(Ident(_)).toList) match {
      case (ast, _) => ast
    }
}
