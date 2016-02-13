package io.getquill.sources

import io.getquill._
import io.getquill.sources.mirror.Row
import io.getquill.TestSource.mirrorSource

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
        (p1: Int) => qr1.filter(t => t.i == p1).map(t => t.i)
      }
      val r = mirrorSource.run(q)(1)
      r.ast mustEqual q.ast.body
      r.binds mustEqual Row(1)
    }
    "two params" in {
      val q = quote {
        (p1: Int, p2: String) => qr1.filter(t => t.i == p1 && t.s == p2).map(t => t.i)
      }
      val r = mirrorSource.run(q)(1, "a")
      r.ast mustEqual q.ast.body
      r.binds mustEqual Row(1, "a")
    }
  }
}
