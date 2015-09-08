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

  "reverse" - {
    "a.reverse.reverse" in {
      val q = quote {
        qr1.sortBy(b => b.s).reverse.reverse
      }
      val n = quote {
        qr1.sortBy(b => b.s)
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
    "a.reverse.filter(b => c)" in {
      val q = quote {
        qr1.sortBy(b => b.s).reverse.filter(d => d == "s2")
      }
      val n = quote {
        qr1.filter(b => b == "s2").sortBy(b => b.s).reverse
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
    "a.map(b => c).reverse" in {
      val q = quote {
        qr1.sortBy(t => t.s).map(t => t.s).reverse
      }
      val n = quote {
        qr1.sortBy(t => t.s).reverse.map(t => t.s)
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
  }

  "sortBy" - {
    "a.sortBy(b => c).filter(d => e)" in {
      val q = quote {
        qr1.sortBy(b => b.s).filter(d => d.s == "s1")
      }
      val n = quote {
        qr1.filter(b => b.s == "s1").sortBy(b => b.s)
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
  }

  "filter" - {
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

  "flatMap" - {
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
    "a.flatMap(b => c).reverse" in {
      val q = quote {
        qr1.flatMap(b => qr2).sortBy(b => b.s).reverse
      }
      val n = quote {
        qr1.flatMap(b => qr2.sortBy(b1 => b1.s).reverse)
      }
      AdHocReduction(q.ast) mustEqual n.ast
    }
  }
}
