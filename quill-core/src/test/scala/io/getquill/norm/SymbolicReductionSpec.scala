package io.getquill.norm

import io.getquill.Spec
import io.getquill.testContext.qr1
import io.getquill.testContext.qr2
import io.getquill.testContext.qr3
import io.getquill.testContext.quote
import io.getquill.testContext.unquote

class SymbolicReductionSpec extends Spec {

  "a.filter(b => c).flatMap(d => e.$)" - {
    "e is an entity" in {
      val q = quote {
        qr1.filter(b => b.s == "s1").flatMap(d => qr2)
      }
      val n = quote {
        qr1.flatMap(d => qr2.filter(x => d.s == "s1"))
      }
      SymbolicReduction.unapply(q.ast) mustEqual Some(n.ast)
    }
    "e isn't an entity" in {
      val q = quote {
        qr1.filter(b => b.s == "s1").flatMap(d => qr2.map(f => f.s))
      }
      val n = quote {
        qr1.flatMap(d => qr2.filter(f => d.s == "s1").map(f => f.s))
      }
      SymbolicReduction.unapply(q.ast) mustEqual Some(n.ast)
    }
  }

  "a.flatMap(b => c).flatMap(d => e)" in {
    val q = quote {
      qr1.flatMap(b => qr2).flatMap(d => qr3)
    }
    val n = quote {
      qr1.flatMap(b => qr2.flatMap(d => qr3))
    }
    SymbolicReduction.unapply(q.ast) mustEqual Some(n.ast)
  }

  "a.union(b).flatMap(c => d)" in {
    val q = quote {
      qr1.union(qr1.filter(t => t.i == 1)).flatMap(c => qr2)
    }
    val n = quote {
      qr1.flatMap(c => qr2).union(qr1.filter(t => t.i == 1).flatMap(c => qr2))
    }
    SymbolicReduction.unapply(q.ast) mustEqual Some(n.ast)
  }

  "a.unionAll(b).flatMap(c => d)" in {
    val q = quote {
      qr1.unionAll(qr1.filter(t => t.i == 1)).flatMap(c => qr2)
    }
    val n = quote {
      qr1.flatMap(c => qr2).unionAll(qr1.filter(t => t.i == 1).flatMap(c => qr2))
    }
    SymbolicReduction.unapply(q.ast) mustEqual Some(n.ast)
  }
}
