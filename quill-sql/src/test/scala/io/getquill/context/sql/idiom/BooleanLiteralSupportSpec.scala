package io.getquill.context.sql.idiom

import io.getquill.{ MirrorSqlDialectWithBooleanLiterals, Spec }
import io.getquill.context.sql.testContext

class BooleanLiteralSupportSpec extends Spec {

  "value-fy boolean expression where needed" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
    import ctx._

    case class Ent(name: String, b: Boolean, bb: Boolean, bc: Boolean)
    val q = quote {
      query[Ent].map(e => (e.name, if (e.b == e.bb) e.bc else e.b == e.bb))
    }

    ctx.run(q).string mustEqual
      "SELECT e.name, CASE WHEN e.b = e.bb THEN e.bc WHEN e.b = e.bb THEN 1 ELSE 0 END FROM Ent e"
  }

  "options" - {
    "exists" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        qr1.filter(t => t.o.exists(_ => if (false) false else true)).map(t => (t.b, true))
      }
      ctx.run(q).string mustEqual
        "SELECT t.b, 1 FROM TestEntity t WHERE t.o IS NOT NULL AND CASE WHEN 1 = 0 THEN 0 ELSE 1 END"
    }
  }

  "joins" - {
    "join + map" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => true).map(t => (t._1.i, t._2.map(_.s), false))
      }
      ctx.run(q).string mustEqual
        "SELECT a.i, b.s, 0 FROM TestEntity a LEFT JOIN TestEntity2 b ON 1 = 1"
    }

    "join + map + filter" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        qr1
          .leftJoin(qr2)
          .on((a, b) => false)
          .map(t => (t._1.i, t._2.map(_.s), false))
          .filter(_._2.forall(v => if (true) true else false))
      }
      ctx.run(q).string mustEqual
        "SELECT a.i, b.s, 0 FROM TestEntity a LEFT JOIN TestEntity2 b ON 1 = 0 WHERE b.s IS NULL OR b.s IS NOT NULL AND CASE WHEN 1 = 1 THEN 1 ELSE 0 END"
    }
  }
}
