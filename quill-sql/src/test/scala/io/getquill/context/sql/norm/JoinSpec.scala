package io.getquill.context.sql.norm

import io.getquill.Spec
import io.getquill.context.sql.testContext._
import io.getquill.context.sql.testContext

class JoinSpec extends Spec {

  "join + filter" in {
    val q = quote {
      qr1.leftJoin(qr2)
        .on((a, b) => a.i == b.i)
        .filter(_._2.map(_.i).forall(_ == 1))
    }
    testContext.run(q).string mustEqual
      "SELECT a.s, a.i, a.l, a.o, a.b, b.s, b.i, b.l, b.o FROM TestEntity a LEFT JOIN TestEntity2 b ON a.i = b.i WHERE b.i IS NULL OR b.i = 1"
  }

  "join + filter with null-check" in {
    val q = quote {
      qr1.leftJoin(qr2)
        .on((a, b) => a.i == b.i)
        .filter(_._2.map(_.i).forall(b => if (b == 1) true else false))
    }
    testContext.run(q).string mustEqual
      "SELECT a.s, a.i, a.l, a.o, a.b, b.s, b.i, b.l, b.o FROM TestEntity a LEFT JOIN TestEntity2 b ON a.i = b.i WHERE b.i IS NULL OR b.i IS NOT NULL AND CASE WHEN b.i = 1 THEN true ELSE false END"
  }

  "join + map + filter" in {
    val q = quote {
      qr1.leftJoin(qr2)
        .on((a, b) => a.i == b.i)
        .map(t => (t._1.i, t._2.map(_.i)))
        .filter(_._2.forall(_ == 1))
    }
    testContext.run(q).string mustEqual
      "SELECT a.i, b.i FROM TestEntity a LEFT JOIN TestEntity2 b ON a.i = b.i WHERE b.i IS NULL OR b.i = 1"
  }

  "join + map + filter with null-check" in {
    val q = quote {
      qr1.leftJoin(qr2)
        .on((a, b) => a.i == b.i)
        .map(t => (t._1.i, t._2.map(_.i)))
        .filter(_._2.forall(b => if (b == 1) true else false))
    }
    testContext.run(q).string mustEqual
      "SELECT a.i, b.i FROM TestEntity a LEFT JOIN TestEntity2 b ON a.i = b.i WHERE b.i IS NULL OR b.i IS NOT NULL AND CASE WHEN b.i = 1 THEN true ELSE false END"
  }

  "join + filter + leftjoin" in {
    val q = quote {
      qr1.leftJoin(qr2).on {
        (a, b) => a.i == b.i
      }.filter {
        ab =>
          ab._2.map(_.l).contains(3L)
      }.leftJoin(qr3).on {
        (ab, c) =>
          ab._2.map(_.i).contains(ab._1.i) && ab._2.map(_.i).contains(c.i)
      }
    }
    testContext.run(q).string mustEqual
      "SELECT ab._1s, ab._1i, ab._1l, ab._1o, ab._1b, ab._2s, ab._2i, ab._2l, ab._2o, c.s, c.i, c.l, c.o FROM (SELECT a.s AS _1s, a.i AS _1i, a.l AS _1l, a.o AS _1o, a.b AS _1b, b.s AS _2s, b.i AS _2i, b.l AS _2l, b.o AS _2o FROM TestEntity a LEFT JOIN TestEntity2 b ON a.i = b.i WHERE b.l = 3) AS ab LEFT JOIN TestEntity3 c ON ab._2i = ab._1i AND ab._2i = c.i"
  }

  "join + distinct + leftjoin" in {
    val q = quote {
      qr1.leftJoin(qr2).on {
        (a, b) => a.i == b.i
      }.distinct.leftJoin(qr3).on {
        (ab, c) =>
          ab._2.map(_.i).contains(ab._1.i) && ab._2.map(_.i).contains(c.i)
      }
    }
    testContext.run(q).string mustEqual
      "SELECT ab._1s, ab._1i, ab._1l, ab._1o, ab._1b, ab._2s, ab._2i, ab._2l, ab._2o, c.s, c.i, c.l, c.o FROM (SELECT DISTINCT a.s AS _1s, a.i AS _1i, a.l AS _1l, a.o AS _1o, a.b AS _1b, b.s AS _2s, b.i AS _2i, b.l AS _2l, b.o AS _2o FROM TestEntity a LEFT JOIN TestEntity2 b ON a.i = b.i) AS ab LEFT JOIN TestEntity3 c ON ab._2i = ab._1i AND ab._2i = c.i"
  }

  "multiple joins + filter + map + distinct" in {
    val q = quote {
      qr1.join(qr2)
        .on { (d, a) => d.i == a.i }
        .join {
          qr3.filter(rp => rp.s == lift("a"))
        }
        .on { (da, p) => da._1.i == p.i }
        .leftJoin(qr4)
        .on { (dap, n) => dap._2.l == n.i }
        .map { case (dap, n) => (dap._1._2.s, dap._1._1.l, n.map(_.i)) }
        .distinct
    }
    testContext.run(q).string mustEqual
      "SELECT DISTINCT a.s, d.l, n.i FROM TestEntity d INNER JOIN TestEntity2 a ON d.i = a.i INNER JOIN (SELECT rp.i, rp.l FROM TestEntity3 rp WHERE rp.s = ?) AS rp ON d.i = rp.i LEFT JOIN TestEntity4 n ON rp.l = n.i"
  }

  "multiple joins + map" in {
    val q = quote {
      qr1.leftJoin(qr2).on((a, b) => a.s == b.s).leftJoin(qr2).on((a, b) => a._1.s == b.s).map(_._1._1)
    }
    testContext.run(q).string mustEqual
      "SELECT a.s, a.i, a.l, a.o, a.b FROM TestEntity a LEFT JOIN TestEntity2 b ON a.s = b.s LEFT JOIN TestEntity2 b1 ON a.s = b1.s"
  }

}