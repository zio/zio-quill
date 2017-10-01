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

  "a.filter(b => c).innerJoin(d).on((e, f) => g) => a.innerJoin(d).on((e, f) => g).filter(x => c[b := x._1])" in {
    val q = quote {
      qr1.filter(a => a.i == 1).join(qr2).on((a, b) => a.i == b.i)
    }
    val n = quote {
      qr1.join(qr2).on((a, b) => a.i == b.i).filter(x => x._1.i == 1)
    }
    SymbolicReduction.unapply(q.ast) mustEqual Some(n.ast)
  }

  "a.innerJoin(b.filter(c => d)).on((e, f) => g) => a.innerJoin(b).on((e, f) => g).filter(x => d[c := x._2])" in {
    val q = quote {
      qr1.join(qr2.filter(b => b.i == 1)).on((a, b) => a.i == b.i)
    }
    val n = quote {
      qr1.join(qr2).on((a, b) => a.i == b.i).filter(x => x._2.i == 1)
    }
    SymbolicReduction.unapply(q.ast) mustEqual Some(n.ast)
  }

  "doesn't reduce non-inner-joins since they aren't commutative" - {
    "a.filter.*join(b)" in {
      val q = quote {
        qr1.filter(a => a.i == 1).leftJoin(qr2).on((a, b) => a.i == b.i)
      }
      SymbolicReduction.unapply(q.ast) mustEqual None
    }
    "a.*join(b.filter)" in {
      val q = quote {
        qr1.rightJoin(qr2.filter(b => b.i == 1)).on((a, b) => a.i == b.i)
      }
      SymbolicReduction.unapply(q.ast) mustEqual None
    }
  }
}
