package io.getquill.norm

import io.getquill._

class AdHocReductionSpec extends Spec {

  "sortBy.*" - {
    "a.sortBy(b => c).filter(d => e)" in {
      val q = quote {
        qr1.sortBy(b => b.s).filter(d => d.s == "s1")
      }
      val n = quote {
        qr1.filter(d => d.s == "s1").sortBy(b => b.s)
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
    "a.sortBy(b => (c)).sortBy(d => e)" in {
      val q = quote {
        qr1.sortBy(b => (b.s, b.i)).sortBy(d => d.l)
      }
      val n = quote {
        qr1.sortBy(b => (b.s, b.i, b.l))
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
    "a.sortBy(b => c).sortBy(d => e)" in {
      val q = quote {
        qr1.sortBy(b => b.s).sortBy(d => d.l)
      }
      val n = quote {
        qr1.sortBy(b => (b.s, b.l))
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
  }

  "filter.filter" - {
    "a.filter(b => c).filter(d => e)" in {
      val q = quote {
        qr1.filter(b => b.s == "s1").filter(d => d.s == "s2")
      }
      val n = quote {
        qr1.filter(b => b.s == "s1" && b.s == "s2")
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
  }

  "flatMap.*" - {
    "a.flatMap(b => c).map(d => e)" in {
      val q = quote {
        qr1.flatMap(b => qr2).map(d => d.s)
      }
      val n = quote {
        qr1.flatMap(b => qr2.map(d => d.s))
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
    "a.flatMap(b => c).filter(d => e)" in {
      val q = quote {
        qr1.flatMap(b => qr2).filter(d => d.s == "s2")
      }
      val n = quote {
        qr1.flatMap(b => qr2.filter(d => d.s == "s2"))
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
    "a.flatMap(b => c).sortBy(d => e)" in {
      val q = quote {
        qr1.flatMap(b => qr2).sortBy(d => d.s)
      }
      val n = quote {
        qr1.flatMap(b => qr2.sortBy(d => d.s))
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
  }

  "applies recursion if the nested asts change" - {
    "flatMap" in {
      val q = quote {
        qr1.flatMap(x => qr2.sortBy(t => t.s).filter(t => t.s == "s1"))
      }
      val n = quote {
        qr1.flatMap(x => qr2.filter(t => t.s == "s1").sortBy(t => t.s))
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
    "filter" in {
      val q = quote {
        qr1.sortBy(t => t.s).sortBy(t => t.i).filter(t => t.s == "s1")
      }
      val n = quote {
        qr1.filter(t => t.s == "s1").sortBy(t => (t.s, t.i))
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
    "map" in {
      val q = quote {
        qr1.sortBy(t => t.s).sortBy(t => t.i).map(t => t.s)
      }
      val n = quote {
        qr1.sortBy(t => (t.s, t.i)).map(t => t.s)
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
    "sortBy" in {
      val q = quote {
        qr1.filter(t => t.s == "2").filter(t => t.i == 1).sortBy(t => t.l)
      }
      val n = quote {
        qr1.filter(t => t.s == "2" && t.i == 1).sortBy(t => t.l)
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
  }
}
