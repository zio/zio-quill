package io.getquill.context.sql.idiom

import io.getquill._

class OracleDialectSpec extends Spec {

  val ctx = new SqlMirrorContext(OracleDialect, Literal) with TestEntities
  import ctx._

  "uses 'mod' function" in {
    ctx.run(qr1.map(t => t.i % 10)).string mustEqual
      "SELECT MOD(t.i, 10) FROM TestEntity t"
  }

  case class _UnderscoreEntity(_1: String, _2: Int, _3: Long)

  "quotes underscore columns and table aliases" - {
    "table and column select" in {
      ctx.run(query[_UnderscoreEntity].map(_t => (_t._1, _t._2))).string mustEqual
        """SELECT _t."_1", _t."_2" FROM "_UnderscoreEntity" "_t""""
    }
    "table and column insert" in {
      ctx.run(query[_UnderscoreEntity].insert(lift(_UnderscoreEntity("foo", 1, 1)))).string mustEqual
        """INSERT INTO "_UnderscoreEntity" ("_1","_2","_3") VALUES (?, ?, ?)"""
    }
  }

  "Insert with returning with single column table" in {
    val q = quote {
      qr4.insert(lift(TestEntity4(0))).returning(_.i)
    }
    ctx.run(q).string mustEqual
      "INSERT INTO TestEntity4 (i) VALUES (DEFAULT)"
  }

  "offset/fetch" - {
    val withOrd = quote {
      qr1.map(t => t.s)
    }

    def offset[T](q: Quoted[Query[T]]) = quote(q.drop(1))
    def offsetFetch[T](q: Quoted[Query[T]]) = quote(q.drop(2).take(3))
    def fetch[T](q: Quoted[Query[T]]) = quote(q.take(3))

    "offset" in {
      ctx.run(offset(withOrd)).string mustEqual
        "SELECT t.s FROM TestEntity t OFFSET 1 ROWS"
    }

    "offset with fetch " in {
      ctx.run(offsetFetch(withOrd)).string mustEqual
        "SELECT t.s FROM TestEntity t OFFSET 2 ROWS FETCH NEXT 3 ROWS ONLY"
    }

    "fetch" in {
      ctx.run(fetch(withOrd)).string mustEqual
        "SELECT t.s FROM TestEntity t FETCH FIRST 3 ROWS ONLY"
    }
  }

  "uses dual for scalars" - {
    "Simple Scalar Select" in {
      ctx.run(1).string mustEqual "SELECT 1 FROM DUAL"
    }

    "Multi Scalar Select" in {
      ctx.run(quote(1 + quote(1))).string mustEqual "SELECT 1 + 1 FROM DUAL"
    }

    "Multi Scalar Select with Infix" in {
      ctx.run("foo"+infix"""'bar'""".as[String]).string mustEqual "SELECT 'foo' || 'bar' FROM DUAL"
    }
  }

}
