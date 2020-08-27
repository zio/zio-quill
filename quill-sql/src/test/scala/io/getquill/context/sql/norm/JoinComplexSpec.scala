package io.getquill.context.sql.norm

import io.getquill.Spec
import io.getquill.context.sql.testContext
import io.getquill.Query

// Advanced spec for join queries that tests various complex use cases
class JoinComplexSpec extends Spec {
  import testContext._

  "ExpandJoin should behave normally with: join + distinct + leftjoin" in {
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

  "ExpandJoin should behave normally with: join + distinct + leftjoin - with additional filter" in {
    val q = quote {
      (qr1.filter(a => a.i == 1)).leftJoin(qr2).on {
        (a, b) => a.i == b.i
      }.distinct.leftJoin(qr3).on {
        (ab, c) =>
          ab._2.map(_.i).contains(ab._1.i) && ab._2.map(_.i).contains(c.i)
      }
    }
    testContext.run(q).string mustEqual
      "SELECT ab._1s, ab._1i, ab._1l, ab._1o, ab._1b, ab._2s, ab._2i, ab._2l, ab._2o, c.s, c.i, c.l, c.o FROM (SELECT DISTINCT a.s AS _1s, a.i AS _1i, a.l AS _1l, a.o AS _1o, a.b AS _1b, b.s AS _2s, b.i AS _2i, b.l AS _2l, b.o AS _2o FROM (SELECT a.s, a.i, a.l, a.o, a.b FROM TestEntity a WHERE a.i = 1) AS a LEFT JOIN TestEntity2 b ON a.i = b.i) AS ab LEFT JOIN TestEntity3 c ON ab._2i = ab._1i AND ab._2i = c.i"
  }

  "FlatJoin should function properly when plugged in with a shadow" - {
    case class FooEntity(fs: String, fi: Int, fl: Long, fo: Option[Int], fb: Boolean)

    val first = quote {
      (tbl: Query[TestEntity]) =>
        for {
          t <- tbl
          a <- qr3.join(a => a.i == t.i)
        } yield (t, a)
    }

    "using filter" in {
      testContext.run(first(qr1.filter(a => a.i == 1))).string mustEqual
        "SELECT a.s, a.i, a.l, a.o, a.b, a1.s, a1.i, a1.l, a1.o FROM TestEntity a INNER JOIN TestEntity3 a1 ON a1.i = a.i WHERE a.i = 1"
    }

    "using map" in {
      testContext.run(first(query[FooEntity].map(a => TestEntity(a.fs, a.fi, a.fl, a.fo, a.fb)))).string mustEqual
        "SELECT a.fs, a.fi, a.fl, a.fo, a.fb, a1.s, a1.i, a1.l, a1.o FROM FooEntity a INNER JOIN TestEntity3 a1 ON a1.i = a.fi"
    }

    val second = quote {
      (tbl: Query[TestEntity]) =>
        for {
          f <- qr3
          t <- tbl.join(a => a.i == f.i)
          a <- qr3.join(a => a.i == t.i)
        } yield (t, a)
    }

    "using filter - second clause - double alias" in {
      testContext.run(second(qr1.filter(a => a.i == 1))).string mustEqual
        "SELECT a.s, a.i, a.l, a.o, a.b, a1.s, a1.i, a1.l, a1.o FROM TestEntity3 f INNER JOIN (SELECT a.s, a.i, a.l, a.o, a.b FROM TestEntity a WHERE a.i = 1) AS a ON a.i = f.i INNER JOIN TestEntity3 a1 ON a1.i = a.i"
    }

    "using map - second clause - double alias" in {
      testContext.run(second(query[FooEntity].map(a => TestEntity(a.fs, a.fi, a.fl, a.fo, a.fb)))).string mustEqual
        "SELECT a.s, a.i, a.l, a.o, a.b, a1.s, a1.i, a1.l, a1.o FROM TestEntity3 f INNER JOIN (SELECT a.fs AS s, a.fi AS i, a.fl AS l, a.fo AS o, a.fb AS b FROM FooEntity a) AS a ON a.i = f.i INNER JOIN TestEntity3 a1 ON a1.i = a.i"
    }
  }
}
