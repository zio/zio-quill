package io.getquill.norm.capture

import scala.language.reflectiveCalls

import io.getquill.Spec
import io.getquill.quote
import io.getquill.unquote

class DealiasSpec extends Spec {

  "ensures that each entity is referenced by the same alias" - {
    "flatMap" in {
      val q = quote {
        qr1.filter(a => a.s == "s").flatMap(b => qr2)
      }
      val n = quote {
        qr1.filter(a => a.s == "s").flatMap(a => qr2)
      }
      Dealias(q.ast) mustEqual n.ast
    }
    "map" in {
      val q = quote {
        qr1.filter(a => a.s == "s").map(b => b.s)
      }
      val n = quote {
        qr1.filter(a => a.s == "s").map(a => a.s)
      }
      Dealias(q.ast) mustEqual n.ast
    }
    "filter" in {
      val q = quote {
        qr1.filter(a => a.s == "s").filter(b => b.s != "l")
      }
      val n = quote {
        qr1.filter(a => a.s == "s").filter(a => a.s != "l")
      }
      Dealias(q.ast) mustEqual n.ast
    }
    "sortBy" in {
      val q = quote {
        qr1.filter(a => a.s == "s").sortBy(b => b.s)
      }
      val n = quote {
        qr1.filter(a => a.s == "s").sortBy(a => a.s)
      }
      Dealias(q.ast) mustEqual n.ast
    }
    "reverse" in {
      val q = quote {
        qr1.sortBy(a => a.s).reverse.map(b => b.s)
      }
      val n = quote {
        qr1.sortBy(a => a.s).reverse.map(a => a.s)
      }
      Dealias(q.ast) mustEqual n.ast
    }
    "take" in {
      val q = quote {
        qr1.filter(a => a.s == "s").take(10).map(b => b.s)
      }
      val n = quote {
        qr1.filter(a => a.s == "s").take(10).map(a => a.s)
      }
      Dealias(q.ast) mustEqual n.ast
    }
    "entity" in {
      Dealias(qr1.ast) mustEqual qr1.ast
    }
  }
}
