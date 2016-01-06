package io.getquill.source.sql.norm

import io.getquill._
import io.getquill.source.sql.mirror.mirrorSource

class FlattenOptionOperationSpec extends Spec {

  "transforms option operations into simple properties" - {
    "map" in {
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => a.s == b.s).map(t => (t._1.s, t._2.map(_.i)))
      }
      mirrorSource.run(q).sql mustEqual
        "SELECT a.s, b.i FROM TestEntity a LEFT JOIN TestEntity2 b ON a.s = b.s"
    }
    "forall" in {
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => a.s == b.s).filter(t => t._2.forall(_.i == t._1.i)).map(_._1.s)
      }
      mirrorSource.run(q).sql mustEqual
        "SELECT a.s FROM TestEntity a LEFT JOIN TestEntity2 b ON a.s = b.s WHERE b.i = a.i"
    }
    "exists" in {
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => a.s == b.s).filter(t => t._2.exists(_.i == t._1.i)).map(_._1.s)
      }
      mirrorSource.run(q).sql mustEqual
        "SELECT a.s FROM TestEntity a LEFT JOIN TestEntity2 b ON a.s = b.s WHERE b.i = a.i"
    }
  }

}
