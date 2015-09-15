package io.getquill.quotation

import io.getquill._

class FreeVariablesSpec extends Spec {

  "detects references to values outside of the quotation (free variables)" - {
    "ident" in {
      val s = "s"
      "quote(s)" mustNot compile
    }
    "function" in {
      val s = "s"
      """
      quote {
        (a: String) => s
      }
      """ mustNot compile
    }
    "filter" in {
      val s = "s"
      """
      quote {
        qr1.filter(_.s == s)
      }
      """ mustNot compile
    }
    "map" in {
      val s = "s"
      """
      quote {
        qr1.map(_ => s)
      }
      """ mustNot compile
    }
    "flatMap" in {
      val s = "s"
      """
      quote {
        qr1.map(_ => s).flatMap(_ => qr2)
      }
      """ mustNot compile
    }
    "sortBy" in {
      val s = "s"
      """
      quote {
        qr1.sortBy(_ => s)
      }
      """ mustNot compile
    }
    "reverse" in {
      val s = "s"
      """
      quote {
        qr1.sortBy(_ => s).reverse
      }
      """ mustNot compile
    }
    "take" in {
      val s = 10
      """
      quote {
        qr1.take(s)
      }
      """ mustNot compile
    }
  }

  "doesn't fail for variables defined in the quotation" - {
    "function" in {
      val q = quote {
        (s: String) => s
      }
    }
    "filter" in {
      val q = quote {
        qr1.filter(t => t.s == "s1")
      }
    }
    "map" in {
      val q = quote {
        qr1.map(_.s)
      }
    }
    "flatMap" in {
      val q = quote {
        qr1.flatMap(t => qr2.filter(u => t.s == u.s))
      }
    }
    "reverse" in {
      val q = quote {
        qr1.sortBy(b => b.s).reverse
      }
    }
  }
}
