package io.getquill.source

import io.getquill.Spec
import io.getquill._
import io.getquill.ast._
import io.getquill.test.mirrorSource
import io.getquill.test.Row

class QueryMacroSpec extends Spec {

  "runs non-binded action" in {
    val q = quote {
      qr1.map(_.i)
    }
    mirrorSource.run(q).ast mustEqual q.ast
  }

  "runs binded query" - {
    "one param" in {
      val q = quote {
        (i: Int) => qr1.filter(t => t.i == i).map(t => t.i)
      }
      val r = mirrorSource.run(q)(1)
      r.ast mustEqual bind(q.ast.body, "i")
      r.binds mustEqual Row(1)
    }
    "two params" in {
      val q = quote {
        (i: Int, s: String) => qr1.filter(t => t.i == i && t.s == s).map(t => t.i)
      }
      val r = mirrorSource.run(q)(1, "a")
      r.ast mustEqual bind(q.ast.body, "i", "s")
      r.binds mustEqual Row(1, "a")
    }
  }

  private def bind(ast: Ast, idents: String*) =
    BindVariables(ast, idents.map(Ident(_)).toList) match {
      case (ast, _) => ast
    }
}
