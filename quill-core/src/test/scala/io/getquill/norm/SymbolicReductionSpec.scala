package io.getquill.norm

import io.getquill.base.Spec
import io.getquill.MirrorContexts.testContext.qr1
import io.getquill.MirrorContexts.testContext.qr2
import io.getquill.MirrorContexts.testContext.qr3
import io.getquill.MirrorContexts.testContext.quote
import io.getquill.MirrorContexts.testContext.unquote
import io.getquill.util.TraceConfig
import io.getquill.ast.Query

class SymbolicReductionSpec extends Spec { // hello

  def symbolicReduction: Query => Option[Query] =
    (new SymbolicReduction(TraceConfig.Empty).unapply _).andThen(o => o.map(replaceTempIdent(_)))

  "a.filter(b => c).flatMap(d => e.$)" - {
    "e is an entity" in {
      val q = quote {
        qr1.filter(b => b.s == "s1").flatMap(_ => qr2)
      }
      val n = quote {
        qr1.flatMap(d => qr2.filter(_ => d.s == "s1"))
      }
      symbolicReduction(q.ast) mustEqual Some(n.ast)
    }
    "e isn't an entity" in {
      val q = quote {
        qr1.filter(b => b.s == "s1").flatMap(_ => qr2.map(f => f.s))
      }
      val n = quote {
        qr1.flatMap(d => qr2.filter(_ => d.s == "s1").map(f => f.s))
      }
      symbolicReduction(q.ast) mustEqual Some(n.ast)
    }
  }

  "a.flatMap(b => c).flatMap(d => e)" in {
    val q = quote {
      qr1.flatMap(_ => qr2).flatMap(_ => qr3)
    }
    val n = quote {
      qr1.flatMap(_ => qr2.flatMap(_ => qr3))
    }
    symbolicReduction(q.ast) mustEqual Some(n.ast)
  }

  "a.union(b).flatMap(c => d)" in {
    val q = quote {
      qr1.union(qr1.filter(t => t.i == 1)).flatMap(_ => qr2)
    }
    val n = quote {
      qr1.flatMap(_ => qr2).union(qr1.filter(t => t.i == 1).flatMap(_ => qr2))
    }
    symbolicReduction(q.ast) mustEqual Some(n.ast)
  }

  "a.unionAll(b).flatMap(c => d)" in {
    val q = quote {
      qr1.unionAll(qr1.filter(t => t.i == 1)).flatMap(_ => qr2)
    }
    val n = quote {
      qr1.flatMap(_ => qr2).unionAll(qr1.filter(t => t.i == 1).flatMap(_ => qr2))
    }
    symbolicReduction(q.ast) mustEqual Some(n.ast)
  }
}
