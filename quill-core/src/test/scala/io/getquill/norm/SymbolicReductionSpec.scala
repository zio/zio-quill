package io.getquill.norm

import io.getquill._

class SymbolicReductionSpec extends Spec {

  "map.*" - {
    "a.map(b => c).map(d => e)" in {
      val q = quote {
        qr1.map(b => "s1").map(d => d == "s2")
      }
      val n = quote {
        qr1.map(b => "s1" == "s2")
      }
      SymbolicReduction(q.ast) mustEqual n.ast
    }
    "a.map(b => c).flatMap(d => e)" in {
      val q = quote {
        qr1.map(b => "s1").flatMap(d => qr2.filter(h => h.s == d))
      }
      val n = quote {
        qr1.flatMap(b => qr2.filter(h => h.s == "s1"))
      }
      SymbolicReduction(q.ast) mustEqual n.ast
    }
    "a.map(b => c).filter(d => e)" in {
      val q = quote {
        qr1.map(b => "s1").filter(d => d == "s2")
      }
      val n = quote {
        qr1.filter(b => "s1" == "s2").map(b => "s1")
      }
      SymbolicReduction(q.ast) mustEqual n.ast
    }
    "a.map(b => c).sortBy(d => e)" in {
      val q = quote {
        qr1.map(b => b.s).sortBy(d => d)
      }
      val n = quote {
        qr1.sortBy(b => b.s).map(b => b.s)
      }
      SymbolicReduction(q.ast) mustEqual n.ast
    }
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

  "applies recursion if the nested asts change" - {
    "flatMap" in {
      val q = quote {
        qr1.flatMap(x => qr2.map(y => y.s).filter(s => s == "s"))
      }
      val n = quote {
        qr1.flatMap(x => qr2.filter(y => y.s == "s").map(y => y.s))
      }
      SymbolicReduction(q.ast) mustEqual n.ast
    }
    "filter" in {
      val q = quote {
        qr1.filter(x => qr2.map(y => y.s).filter(s => s == "s").isEmpty)
      }
      val n = quote {
        qr1.filter(x => qr2.filter(y => y.s == "s").map(y => y.s).isEmpty)
      }
      SymbolicReduction(q.ast) mustEqual n.ast
    }
    "map" in {
      val q = quote {
        qr1.map(x => qr2.map(y => y.s).filter(s => s == "s").isEmpty)
      }
      val n = quote {
        qr1.map(x => qr2.filter(y => y.s == "s").map(y => y.s).isEmpty)
      }
      SymbolicReduction(q.ast) mustEqual n.ast
    }
    "sortBy" in {
      val q = quote {
        qr1.sortBy(t => (t.i, t.s)._1)
      }
      val n = quote {
        qr1.sortBy(t => t.i)
      }
      SymbolicReduction(q.ast) mustEqual n.ast
    }
  }
}
