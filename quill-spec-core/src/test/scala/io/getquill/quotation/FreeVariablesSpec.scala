package io.getquill.quotation

import io.getquill.Spec
import io.getquill.ast.IdentName
import io.getquill.testContext.implicitOrd
import io.getquill.testContext.qr1
import io.getquill.testContext.qr2
import io.getquill.testContext.quote
import io.getquill.testContext.unquote

class FreeVariablesSpec extends Spec {

  val s = "s"
  val i = 10

  "detects references to values outside of the quotation (free variables)" - {
    "ident" in {
      val q = quote(s)
      FreeVariables(q.ast) mustEqual Set(IdentName("s"))
    }
    "function" in {
      val q =
        quote {
          (a: String) => s
        }
      FreeVariables(q.ast) mustEqual Set(IdentName("s"))
    }
    "filter" in {
      val q =
        quote {
          qr1.filter(_.s == s)
        }
      FreeVariables(q.ast) mustEqual Set(IdentName("s"))
    }
    "map" in {
      val q =
        quote {
          qr1.map(_ => s)
        }
      FreeVariables(q.ast) mustEqual Set(IdentName("s"))
    }
    "flatMap" in {
      val q =
        quote {
          qr1.map(_ => s).flatMap(_ => qr2)
        }
      FreeVariables(q.ast) mustEqual Set(IdentName("s"))
    }
    "concatMap" in {
      val a = Seq(1, 2)
      val q =
        quote {
          qr1.concatMap(_ => a).flatMap(_ => qr2)
        }
      FreeVariables(q.ast) mustEqual Set(IdentName("a"))
    }
    "sortBy" in {
      val q =
        quote {
          qr1.sortBy(_ => s)
        }
      FreeVariables(q.ast) mustEqual Set(IdentName("s"))
    }
    "groupBy" in {
      val q =
        quote {
          qr1.groupBy(_ => s)
        }
      FreeVariables(q.ast) mustEqual Set(IdentName("s"))
    }
    "take" in {
      val q =
        quote {
          qr1.take(i)
        }
      FreeVariables(q.ast) mustEqual Set(IdentName("i"))
    }
    "conditional outer join" in {
      val q =
        quote {
          qr1.leftJoin(qr2).on((a, b) => a.s == s)
        }
      FreeVariables(q.ast) mustEqual Set(IdentName("s"))
    }
    "assignment" in {
      val q = quote {
        qr1.insert(_.i -> i)
      }
      FreeVariables(q.ast) mustEqual Set(IdentName("i"))
    }
    "join" in {
      val i = 1
      val q = quote {
        qr1.join(qr2.filter(_.i == i))
          .on((t1, t2) => t1.i == t2.i)
      }
      FreeVariables(q.ast) mustEqual Set(IdentName("i"))
    }
    "option operators" - {
      "map" in {
        val i = 1
        val q = quote {
          qr1.map(_.o.map(_ == i))
        }
        FreeVariables(q.ast) mustEqual Set(IdentName("i"))
      }
      "forall" in {
        val i = 1
        val q = quote {
          qr1.filter(_.o.forall(_ == i))
        }
        FreeVariables(q.ast) mustEqual Set(IdentName("i"))

      }
      "exists" in {
        val i = 1
        val q = quote {
          qr1.filter(_.o.exists(_ == i))
        }
        FreeVariables(q.ast) mustEqual Set(IdentName("i"))
      }
      "contains" in {
        val i = 1
        val q = quote {
          qr1.filter(_.o.contains(i))
        }
        FreeVariables(q.ast) mustEqual Set(IdentName("i"))
      }
    }
  }

  "takes in consideration variables defined in the quotation" - {
    "function" in {
      val q = quote {
        (s: String) => s
      }
      FreeVariables(q.ast) mustBe empty
    }
    "filter" in {
      val q = quote {
        qr1.filter(t => t.s == "s1")
      }
      FreeVariables(q.ast) mustBe empty
    }
    "map" in {
      val q = quote {
        qr1.map(_.s)
      }
      FreeVariables(q.ast) mustBe empty
    }
    "flatMap" in {
      val q = quote {
        qr1.flatMap(t => qr2.filter(u => t.s == u.s))
      }
      FreeVariables(q.ast) mustBe empty
    }
    "conditional outer join" in {
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => a.s == b.s)
      }
      FreeVariables(q.ast) mustBe empty
    }
    "option operators" - {
      "map" in {
        val i = 1
        val q = quote {
          qr1.map(t => t.o.map(_ == t.i))
        }
        FreeVariables(q.ast) mustBe empty
      }
      "forall" in {
        val i = 1
        val q = quote {
          qr1.filter(t => t.o.forall(_ == t.i))
        }
        FreeVariables(q.ast) mustBe empty

      }
      "exists" in {
        val i = 1
        val q = quote {
          qr1.filter(t => t.o.exists(_ == t.i))
        }
        FreeVariables(q.ast) mustBe empty
      }
      "contains" in {
        val i = 1
        val q = quote {
          qr1.filter(t => t.o.contains(t.i))
        }
        FreeVariables(q.ast) mustBe empty
      }
    }
    "assignment" in {
      val q = quote {
        qr1.insert(t => t.i -> (t.i + 1))
      }
      FreeVariables(q.ast) mustBe empty
    }
  }
}
