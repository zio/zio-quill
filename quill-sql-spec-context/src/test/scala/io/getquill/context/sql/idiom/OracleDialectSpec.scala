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

  "Insert with returning" - {
    "with single column table" in {
      val q = quote {
        qr4.insert(lift(TestEntity4(0))).returning(_.i)
      }
      ctx.run(q).string mustEqual
        "INSERT INTO TestEntity4 (i) VALUES (?)"
    }

    "returning generated with single column table" in {
      val q = quote {
        qr4.insert(lift(TestEntity4(0))).returningGenerated(_.i)
      }
      ctx.run(q).string mustEqual
        "INSERT INTO TestEntity4 (i) VALUES (DEFAULT)"
    }
    "returning with multi column table" in {
      val q = quote {
        qr1.insert(lift(TestEntity("s", 0, 0L, Some(3), true))).returning(r => (r.i, r.l))
      }
      ctx.run(q).string mustEqual
        "INSERT INTO TestEntity (s,i,l,o,b) VALUES (?, ?, ?, ?, ?)"
    }
    "returning generated with multi column table" in {
      val q = quote {
        qr1.insert(lift(TestEntity("s", 0, 0L, Some(3), true))).returningGenerated(r => (r.i, r.l))
      }
      ctx.run(q).string mustEqual
        "INSERT INTO TestEntity (s,o,b) VALUES (?, ?, ?)"
    }
    "returning - multiple fields + operations - should not compile" in {
      val q = quote {
        qr1.insert(lift(TestEntity("s", 1, 2L, Some(3), true)))
      }
      "ctx.run(q.returning(r => (r.i, r.l + 1))).string" mustNot compile
    }
    "returning generated - multiple fields + operations - should not compile" in {
      val q = quote {
        qr1.insert(lift(TestEntity("s", 1, 2L, Some(3), true)))
      }
      "ctx.run(q.returningGenerated(r => (r.i, r.l + 1))).string" mustNot compile
    }
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
      ctx.run("foo" + infix"""'bar'""".as[String]).string mustEqual "SELECT 'foo' || 'bar' FROM DUAL"
    }
  }

  "literal booleans" - {
    "boolean expressions" - {
      "uses 1 = 1 instead of true" in {
        ctx.run(qr4.filter(t => true)).string mustEqual
          "SELECT t.i FROM TestEntity4 t WHERE 1 = 1"
      }
      "uses 1 = 0 instead of false" in {
        ctx.run(qr4.filter(t => false)).string mustEqual
          "SELECT t.i FROM TestEntity4 t WHERE 1 = 0"
      }
      "uses 1 = 0 and 1 = 1 altogether" in {
        ctx.run(qr4.filter(t => false).filter(t => true)).string mustEqual
          "SELECT t.i FROM TestEntity4 t WHERE 1 = 0 AND 1 = 1"
      }
    }
    "boolean values" - {
      "uses 1 instead of true" in {
        ctx.run(qr4.map(t => (t.i, true))).string mustEqual
          "SELECT t.i, 1 FROM TestEntity4 t"
      }
      "uses 0 instead of false" in {
        ctx.run(qr4.map(t => (t.i, false))).string mustEqual
          "SELECT t.i, 0 FROM TestEntity4 t"
      }
      "uses 0 and 1 altogether" in {
        ctx.run(qr4.map(t => (t.i, true, false))).string mustEqual
          "SELECT t.i, 1, 0 FROM TestEntity4 t"
      }
    }
    "boolean values and expressions together" in {
      ctx.run(qr4.filter(t => true).filter(t => false).map(t => (t.i, false, true))).string mustEqual
        "SELECT t.i, 0, 1 FROM TestEntity4 t WHERE 1 = 1 AND 1 = 0"
    }
    "if" - {
      "simple booleans" in {
        val q = quote {
          qr1.map(t => if (true) true else false)
        }
        ctx.run(q).string mustEqual
          "SELECT CASE WHEN 1 = 1 THEN 1 ELSE 0 END FROM TestEntity t"
      }
      "nested conditions" - {
        "inside then" in {
          val q = quote {
            qr1.map(t => if (true) {
              if (false) true else false
            } else true)
          }
          ctx.run(q).string mustEqual
            "SELECT CASE WHEN 1 = 1 THEN CASE WHEN 1 = 0 THEN 1 ELSE 0 END ELSE 1 END FROM TestEntity t"
        }
        "inside else" in {
          val q = quote {
            qr1.map(t => if (true) true else if (false) true else false)
          }
          ctx.run(q).string mustEqual
            "SELECT CASE WHEN 1 = 1 THEN 1 WHEN 1 = 0 THEN 1 ELSE 0 END FROM TestEntity t"
        }
        "inside both" in {
          val q = quote {
            qr1.map(t => if (true) {
              if (false) true else false
            } else {
              if (true) false else true
            })
          }
          ctx.run(q).string mustEqual
            "SELECT CASE WHEN 1 = 1 THEN CASE WHEN 1 = 0 THEN 1 ELSE 0 END WHEN 1 = 1 THEN 0 ELSE 1 END FROM TestEntity t"
        }
      }
    }
  }
}