package io.getquill.norm

import language.reflectiveCalls
import io.getquill._
import io.getquill.Spec

class NormalizeSpec extends Spec {

  "applies symbolic reduction" - {
    "a.flatMap(b => c.map(d => e)).flatMap(f => g)" in {
      val q = quote {
        qr1.flatMap(b => qr2.map(d => "s1")).flatMap(f => qr3.filter(h => h.s == f))
      }
      val n = quote {
        qr1.flatMap(b => qr2.flatMap(d => qr3.filter(h => h.s == "s1")))
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "a.map(b => c).flatMap(d => e)" in {
      val q = quote {
        qr1.map(b => "s1").flatMap(d => qr2.filter(h => h.s == d))
      }
      val n = quote {
        qr1.flatMap(b => qr2.filter(h => h.s == "s1"))
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "a.map(b => c).filter(d => e)" in {
      val q = quote {
        qr1.map(b => "s1").filter(d => d == "s2")
      }
      val n = quote {
        qr1.filter(b => "s1" == "s2").map(b => "s1")
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "a.map(b => c).map(d => e)" in {
      val q = quote {
        qr1.map(b => "s1").map(d => d == "s2")
      }
      val n = quote {
        qr1.map(b => "s1" == "s2")
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "a.flatMap(b => c).flatMap(d => e)" in {
      val q = quote {
        qr1.flatMap(b => qr2).flatMap(d => qr3)
      }
      val n = quote {
        qr1.flatMap(b => qr2.flatMap(d => qr3))
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "a.filter(b => c).flatMap(d => e.map(f => g))" in {
      val q = quote {
        qr1.filter(b => b.s == "s1").flatMap(d => qr2.map(f => "s2"))
      }
      val n = quote {
        qr1.flatMap(b => qr2.filter(temp => b.s == "s1").map(temp => "s2"))
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "a.filter(b => c).flatMap(d => e)" in {
      val q = quote {
        qr1.filter(b => b.s == "s1").flatMap(d => qr2)
      }
      val n = quote {
        qr1.flatMap(b => qr2.filter(temp => b.s == "s1"))
      }
      Normalize(q.ast) mustEqual n.ast
    }
  }

  "applies adhoc reduction" - {
    "a.filter(b => c).filter(d => e)" in {
      val q = quote {
        qr1.filter(b => b.s == "s1").filter(d => d.s == "s2")
      }
      val n = quote {
        qr1.filter(b => b.s == "s1" && b.s == "s2")
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "a.flatMap(b => c).filter(d => e)" in {
      val q = quote {
        qr1.flatMap(b => qr2).filter(d => d.s == "s2")
      }
      val n = quote {
        qr1.flatMap(b => qr2.filter(temp => b.s == "s2"))
      }
      Normalize(q.ast) mustEqual n.ast
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
      Normalize(q.ast) mustEqual n.ast
    }
    "filter" in {
      val q = quote {
        qr1.filter(x => qr2.map(y => y.s).filter(s => s == "s").isEmpty)
      }
      val n = quote {
        qr1.filter(x => qr2.filter(y => y.s == "s").map(y => y.s).isEmpty)
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "map" in {
      val q = quote {
        qr1.map(x => qr2.map(y => y.s).filter(s => s == "s").isEmpty)
      }
      val n = quote {
        qr1.map(x => qr2.filter(y => y.s == "s").map(y => y.s).isEmpty)
      }
      Normalize(q.ast) mustEqual n.ast
    }
  }
}
