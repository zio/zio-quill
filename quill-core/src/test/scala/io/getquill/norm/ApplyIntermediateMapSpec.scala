package io.getquill.norm

import io.getquill.Spec
import io.getquill.testSource._

class ApplyIntermediateMapSpec extends Spec {

  "avoids applying the intermmediate map after a groupBy" - {
    "flatMap" in {
      val q = quote {
        qr1.groupBy(t => t.i).map(y => y._1).flatMap(s => qr2.filter(z => z.s == s))
      }
      ApplyIntermediateMap.unapply(q.ast) mustEqual None
    }
    "filter" in {
      val q = quote {
        qr1.groupBy(t => t.i).map(y => y._1).filter(s => s == "s")
      }
      ApplyIntermediateMap.unapply(q.ast) mustEqual None
    }
    "map" in {
      val q = quote {
        qr1.groupBy(t => t.i).map(y => y._1).map(s => s)
      }
      ApplyIntermediateMap.unapply(q.ast) mustEqual None
    }
    "sortBy" in {
      val q = quote {
        qr1.groupBy(t => t.i).map(y => y._1).sortBy(s => s)
      }
      ApplyIntermediateMap.unapply(q.ast) mustEqual None
    }
    "identity map" in {
      val q = quote {
        qr1.groupBy(t => t.i).map(y => y)
      }
      ApplyIntermediateMap.unapply(q.ast) mustEqual None
    }
  }

  "applies intermediate map" - {
    "flatMap" in {
      val q = quote {
        qr1.map(y => y.s).flatMap(s => qr2.filter(z => z.s == s))
      }
      val n = quote {
        qr1.flatMap(y => qr2.filter(z => z.s == y.s))
      }
      ApplyIntermediateMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "filter" in {
      val q = quote {
        qr1.map(y => y.s).filter(s => s == "s")
      }
      val n = quote {
        qr1.filter(y => y.s == "s").map(y => y.s)
      }
      ApplyIntermediateMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "map" in {
      val q = quote {
        qr1.map(y => y.s).map(s => s)
      }
      val n = quote {
        qr1.map(y => y.s)
      }
      ApplyIntermediateMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "sortBy" in {
      val q = quote {
        qr1.map(y => y.s).sortBy(s => s)
      }
      val n = quote {
        qr1.sortBy(y => y.s).map(y => y.s)
      }
      ApplyIntermediateMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "identity map" in {
      val q = quote {
        qr1.sortBy(y => y.s).map(y => y)
      }
      val n = quote {
        qr1.sortBy(y => y.s)
      }
      ApplyIntermediateMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "distinct" in {
      val q = quote {
        query[TestEntity].map(i => (i.i, i.l)).distinct.map(x => (x._1, x._2))
      }
      val n = quote {
        query[TestEntity].map(i => (i.i, i.l)).distinct
      }
      ApplyIntermediateMap.unapply(q.ast) mustEqual Some(n.ast)
    }
  }
}
