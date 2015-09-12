package io.getquill.norm

import io.getquill.Spec
import io.getquill.quote
import io.getquill.unquote

class NormalizeNestedStructuresSpec extends Spec {

  "flatMap" in {
    val q = quote {
      qr1.flatMap(x => qr2.map(y => y.s).filter(s => s == "s"))
    }
    val n = quote {
      qr1.flatMap(x => qr2.filter(y => y.s == "s").map(y => y.s))
    }
    NormalizeNestedStructures.unapply(q.ast) mustEqual Some(n.ast)
  }
  "filter" in {
    val q = quote {
      qr1.filter(x => qr2.map(y => y.s).filter(s => s == "s").isEmpty)
    }
    val n = quote {
      qr1.filter(x => qr2.filter(y => y.s == "s").map(y => y.s).isEmpty)
    }
    NormalizeNestedStructures.unapply(q.ast) mustEqual Some(n.ast)
  }
  "map" in {
    val q = quote {
      qr1.map(x => qr2.map(y => y.s).filter(s => s == "s").isEmpty)
    }
    val n = quote {
      qr1.map(x => qr2.filter(y => y.s == "s").map(y => y.s).isEmpty)
    }
    NormalizeNestedStructures.unapply(q.ast) mustEqual Some(n.ast)
  }
  "sortBy" in {
    val q = quote {
      qr1.sortBy(t => (t.i, t.s)._1)
    }
    val n = quote {
      qr1.sortBy(t => t.i)
    }
    NormalizeNestedStructures.unapply(q.ast) mustEqual Some(n.ast)
  }
  "reverse" in {
    val q = quote {
      qr1.sortBy(t => (t.i, t.s)._1).reverse
    }
    val n = quote {
      qr1.sortBy(t => t.i).reverse
    }
    NormalizeNestedStructures.unapply(q.ast) mustEqual Some(n.ast)
  }
}
