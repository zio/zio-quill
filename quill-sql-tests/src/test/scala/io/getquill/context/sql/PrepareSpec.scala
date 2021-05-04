package io.getquill.context.sql

import io.getquill.Spec
import io.getquill.context.sql.testContext._

class PrepareSpec extends Spec {

  "nested and mapped outer joins" in pendingUntilFixed {
    val q = quote {
      for {
        a <- qr1.leftJoin(qr2.map(t => (t.i, t.l))).on((a, b) => a.i > b._1)
        b <- qr1.leftJoin(qr2.map(t => (t.i, t.l))).on((a, b) => a.l < b._2)
      } yield {
        (a._2.map(_._1), b._2.map(_._2))
      }
    }
    testContext.run(q).string mustEqual
      "SELECT t.i, t1.l FROM TestEntity a LEFT JOIN TestEntity2 t ON a.i > t.i, TestEntity a1 LEFT JOIN  TestEntity2 t1 ON a1.l < t1.l"
    ()
  }

  "mirror sql joins" - {

    "with one secondary table" in {
      val q = quote {
        for {
          (a, b) <- qr1 join qr2 on ((a, b) => a.i == b.i)
          c <- qr1 join (c => c.i == a.i)
        } yield (a, b, c)
      }
      testContext.run(q).string mustEqual
        "SELECT a.s, a.i, a.l, a.o, a.b, b.s, b.i, b.l, b.o, c.s, c.i, c.l, c.o, c.b FROM TestEntity a INNER JOIN TestEntity2 b ON a.i = b.i INNER JOIN TestEntity c ON c.i = a.i"
    }

    "with many secondary tables" in {
      val q = quote {
        for {
          (a, b) <- qr1 join qr2 on ((a, b) => a.i == b.i)
          c <- qr1 join (c => c.i == a.i)
          d <- qr2 join (d => d.i == b.i)
          e <- qr3 join (e => e.i == c.i)
        } yield (a, b, c, d, e)
      }
      testContext.run(q).string mustEqual
        "SELECT a.s, a.i, a.l, a.o, a.b, b.s, b.i, b.l, b.o, c.s, c.i, c.l, c.o, c.b, d.s, d.i, d.l, d.o, e.s, e.i, e.l, e.o FROM TestEntity a INNER JOIN TestEntity2 b ON a.i = b.i INNER JOIN TestEntity c ON c.i = a.i INNER JOIN TestEntity2 d ON d.i = b.i INNER JOIN TestEntity3 e ON e.i = c.i"
    }

    "with outer join" in {
      val q = quote {
        for {
          (a, b) <- qr1 join qr2 on ((a, b) => a.i == a.i)
          c <- qr1 rightJoin (c => c.i == a.i)
        } yield (a, b, c.map(c => c.i))
      }
      testContext.run(q).string mustEqual
        "SELECT a.s, a.i, a.l, a.o, a.b, b.s, b.i, b.l, b.o, c.i FROM TestEntity a INNER JOIN TestEntity2 b ON a.i = a.i RIGHT JOIN TestEntity c ON c.i = a.i"
    }
  }
}
