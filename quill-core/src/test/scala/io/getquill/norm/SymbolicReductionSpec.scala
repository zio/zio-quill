package io.getquill.norm

import io.getquill._

class SymbolicReductionSpec extends Spec {

  "a.reverse.reverse" in {
    val q = quote {
      qr1.sortBy(b => b.s).reverse.reverse
    }
    val n = quote {
      qr1.sortBy(b => b.s)
    }
    SymbolicReduction.unapply(q.ast) mustEqual Some(n.ast)
  }

  "*.flatMap" - {
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
  }
}
