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
}
