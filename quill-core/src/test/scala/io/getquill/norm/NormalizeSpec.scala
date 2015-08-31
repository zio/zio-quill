package io.getquill.norm

import io.getquill._

class NormalizeSpec extends Spec {

  "map.*" - {
    "a.map(b => c).map(d => e)" in {
      val q = quote {
        qr1.map(b => "s1").map(d => d == "s2")
      }
      val n = quote {
        qr1.map(b => "s1" == "s2")
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
    "a.filter(b => c).flatMap(d => e.$)" - {
      "e is an entity" in {
        val q = quote {
          qr1.filter(b => b.s == "s1").flatMap(d => qr3)
        }
        val n = quote {
          qr1.flatMap(b => qr3.filter(x => b.s == "s1"))
        }
        Normalize(q.ast) mustEqual n.ast
      }
      "e isn't an entity" in {
        val q = quote {
          qr1.filter(b => b.s == "s1").flatMap(d => qr3.map(f => f.s))
        }
        val n = quote {
          qr1.flatMap(b => qr3.filter(f => b.s == "s1").map(f => f.s))
        }
        Normalize(q.ast) mustEqual n.ast
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
        Normalize(q.ast) mustEqual n.ast
      }
      "e isn't an entity" in {
        val q = quote {
          qr1.sortBy(b => b.s).flatMap(d => qr3.map(f => f.s))
        }
        val n = quote {
          qr1.flatMap(b => qr3.sortBy(f => b.s).map(f => f.s))
        }
        Normalize(q.ast) mustEqual n.ast
      }
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
  }

  "sortBy.*" - {
    "a.sortBy(b => c).filter(d => e)" in {
      val q = quote {
        qr1.sortBy(b => b.s).filter(d => d.s == "s1")
      }
      val n = quote {
        qr1.filter(b => b.s == "s1").sortBy(b => b.s)
      }
      Normalize(q.ast) mustEqual n.ast
    }
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
  }

  "filter.filter" - {
    "a.filter(b => c).filter(d => e)" in {
      val q = quote {
        qr1.filter(b => b.s == "s1").filter(d => d.s == "s2")
      }
      val n = quote {
        qr1.filter(b => b.s == "s1" && b.s == "s2")
      }
      Normalize(q.ast) mustEqual n.ast
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

  "applies recursion if the nested asts change" - {
    "flatMap" in {
      val q = quote {
        qr1.flatMap(x => qr2.sortBy(t => t.s).filter(t => t.s == "s1"))
      }
      val n = quote {
        qr1.flatMap(x => qr2.filter(t => t.s == "s1").sortBy(t => t.s))
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "filter" in {
      val q = quote {
        qr1.sortBy(t => t.s).sortBy(t => t.i).filter(t => t.s == "s1")
      }
      val n = quote {
        qr1.filter(t => t.s == "s1").sortBy(t => (t.s, t.i))
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "map" in {
      val q = quote {
        qr1.sortBy(t => t.s).sortBy(t => t.i).map(t => t.s)
      }
      val n = quote {
        qr1.sortBy(t => (t.s, t.i)).map(t => t.s)
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "sortBy" in {
      val q = quote {
        qr1.filter(t => t.s == "2").filter(t => t.i == 1).sortBy(t => t.l)
      }
      val n = quote {
        qr1.filter(t => t.s == "2" && t.i == 1).sortBy(t => t.l)
      }
      Normalize(q.ast) mustEqual n.ast
    }
  }
}
