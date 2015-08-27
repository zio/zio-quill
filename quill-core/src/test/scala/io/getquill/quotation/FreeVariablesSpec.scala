package io.getquill.quotation

import io.getquill._
import io.getquill.Spec

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
  }

  "doesn't fail for variables defined in the quotation" - {
    "function" in {
      quote {
        (s: String) => s
      }
    }
    "filter" in {
      quote {
        qr1.filter(t => t.s == "s1")
      }
    }
    "map" in {
      quote {
        qr1.map(_.s)
      }
    }
    "flatMap" in {
      quote {
        qr1.flatMap(t => qr2.filter(u => t.s == u.s))
      }
    }
  }
}
