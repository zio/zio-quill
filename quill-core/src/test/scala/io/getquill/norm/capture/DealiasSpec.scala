package io.getquill.norm.capture

import io.getquill.Spec
import io.getquill._
import language.reflectiveCalls

class DealiasSpec extends Spec {

  "ensures that each entity is referenced by the same alias" - {
    "filter.map" in {
      val q = quote {
        qr1.filter(a => a.s == "s1").map(b => b.s)
      }
      val n = quote {
        qr1.filter(a => a.s == "s1").map(a => a.s)
      }
      Dealias(q.ast) mustEqual n.ast
    }
    "filter.flatMap" in {
      val q = quote {
        qr1.filter(a => a.s == "s1").flatMap(b => qr2.filter(c => c == b))
      }
      val n = quote {
        qr1.filter(a => a.s == "s1").flatMap(a => qr2.filter(c => c == a))
      }
      Dealias(q.ast) mustEqual n.ast
    }
    "filter.filter" in {
      val q = quote {
        qr1.filter(a => a.s == "s1").filter(b => b.s == "s2")
      }
      val n = quote {
        qr1.filter(a => a.s == "s1").filter(a => a.s == "s2")
      }
      Dealias(q.ast) mustEqual n.ast
    }
  }
}
