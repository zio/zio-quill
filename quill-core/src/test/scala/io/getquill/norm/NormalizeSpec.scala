package io.getquill.norm

import io.getquill._

class NormalizeSpec extends Spec {

  "normalizes random-generated queries" - {
    val gen = new QueryGenerator(1)
    for (i <- (2 to 15)) {
      for (j <- (0 until 30)) {
        val query = gen(i)
        val n = Normalize(query)
        s"$i levels ($j) - $query" in {
          val q = VerifyNormalization(n)
        }
      }
    }
  }

  "reduces nested structures" in {
    val q = quote {
      qr1.flatMap(x => qr2.sortBy(t => t.s).filter(t => t.s == "s1"))
    }
    val n = quote {
      qr1.flatMap(x => qr2.filter(t => t.s == "s1").sortBy(t => t.s))
    }
    Normalize(q.ast) mustEqual n.ast
  }

  "applies intermediate map" in {
    val q = quote {
      qr1.map(b => "s1").map(d => d == "s2")
    }
    val n = quote {
      qr1.map(b => "s1" == "s2")
    }
    Normalize(q.ast) mustEqual n.ast
  }

  "reverse" - {
    "a.reverse.reverse" in {
      val q = quote {
        qr1.sortBy(b => b.s).reverse.reverse
      }
      val n = quote {
        qr1.sortBy(b => b.s)
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "a.reverse.filter(b => c)" in {
      val q = quote {
        qr1.sortBy(b => b.s).reverse.filter(d => d == "s2")
      }
      val n = quote {
        qr1.filter(b => b == "s2").sortBy(b => b.s).reverse
      }
      Normalize(q.ast) mustEqual n.ast
    }
    "a.map(b => c).reverse" in {
      val q = quote {
        qr1.sortBy(t => t.s).map(t => t.s).reverse
      }
      val n = quote {
        qr1.sortBy(t => t.s).reverse.map(t => t.s)
      }
      Normalize(q.ast) mustEqual n.ast
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
      Normalize(q.ast) mustEqual n.ast
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
      Normalize(q.ast) mustEqual n.ast
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
    "a.flatMap(b => c).reverse" in {
      val q = quote {
        qr1.flatMap(b => qr2).sortBy(b => b.s).reverse
      }
      val n = quote {
        qr1.flatMap(b => qr2.sortBy(b1 => b1.s).reverse)
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
}
