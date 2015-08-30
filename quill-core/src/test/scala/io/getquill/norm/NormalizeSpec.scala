package io.getquill.norm

import language.reflectiveCalls
import io.getquill._
import io.getquill.Spec

class NormalizeSpec extends Spec {

  "applies symbolic reduction" - {
    "map.*" - {
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
      "a.map(b => c).sortBy(d => e)" in {
        val q = quote {
          qr1.map(b => b.s).sortBy(d => d)
        }
        val n = quote {
          qr1.sortBy(b => b.s).map(b => b.s)
        }
        Normalize(q.ast) mustEqual n.ast
      }
    }
    "*.flatMap" - {
      "a.flatMap(b => c.map(d => e)).flatMap(f => g)" in {
        val q = quote {
          qr1.flatMap(b => qr2.map(d => "s1")).flatMap(f => qr3.filter(h => h.s == f))
        }
        val n = quote {
          qr1.flatMap(b => qr2.flatMap(d => qr3.filter(h => h.s == "s1")))
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
          qr1.flatMap(b => qr2.filter(f => b.s == "s1").map(f => "s2"))
        }
        Normalize(q.ast) mustEqual n.ast
      }
      "a.filter(b => c).flatMap(d => e)" in {
        val q = quote {
          qr1.filter(b => b.s == "s1").flatMap(d => qr2)
        }
        val n = quote {
          qr1.flatMap(b => qr2.filter(x => b.s == "s1"))
        }
        Normalize(q.ast) mustEqual n.ast
      }
      "a.sortBy(b => c).flatMap(d => e.map(f => g))" in {
        val q = quote {
          qr1.sortBy(b => b.s).flatMap(d => qr2.map(f => "s2"))
        }
        val n = quote {
          qr1.flatMap(b => qr2.sortBy(f => b.s).map(f => "s2"))
        }
        Normalize(q.ast) mustEqual n.ast
      }
      "a.sortBy(b => c).flatMap(d => e)" in {
        val q = quote {
          qr1.sortBy(b => b.s).flatMap(d => qr2)
        }
        val n = quote {
          qr1.flatMap(b => qr2.sortBy(x => b.s))
        }
        Normalize(q.ast) mustEqual n.ast
      }
    }
  }

  "applies adhoc reduction" - {
    "filter" - {
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
          qr1.flatMap(b => qr2.filter(d => d.s == "s2"))
        }
        Normalize(q.ast) mustEqual n.ast
      }
    }
    "sortBy" - {
      "a.sortBy(b => (c)).sortBy(d => e)" in {
        val q = quote {
          qr1.sortBy(b => (b.s, b.i)).sortBy(d => d.l)
        }
        val n = quote {
          qr1.sortBy(b => (b.s, b.i, b.l))
        }
        Normalize(q.ast) mustEqual n.ast
      }
      "a.sortBy(b => c).sortBy(d => e)" in {
        val q = quote {
          qr1.sortBy(b => b.s).sortBy(d => d.l)
        }
        val n = quote {
          qr1.sortBy(b => (b.s, b.l))
        }
        Normalize(q.ast) mustEqual n.ast
      }
      "a.flatMap(b => c).sortBy(d => e)" in {
        val q = quote {
          qr1.flatMap(b => qr2).sortBy(d => d.s)
        }
        val n = quote {
          qr1.flatMap(b => qr2.sortBy(d => d.s))
        }
        Normalize(q.ast) mustEqual n.ast
      }
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
    "sortBy" in {
      val q = quote {
        qr1.sortBy(t => (t.i, t.s)._1)
      }
      val n = quote {
        qr1.sortBy(t => t.i)
      }
      Normalize(q.ast) mustEqual n.ast
    }
  }
}
