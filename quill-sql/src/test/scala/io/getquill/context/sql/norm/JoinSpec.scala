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
      "SELECT a.s, a.i, a.l, a.o, b.s, b.i, b.l, b.o FROM TestEntity a LEFT JOIN TestEntity2 b ON a.i = b.i WHERE b.i IS NULL OR b.i = 1"
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
      "SELECT ab.s, ab.i, ab.l, ab.o, ab.s, ab.i, ab.l, ab.o, c.s, c.i, c.l, c.o FROM (SELECT a.s s, a.o o, a.l l, a.i i, b.l l, b.i i, b.o o, b.s s FROM TestEntity a LEFT JOIN TestEntity2 b ON a.i = b.i WHERE b.l = 3) ab LEFT JOIN TestEntity3 c ON ab.i = ab.i AND ab.i = c.i"
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
      "SELECT ab.s, ab.i, ab.l, ab.o, ab.s, ab.i, ab.l, ab.o, c.s, c.i, c.l, c.o FROM (SELECT DISTINCT a.*, b.* FROM TestEntity a LEFT JOIN TestEntity2 b ON a.i = b.i) ab LEFT JOIN TestEntity3 c ON ab.i = ab.i AND ab.i = c.i"
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
      "SELECT DISTINCT a.s, d.l, n.i FROM TestEntity d INNER JOIN TestEntity2 a ON d.i = a.i INNER JOIN (SELECT rp.l, rp.i FROM TestEntity3 rp WHERE rp.s = ?) rp ON d.i = rp.i LEFT JOIN TestEntity4 n ON rp.l = n.i"
  }

  "multiple joins + map" in {
    val q = quote {
      qr1.leftJoin(qr2).on((a, b) => a.s == b.s).leftJoin(qr2).on((a, b) => a._1.s == b.s).map(_._1._1)
    }
    testContext.run(q).string mustEqual
      "SELECT a.s, a.i, a.l, a.o FROM TestEntity a LEFT JOIN TestEntity2 b ON a.s = b.s LEFT JOIN TestEntity2 b1 ON a.s = b1.s"
  }

}