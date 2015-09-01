package io.getquill.norm

import io.getquill.Spec
import io.getquill.quote
import io.getquill.unquote

class SymbolicReductionSpec extends Spec {

  "reduces nested structures" in {
    val q = quote {
      qr1.flatMap(x => qr2.map(y => y.s).filter(s => s == "s"))
    }
    val n = quote {
      qr1.flatMap(x => qr2.filter(y => y.s == "s").map(y => y.s))
    }
    SymbolicReduction(q.ast) mustEqual n.ast
  }

  "applies intermediate map" in {
    val q = quote {
      qr1.map(b => "s1").map(d => d == "s2")
    }
    val n = quote {
      qr1.map(b => "s1" == "s2")
    }
    SymbolicReduction(q.ast) mustEqual n.ast
  }

  "*.flatMap" - {
    "a.filter(b => c).flatMap(d => e.$)" - {
      "e is an entity" in {
        val q = quote {
          qr1.filter(b => b.s == "s1").flatMap(d => qr3)
        }
        val n = quote {
          qr1.flatMap(b => qr3.filter(x => b.s == "s1"))
        }
        SymbolicReduction(q.ast) mustEqual n.ast
      }
      "e isn't an entity" in {
        val q = quote {
          qr1.filter(b => b.s == "s1").flatMap(d => qr3.map(f => f.s))
        }
        val n = quote {
          qr1.flatMap(b => qr3.filter(f => b.s == "s1").map(f => f.s))
        }
        SymbolicReduction(q.ast) mustEqual n.ast
      }
    }
    "a.sortBy(b => c).flatMap(d => e.$)" - {
      "e is an entity" in {
        val q = quote {
          qr1.sortBy(b => b.s).flatMap(d => qr3)
        }
        val n = quote {
          qr1.flatMap(b => qr3.sortBy(x => b.s))
        }
        SymbolicReduction(q.ast) mustEqual n.ast
      }
      "e isn't an entity" in {
        val q = quote {
          qr1.sortBy(b => b.s).flatMap(d => qr3.map(f => f.s))
        }
        val n = quote {
          qr1.flatMap(b => qr3.sortBy(f => b.s).map(f => f.s))
        }
        SymbolicReduction(q.ast) mustEqual n.ast
      }
    }
    "a.flatMap(b => c).flatMap(d => e)" in {
      val q = quote {
        qr1.flatMap(b => qr2).flatMap(d => qr3)
      }
      val n = quote {
        qr1.flatMap(b => qr2.flatMap(d => qr3))
      }
      SymbolicReduction(q.ast) mustEqual n.ast
    }
  }
}
