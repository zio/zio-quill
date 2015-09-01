package io.getquill.norm

import io.getquill.Spec
import io.getquill.quote
import io.getquill.unquote

class AdHocReductionSpec extends Spec {

  "reduces nested structures" in {
    val q = quote {
      qr1.flatMap(x => qr2.sortBy(t => t.s).filter(t => t.s == "s1"))
    }
    val n = quote {
      qr1.flatMap(x => qr2.filter(t => t.s == "s1").sortBy(t => t.s))
    }
    AdHocReduction(q.ast) mustEqual n.ast
  }

  "applies intermediate map" in {
    val q = quote {
      qr1.map(b => "s1").map(d => d == "s2")
    }
    val n = quote {
      qr1.map(b => "s1" == "s2")
    }
    AdHocReduction(q.ast) mustEqual n.ast
  }

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
      AdHocReduction(q.ast) mustEqual q.ast
    }
    "a.sortBy(b => c).sortBy(d => e)" in {
      val q = quote {
        qr1.sortBy(b => b.s).sortBy(d => d.l)
      }
      AdHocReduction(q.ast) mustEqual q.ast
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
}
