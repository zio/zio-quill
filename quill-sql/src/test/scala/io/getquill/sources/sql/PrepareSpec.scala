package io.getquill.sources.sql

import io.getquill._

class PrepareSpec extends Spec {

  "nested and mapped outer joins" in {
    val q = quote {
      for {
        a <- qr1.leftJoin(qr2.map(t => (t.i, t.l))).on((a, b) => a.i > b._1)
        b <- qr1.leftJoin(qr2.map(t => (t.i, t.l))).on((a, b) => a.l < b._2)
      } yield {
        (a._2.map(_._1), b._2.map(_._2))
      }
    }
    mirrorSource.run(q).sql mustEqual
      "SELECT t._1, t1._2 FROM TestEntity a LEFT JOIN (SELECT t.i _1 FROM TestEntity2 t) t ON a.i > t._1, TestEntity a1 LEFT JOIN (SELECT t1.l _2 FROM TestEntity2 t1) t1 ON a1.l < t1._2"
  }

  "mirror sql joins" - {

    "with one secondary table" in {
      val q = quote {
        for {
          (a, b) <- qr1 join qr2 on (_.i == _.i)
          c <- qr1 join (_.i == a.i)
        } yield (a, b, c)
      }
      mirrorSource.run(q).sql mustEqual
        "SELECT x3.s, x3.i, x3.l, x3.o, x4.s, x4.i, x4.l, x4.o, x5.s, x5.i, x5.l, x5.o FROM TestEntity x3 INNER JOIN TestEntity2 x4 ON x3.i = x4.i INNER JOIN TestEntity x5 ON x5.i = x3.i"
    }

    "with many secondary tables" in {
      val q = quote {
        for {
          (a, b) <- qr1 join qr2 on (_.i == _.i)
          c <- qr1 join (_.i == a.i)
          d <- qr2 join (_.i == b.i)
          e <- qr3 join (_.i == c.i)
        } yield (a, b, c, d, e)
      }
      mirrorSource.run(q).sql mustEqual
        "SELECT x7.s, x7.i, x7.l, x7.o, x8.s, x8.i, x8.l, x8.o, x9.s, x9.i, x9.l, x9.o, x10.s, x10.i, x10.l, x10.o, x11.s, x11.i, x11.l, x11.o FROM TestEntity x7 INNER JOIN TestEntity2 x8 ON x7.i = x8.i INNER JOIN TestEntity x9 ON x9.i = x7.i INNER JOIN TestEntity2 x10 ON x10.i = x8.i INNER JOIN TestEntity3 x11 ON x11.i = x9.i"
    }

    "with outer join" in {
      val q = quote {
        for {
          (a, b) <- qr1 join qr2 on (_.i == _.i)
          c <- qr1 rightJoin (_.i == a.i)
        } yield (a, b, c.map(_.i))
      }
      mirrorSource.run(q).sql mustEqual
        "SELECT x13.s, x13.i, x13.l, x13.o, x14.s, x14.i, x14.l, x14.o, x15.i FROM TestEntity x13 INNER JOIN TestEntity2 x14 ON x13.i = x14.i RIGHT JOIN TestEntity x15 ON x15.i = x13.i"
    }
  }

}
