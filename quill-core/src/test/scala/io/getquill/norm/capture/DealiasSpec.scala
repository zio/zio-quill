package io.getquill.norm.capture

import scala.language.reflectiveCalls

import io.getquill.Spec
import io.getquill.quote
import io.getquill.unquote

class DealiasSpec extends Spec {

  "ensures that each entity is referenced by the same alias" - {
    "filter" - {
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
      "filter.sortBy" in {
        val q = quote {
          qr1.filter(a => a.s == "s1").sortBy(b => b.s)
        }
        val n = quote {
          qr1.filter(a => a.s == "s1").sortBy(a => a.s)
        }
        Dealias(q.ast) mustEqual n.ast
      }
    }
    "sortBy" - {
      "sortBy.map" in {
        val q = quote {
          qr1.sortBy(a => a.s).map(b => b.s)
        }
        val n = quote {
          qr1.sortBy(a => a.s).map(a => a.s)
        }
        Dealias(q.ast) mustEqual n.ast
      }
      "sortBy.flatMap" in {
        val q = quote {
          qr1.sortBy(a => a.s).flatMap(b => qr2.sortBy(c => c == b))
        }
        val n = quote {
          qr1.sortBy(a => a.s).flatMap(a => qr2.sortBy(c => c == a))
        }
        Dealias(q.ast) mustEqual n.ast
      }
      "sortBy.filter" in {
        val q = quote {
          qr1.sortBy(a => a.s).filter(b => b.s == "s2")
        }
        val n = quote {
          qr1.sortBy(a => a.s).filter(a => a.s == "s2")
        }
        Dealias(q.ast) mustEqual n.ast
      }
      "sortBy.sortBy" in {
        val q = quote {
          qr1.sortBy(a => a.s).sortBy(b => b.s)
        }
        val n = quote {
          qr1.sortBy(a => a.s).sortBy(a => a.s)
        }
        Dealias(q.ast) mustEqual n.ast
      }
    }
  }
}
