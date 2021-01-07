package io.getquill.context.sql.idiom

import io.getquill.{ MirrorSqlDialectWithBooleanLiterals, Spec }
import io.getquill.context.sql.testContext

class BooleanLiteralSupportSpec extends Spec {

  "value-fy boolean expression where needed" - testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
    import ctx._

    case class Ent(name: String, b: Boolean, bb: Boolean, bc: Boolean, num: Int)
    case class Status(name: String, value: Boolean)

    "condition" in {
      val q = quote {
        query[Ent].map(e => (e.name, if (e.b == e.bb) e.bc else e.b == e.bb))
      }
      ctx.run(q).string mustEqual
        "SELECT e.name, CASE WHEN e.b = e.bb THEN e.bc WHEN e.b = e.bb THEN 1 ELSE 0 END FROM Ent e"
    }

    "map-clause" in {
      val q = quote {
        query[Ent].map(e => e.bb == true)
      }
      ctx.run(q).string mustEqual
        "SELECT CASE WHEN e.bb = 1 THEN 1 ELSE 0 END FROM Ent e"
    }
    "map-clause with int" in {
      val q = quote {
        query[Ent].map(e => e.num > 10)
      }
      ctx.run(q).string mustEqual
        "SELECT CASE WHEN e.num > 10 THEN 1 ELSE 0 END FROM Ent e"
    }
    "tuple" in {
      val q = quote {
        query[Ent].map(e => ("foo", e.bb == true))
      }
      ctx.run(q).string mustEqual
        "SELECT 'foo', CASE WHEN e.bb = 1 THEN 1 ELSE 0 END FROM Ent e"
    }
    "tuple-multi" in {
      val q = quote {
        query[Ent].map(e => (e.bb == true, e.bc == false, e.num > 1))
      }
      ctx.run(q).string mustEqual
        "SELECT CASE WHEN e.bb = 1 THEN 1 ELSE 0 END, CASE WHEN e.bc = 0 THEN 1 ELSE 0 END, CASE WHEN e.num > 1 THEN 1 ELSE 0 END FROM Ent e"
    }
    "case-class" in {
      val q = quote {
        query[Ent].map(e => Status("foo", e.bb == true))
      }
      ctx.run(q).string mustEqual
        "SELECT 'foo', CASE WHEN e.bb = 1 THEN 1 ELSE 0 END FROM Ent e"
    }
  }

  "infix" - testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
    import ctx._

    "expressify asCondition" - {
      case class Ent(name: String, i: Int, b: Boolean)

      "filter-clause" in {
        val q = quote {
          query[Ent].filter(e => infix"${e.i} > 123".asCondition)
        }
        ctx.run(q).string mustEqual
          "SELECT e.name, e.i, e.b FROM Ent e WHERE e.i > 123"
      }

      "pure filter-clause" in {
        val q = quote {
          query[Ent].filter(e => infix"${e.i} > 123".pure.asCondition)
        }
        ctx.run(q).string mustEqual
          "SELECT e.name, e.i, e.b FROM Ent e WHERE e.i > 123"
      }

      "map-clause" in {
        val q = quote {
          query[Ent].map(e => infix"${e.i} > 123".asCondition) //hello
        }.dynamic
        println(io.getquill.util.Messages.qprint(q.ast))
        ctx.run(q).string mustEqual
          "SELECT e._1 FROM (SELECT CASE WHEN e.i > 123 THEN 1 ELSE 0 END AS _1 FROM Ent e) AS e"
      }

      "distinct map-clause" in {
        val q = quote {
          query[Ent].map(e => ("foo", infix"${e.i} > 123".asCondition)).distinct.map(r => ("baz", r._2))
        }.dynamic
        println(io.getquill.util.Messages.qprint(q.ast))
        ctx.run(q).string mustEqual
          "SELECT 'baz', e._2 FROM (SELECT DISTINCT e._1, e._2 FROM (SELECT 'foo' AS _1, CASE WHEN e.i > 123 THEN 1 ELSE 0 END AS _2 FROM Ent e) AS e) AS e"
      }

      "distinct tuple map-clause" in {
        val q = quote {
          query[Ent].map(e => ("foo", infix"${e.i} > 123".asCondition)).distinct //hellooooooo
        }.dynamic
        println(io.getquill.util.Messages.qprint(q.ast))
        ctx.run(q).string mustEqual
          "SELECT DISTINCT e._1, e._2 FROM (SELECT 'foo' AS _1, CASE WHEN e.i > 123 THEN 1 ELSE 0 END AS _2 FROM Ent e) AS e"
      }

      "pure map-clause" in {
        val q = quote {
          query[Ent].map(e => infix"${e.i} > 123".pure.asCondition)
        }
        ctx.run(q).string mustEqual
          "SELECT CASE WHEN e.i > 123 THEN 1 ELSE 0 END FROM Ent e"
      }

      "pure distinct map-clause" in {
        val q = quote {
          query[Ent].map(e => ("foo", infix"${e.i} > 123".pure.asCondition)).distinct.map(r => ("baz", r._2))
        }
        println(io.getquill.util.Messages.qprint(q.ast))
        ctx.run(q).string mustEqual
          "SELECT 'baz', e._2 FROM (SELECT DISTINCT 'foo' AS _1, CASE WHEN e.i > 123 THEN 1 ELSE 0 END AS _2 FROM Ent e) AS e"
      }

      "pure map-clause - double element" in {
        val q = quote {
          query[Ent].map(e => infix"${e.i} > 123".pure.asCondition).distinct.map(r => (r, r))
        }.dynamic
        ctx.run(q).string mustEqual
          "SELECT e._1, e._1 FROM (SELECT DISTINCT CASE WHEN e.i > 123 THEN 1 ELSE 0 END AS _1 FROM Ent e) AS e"
      }
    }

    "valuefy normally" - {
      case class Ent(name: String, i: Int, b: Boolean)

      "filter-clause" in {
        val q = quote {
          query[Ent].filter(e => infix"SomeUdf(${e.i})".as[Boolean])
        }
        ctx.run(q).string mustEqual
          "SELECT e.name, e.i, e.b FROM Ent e WHERE 1 = SomeUdf(e.i)"
      }

      "pure filter-clause" in {
        val q = quote {
          query[Ent].filter(e => infix"SomeUdf(${e.i})".pure.as[Boolean])
        }
        ctx.run(q).string mustEqual
          "SELECT e.name, e.i, e.b FROM Ent e WHERE 1 = SomeUdf(e.i)"
      }

      "map-clause" in {
        val q = quote {
          query[Ent].map(e => infix"SomeUdf(${e.i})".as[Boolean])
        }
        ctx.run(q).string mustEqual
          "SELECT e._1 FROM (SELECT SomeUdf(e.i) AS _1 FROM Ent e) AS e"
      }

      "pure map-clause" in {
        val q = quote {
          query[Ent].map(e => infix"SomeUdf(${e.i})".pure.as[Boolean])
        }
        ctx.run(q).string mustEqual
          "SELECT SomeUdf(e.i) FROM Ent e"
      }
    }
  }

  "do not expressify string transforming operations" - {
    case class Product(id: Long, desc: String, sku: Int)

    "first parameter" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        query[Product].filter(p => lift("1").toInt == p.sku)
      }
      ctx.run(q).string mustEqual
        "SELECT p.id, p.desc, p.sku FROM Product p WHERE  (?) = p.sku"
    }

    "second parameter" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        query[Product].filter(p => p.sku == lift("1").toInt)
      }
      ctx.run(q).string mustEqual
        "SELECT p.id, p.desc, p.sku FROM Product p WHERE p.sku =  (?)"
    }

    "both parameters" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        query[Product].filter(p => lift("2").toInt == lift("1").toInt)
      }
      ctx.run(q).string mustEqual
        "SELECT p.id, p.desc, p.sku FROM Product p WHERE  (?) =  (?)"
    }
  }

  "options" - {
    "exists" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        qr1.filter(t => t.o.exists(_ => if (false) false else true)).map(t => (t.b, true))
      }
      ctx.run(q).string mustEqual
        "SELECT t.b, 1 FROM TestEntity t WHERE t.o IS NOT NULL AND (1 = 0 AND 1 = 0 OR NOT (1 = 0) AND 1 = 1)"
    }

    "exists - lifted" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        qr1.filter(t => t.o.exists(_ => if (lift(false)) lift(false) else lift(true))).map(t => (t.b, true))
      }
      ctx.run(q).string mustEqual
        "SELECT t.b, 1 FROM TestEntity t WHERE t.o IS NOT NULL AND (1 = ? AND 1 = ? OR NOT (1 = ?) AND 1 = ?)"
    }
  }

  "joins" - {
    import testContext.extras._

    "join + map" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => true).map(t => (t._1.i, t._2.map(_.s), false))
      }
      ctx.run(q).string mustEqual
        "SELECT a.i, b.s, 0 FROM TestEntity a LEFT JOIN TestEntity2 b ON 1 = 1"
    }

    "join + map (with conditional)" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => true).map(t => (t._1.i, if (t._2.map(_.i > 20) === true) false else true))
      }
      ctx.run(q).string mustEqual
        "SELECT a.i, CASE WHEN CASE WHEN b.i > 20 THEN 1 ELSE 0 END = 1 THEN 0 ELSE 1 END FROM TestEntity a LEFT JOIN TestEntity2 b ON 1 = 1"
    }

    "join + map (with conditional comparison)" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => true).map(t => (t._1.i, if (t._2.forall(_.i > 20)) false else true))
      }
      ctx.run(q).string mustEqual
        "SELECT a.i, CASE WHEN b IS NULL OR b.i > 20 THEN 0 ELSE 1 END FROM TestEntity a LEFT JOIN TestEntity2 b ON 1 = 1"
    }

    "join + map (with conditional comparison lifted)" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => true).map(t => (t._1.i, if (t._2.forall(_.i > 20)) lift(false) else lift(true)))
      }
      ctx.run(q).string mustEqual
        "SELECT a.i, CASE WHEN b IS NULL OR b.i > 20 THEN ? ELSE ? END FROM TestEntity a LEFT JOIN TestEntity2 b ON 1 = 1"
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
        "SELECT a.i, b.s, 0 FROM TestEntity a LEFT JOIN TestEntity2 b ON 1 = 0 WHERE b.s IS NULL OR b.s IS NOT NULL AND (1 = 1 AND 1 = 1 OR NOT (1 = 1) AND 1 = 0)"
    }

    "join + map + filter (lifted)" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        qr1
          .leftJoin(qr2)
          .on((a, b) => false)
          .map(t => (t._1.i, t._2.map(_.s), false))
          .filter(_._2.forall(v => if (lift(true)) lift(true) else lift(false)))
      }
      ctx.run(q).string mustEqual
        "SELECT a.i, b.s, 0 FROM TestEntity a LEFT JOIN TestEntity2 b ON 1 = 0 WHERE b.s IS NULL OR b.s IS NOT NULL AND (1 = ? AND 1 = ? OR NOT (1 = ?) AND 1 = ?)"
    }

    "for-comprehension with constant" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        for {
          t1 <- query[TestEntity]
          t2 <- query[TestEntity].join(t => true)
        } yield (t1, t2)
      }
      ctx.run(q).string mustEqual
        "SELECT t1.s, t1.i, t1.l, t1.o, t1.b, t.s, t.i, t.l, t.o, t.b FROM TestEntity t1 INNER JOIN TestEntity t ON 1 = 1"
    }

    "for-comprehension with field" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        for {
          t1 <- query[TestEntity]
          t2 <- query[TestEntity].join(t => t.b)
        } yield (t1, t2)
      }
      ctx.run(q).string mustEqual
        "SELECT t1.s, t1.i, t1.l, t1.o, t1.b, t.s, t.i, t.l, t.o, t.b FROM TestEntity t1 INNER JOIN TestEntity t ON 1 = t.b"
    }
  }

  "unary operators" - {
    "constant" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        query[TestEntity].filter(t => !true)
      }
      ctx.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE NOT (1 = 1)"
    }
    "field" in testContext.withDialect(MirrorSqlDialectWithBooleanLiterals) { ctx =>
      import ctx._
      val q = quote {
        query[TestEntity].filter(t => !t.b)
      }
      ctx.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE NOT (1 = t.b)"
    }
  }
}
