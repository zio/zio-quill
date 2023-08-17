package io.getquill.norm

import io.getquill.base.Spec
import io.getquill.MirrorContexts.testContext.TestEntity
import io.getquill.MirrorContexts.testContext.implicitOrd
import io.getquill.MirrorContexts.testContext.qr1
import io.getquill.MirrorContexts.testContext.qr2
import io.getquill.MirrorContexts.testContext.query
import io.getquill.MirrorContexts.testContext.quote
import io.getquill.MirrorContexts.testContext.unquote
import io.getquill.util.TraceConfig

class ApplyMapSpec extends Spec {

  val ApplyMap = new ApplyMap(TraceConfig.Empty)

  "avoids applying the intermediate map after a groupBy" - {
    "flatMap" in {
      val q = quote {
        qr1.groupBy(t => t.s).map(y => y._1).flatMap(s => qr2.filter(z => z.s == s))
      }
      ApplyMap.unapply(q.ast) mustEqual None
    }
    "filter" in {
      val q = quote {
        qr1.groupBy(t => t.s).map(y => y._1).filter(s => s == "s")
      }
      ApplyMap.unapply(q.ast) mustEqual None
    }
    "map" in {
      val q = quote {
        qr1.groupBy(t => t.i).map(y => y._1).filter(i => i == 1)
      }
      ApplyMap.unapply(q.ast) mustEqual None
    }
    "sortBy" in {
      val q = quote {
        qr1.groupBy(t => t.i).map(y => y._1).sortBy(s => s)
      }
      ApplyMap.unapply(q.ast) mustEqual None
    }
    "take" in {
      val q = quote {
        qr1.groupBy(t => t.i).map(y => y._1).take(1)
      }
      ApplyMap.unapply(q.ast) mustEqual None
    }
    "drop" in {
      val q = quote {
        qr1.groupBy(t => t.i).map(y => y._1).drop(1)
      }
      ApplyMap.unapply(q.ast) mustEqual None
    }
    "identity map" in {
      val q = quote {
        qr1.groupBy(t => t.i).map(y => y)
      }
      ApplyMap.unapply(q.ast) mustEqual None
    }
    "mapped join" - {
      "left" in {
        val q = quote {
          qr1.groupBy(t => t.i).map(t => t._1).join(qr2).on((a, b) => a == b.i)
        }
        ApplyMap.unapply(q.ast) mustEqual None
      }
      "right" in {
        val q = quote {
          qr1.join(qr2.groupBy(t => t.i).map(t => t._1)).on((a, b) => a.i == b)
        }
        ApplyMap.unapply(q.ast) mustEqual None
      }
      "both" in {
        val q = quote {
          qr1.groupBy(t => t.i).map(t => t._1).join(qr2.groupBy(t => t.i).map(t => t._1)).on((a, b) => a == b)
        }
        ApplyMap.unapply(q.ast) mustEqual None
      }
    }
  }

  "avoids applying map with nested query" - {
    "identity map" in {
      val q = quote {
        qr1.map(x => x.i).nested.map(x => x)
      }
      ApplyMap.unapply(q.ast) mustEqual None
    }
    "join" in {
      val q = quote {
        qr1.join(qr2).on((a, b) => a.i == b.i).map(t => t).nested
      }
      ApplyMap.unapply(q.ast) mustEqual None
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
      ApplyMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "filter" in {
      val q = quote {
        qr1.map(y => y.s).filter(s => s == "s")
      }
      val n = quote {
        qr1.filter(y => y.s == "s").map(y => y.s)
      }
      ApplyMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "map" in {
      val q = quote {
        qr1.map(y => y.s).map(s => s)
      }
      val n = quote {
        qr1.map(y => y.s)
      }
      ApplyMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "sortBy" in {
      val q = quote {
        qr1.map(y => y.s).sortBy(s => s)
      }
      val n = quote {
        qr1.sortBy(y => y.s).map(y => y.s)
      }
      ApplyMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "identity map" in {
      val q = quote {
        qr1.sortBy(y => y.s).map(y => y)
      }
      val n = quote {
        qr1.sortBy(y => y.s)
      }
      ApplyMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "distinct" in {
      val q = quote {
        query[TestEntity].map(i => (i.i, i.l)).distinct.map(x => (x._1, x._2))
      }
      val n = quote {
        query[TestEntity].map(i => (i.i, i.l)).distinct
      }
      ApplyMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "distinct + sort" in {
      val q = quote {
        query[TestEntity].map(i => (i.i, i.l)).distinct.sortBy(_._1)
      }
      val n = quote {
        query[TestEntity].sortBy(i => i.i).map(i => (i.i, i.l)).distinct
      }
      ApplyMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "take" in {
      val q = quote {
        qr1.map(y => y.s).take(1)
      }
      val n = quote {
        qr1.take(1).map(y => y.s)
      }
      ApplyMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "drop" in {
      val q = quote {
        qr1.map(y => y.s).drop(1)
      }
      val n = quote {
        qr1.drop(1).map(y => y.s)
      }
      ApplyMap.unapply(q.ast) mustEqual Some(n.ast)
    }
    "nested" in {
      val q = quote {
        qr1.map(y => y.s).nested
      }
      // I.e. apply-map should not change anything in a nested query. The original structure should be preserved
      // since nesting a query should isolate everything inside the nesting.
      // For example this:
      //   ApplyMap(query[Person].nested.map(p => p.age + 1)
      // Should NOT reduce to this:
      //   ApplyMap(query[Person].map(p => p.age + 1).nested
      // The original query should remain unaltered.
      // That is the semantic meaning of `nested` and things like impure-infixes rely on that.
      // (e.g. if the impure-infix is a partition-by it needs to remain as-is and not be moved around the query).
      ApplyMap.unapply(q.ast) mustEqual None
    }
    "mapped join" - {
      "left" in {
        val q = quote {
          qr1.map(y => y.s).join(qr2).on((a, b) => a == b.s)
        }
        val n = quote {
          qr1.join(qr2).on((y, b) => y.s == b.s).map(t => (t._1.s, t._2))
        }
        ApplyMap.unapply(q.ast) mustEqual Some(n.ast)
      }
      "right" in {
        val q = quote {
          qr1.join(qr2.map(y => y.s)).on((a, b) => a.s == b)
        }
        val n = quote {
          qr1.join(qr2).on((a, y) => a.s == y.s).map(t => (t._1, t._2.s))
        }
        ApplyMap.unapply(q.ast) mustEqual Some(n.ast)
      }
      "both" in {
        val q = quote {
          qr1.map(y => y.s).join(qr2.map(u => u.s)).on((a, b) => a == b)
        }
        val n = quote {
          qr1.join(qr2).on((y, u) => y.s == u.s).map(t => (t._1.s, t._2.s))
        }
        ApplyMap.unapply(q.ast) mustEqual Some(n.ast)
      }
    }
  }
}
