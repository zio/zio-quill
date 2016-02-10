package io.getquill.source.sql

import io.getquill._
import io.getquill.source.sql.idiom.FallbackDialect
import io.getquill.naming.Literal
import io.getquill.source.sql.mirror.mirrorSource

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
  
  "mirror sql joins" in {
    val q = quote{
      for{
        (a,b)<- qr1 join qr2 on(_.i == _.i)
           c <- qr1 join(_.i == a.i)
           d <- qr2 join(_.i == b.i)
           e <- qr3 join(_.i == c.i)
      } yield (a,b,c,d,e)
    }
    mirrorSource.run(q).sql mustEqual
      "SELECT x3.s, x3.i, x3.l, x3.o, x4.s, x4.i, x4.l, x4.o, x5.s, x5.i, x5.l, x5.o, x6.s, x6.i, x6.l, x6.o, x7.s, x7.i, x7.l, x7.o FROM TestEntity x3 INNER JOIN TestEntity2 x4 ON x3.i = x4.i INNER JOIN TestEntity x5 ON x5.i = x3.i INNER JOIN TestEntity2 x6 ON x6.i = x4.i INNER JOIN TestEntity3 x7 ON x7.i = x5.i"
  }
  	
}
