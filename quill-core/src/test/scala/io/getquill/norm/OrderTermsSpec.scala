package io.getquill.norm

import io.getquill._

class OrderTermsSpec extends Spec {

  "reverse" - {
    "a.reverse.filter(b => c)" in {
      val q = quote {
        qr1.sortBy(b => b.s).reverse.filter(d => d == "s2")
      }
      val n = quote {
        qr1.sortBy(b => b.s).filter(d => d == "s2").reverse
      }
      OrderTerms.unapply(q.ast) mustEqual Some(n.ast)
    }
    "a.map(b => c).reverse" in {
      val q = quote {
        qr1.sortBy(t => t.s).map(t => t.s).reverse
      }
      val n = quote {
        qr1.sortBy(t => t.s).reverse.map(t => t.s)
      }
      OrderTerms.unapply(q.ast) mustEqual Some(n.ast)
    }
  }

  "sortBy" - {
    "a.sortBy(b => c).filter(d => e)" in {
      val q = quote {
        qr1.sortBy(b => b.s).filter(d => d.s == "s1")
      }
      val n = quote {
        qr1.filter(d => d.s == "s1").sortBy(b => b.s)
      }
      OrderTerms.unapply(q.ast) mustEqual Some(n.ast)
    }
  }

  "take" - {
    "a.map(b => c).take(d)" in {
      val q = quote {
        qr1.map(b => b.s).take(10)
      }
      val n = quote {
        qr1.take(10).map(b => b.s)
      }
      OrderTerms.unapply(q.ast) mustEqual Some(n.ast)
    }
  }

}
