package io.getquill.source

import io.getquill._
import io.getquill.ast._
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
      val r = mirrorSource.run(q).using(List(1, 2))
      r.ast mustEqual bind(q.ast.body, "i")
      r.bindList mustEqual List(Row(1), Row(2))
    }
    "two params" in {
      val q = quote {
        (i: Int, s: String) => qr1.insert(_.i -> i, _.s -> s)
      }
      val r = mirrorSource.run(q).using(List((1, "a"), (2, "b")))
      r.ast mustEqual bind(q.ast.body, "i", "s")
      r.bindList mustEqual List(Row(1, "a"), Row(2, "b"))
    }
  }

  private def bind(ast: Ast, idents: String*) =
    BindVariables(ast, idents.map(Ident(_)).toList) match {
      case (ast, _) => ast
    }
}
