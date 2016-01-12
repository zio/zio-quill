package io.getquill.norm

import io.getquill._
import io.getquill.ast.Ast

class FlattenOptionOperationSpec extends Spec {

  "transforms option operations into simple properties" - {
    "map" in {
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => a.s == b.s).map(t => (t._1.s, t._2.map(_.i)))
      }
      FlattenOptionOperation(q.ast: Ast).toString mustEqual
        "query[TestEntity].leftJoin(query[TestEntity2]).on((a, b) => a.s == b.s).map(t => (t._1.s, t._2.i))"
    }
    "forall" in {
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => a.s == b.s).filter(t => t._2.forall(_.i == t._1.i)).map(_._1.s)
      }
      FlattenOptionOperation(q.ast: Ast).toString mustEqual
        "query[TestEntity].leftJoin(query[TestEntity2]).on((a, b) => a.s == b.s).filter(t => t._2.i == t._1.i).map(x$3 => x$3._1.s)"
    }
    "exists" in {
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => a.s == b.s).filter(t => t._2.exists(_.i == t._1.i)).map(_._1.s)
      }
      FlattenOptionOperation(q.ast: Ast).toString mustEqual
        "query[TestEntity].leftJoin(query[TestEntity2]).on((a, b) => a.s == b.s).filter(t => t._2.i == t._1.i).map(x$5 => x$5._1.s)"
    }
  }
}
