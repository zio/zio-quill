package io.getquill.norm

import io.getquill._

class NormalizeNestedStructuresSpec extends Spec {

  "returns Some if a nested structure changes" - {
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
    "take" in {
      val q = quote {
        qr1.sortBy(t => (t.i, t.s)._1).take((1, 2)._2)
      }
      val n = quote {
        qr1.sortBy(t => t.i).take(2)
      }
      NormalizeNestedStructures.unapply(q.ast) mustEqual Some(n.ast)
    }
    "drop" in {
      val q = quote {
        qr1.sortBy(t => (t.i, t.s)._1).drop((1, 2)._2)
      }
      val n = quote {
        qr1.sortBy(t => t.i).drop(2)
      }
      NormalizeNestedStructures.unapply(q.ast) mustEqual Some(n.ast)
    }
    "union" in {
      val q = quote {
        qr1.filter(t => t.s == ("a", "b")._1).union(qr1)
      }
      val n = quote {
        qr1.filter(t => t.s == "a").union(qr1)
      }
      NormalizeNestedStructures.unapply(q.ast) mustEqual Some(n.ast)
    }
    "unionAll" in {
      val q = quote {
        qr1.filter(t => t.s == ("a", "b")._1).unionAll(qr1)
      }
      val n = quote {
        qr1.filter(t => t.s == "a").unionAll(qr1)
      }
      NormalizeNestedStructures.unapply(q.ast) mustEqual Some(n.ast)
    }
  }

  "returns None if none of the nested structures changes" - {
    "flatMap" in {
      val q = quote {
        qr1.flatMap(x => qr2)
      }
      NormalizeNestedStructures.unapply(q.ast) mustEqual None
    }
    "filter" in {
      val q = quote {
        qr1.filter(x => x.i == 1)
      }
      NormalizeNestedStructures.unapply(q.ast) mustEqual None
    }
    "map" in {
      val q = quote {
        qr1.map(x => x.i)
      }
      NormalizeNestedStructures.unapply(q.ast) mustEqual None
    }
    "sortBy" in {
      val q = quote {
        qr1.sortBy(t => t.i)
      }
      NormalizeNestedStructures.unapply(q.ast) mustEqual None
    }
    "reverse" in {
      val q = quote {
        qr1.sortBy(t => t.i).reverse
      }
      NormalizeNestedStructures.unapply(q.ast) mustEqual None
    }
    "take" in {
      val q = quote {
        qr1.sortBy(t => t.i).take(2)
      }
      NormalizeNestedStructures.unapply(q.ast) mustEqual None
    }
    "drop" in {
      val q = quote {
        qr1.sortBy(t => t.i).drop(2)
      }
      NormalizeNestedStructures.unapply(q.ast) mustEqual None
    }
  }
}
