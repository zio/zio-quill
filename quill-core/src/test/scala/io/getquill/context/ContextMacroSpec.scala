package io.getquill.context

import io.getquill.MirrorContexts.testContext
import io.getquill.MirrorContexts.testContext._
import mirror.Row
import io.getquill.MirrorContext
import io.getquill.NamingStrategy
import io.getquill.idiom.Idiom
import io.getquill.MirrorIdiom
import io.getquill.TestEntities
import io.getquill.Literal
import io.getquill.Escape
import io.getquill.UpperCase
import io.getquill.SnakeCase
import io.getquill.Action
import io.getquill.Query
import io.getquill.base.Spec

class ContextMacroSpec extends Spec {

  "runs actions" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.delete
        }
        testContext.run(q).string mustEqual
          """querySchema("TestEntity").delete"""
      }
      "sql" in {
        val q = quote {
          sql"STRING".as[Action[TestEntity]]
        }
        testContext.run(q).string mustEqual
          """sql"STRING""""
      }
      "dynamic" in {
        val q = quote {
          qr1.delete
        }
        testContext.run(q.dynamic).string mustEqual
          """querySchema("TestEntity").delete"""
      }
      "dynamic type param" in {
        def test[T: SchemaMeta] = quote(query[T].delete)
        val r                   = testContext.run(test[TestEntity])
        r.string mustEqual """querySchema("TestEntity").delete"""
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          qr1.filter(t => t.s == lift("a")).delete
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").filter(t => t.s == ?).delete"""
        r.prepareRow mustEqual Row("a")
      }
      "sql" in {
        val q = quote {
          sql"t = ${lift("a")}".as[Action[TestEntity]]
        }
        val r = testContext.run(q)
        r.string mustEqual s"""sql"t = $${?}""""
        r.prepareRow mustEqual Row("a")
      }
      "dynamic" in {
        val q = quote {
          sql"t = ${lift("a")}".as[Action[TestEntity]]
        }
        val r = testContext.run(q.dynamic)
        r.string mustEqual s"""sql"t = $${?}""""
        r.prepareRow mustEqual Row("a")
      }
      "dynamic type param" in {
        import language.reflectiveCalls
        def test[T <: { def i: Int }: SchemaMeta] = quote {
          query[T].filter(t => t.i == lift(1)).delete
        }
        val r = testContext.run(test[TestEntity])
        r.string mustEqual """querySchema("TestEntity").filter(t => t.i == ?).delete"""
        r.prepareRow mustEqual Row(1)
      }
    }
  }

  "translate actions" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.delete
        }
        testContext.translate(q) mustEqual
          """querySchema("TestEntity").delete"""
      }
      "sql" in {
        val q = quote {
          sql"STRING".as[Action[TestEntity]]
        }
        testContext.translate(q) mustEqual
          """sql"STRING""""
      }
      "dynamic" in {
        val q = quote {
          qr1.delete
        }
        testContext.translate(q.dynamic) mustEqual
          """querySchema("TestEntity").delete"""
      }
      "dynamic type param" in {
        def test[T: SchemaMeta] = quote(query[T].delete)
        testContext.translate(test[TestEntity]) mustEqual
          """querySchema("TestEntity").delete"""
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          qr1.filter(t => t.s == lift("a")).delete
        }
        testContext.translate(q) mustEqual
          """querySchema("TestEntity").filter(t => t.s == lift('a')).delete"""
      }
      "sql" in {
        val q = quote {
          sql"t = ${lift("a")}".as[Action[TestEntity]]
        }
        testContext.translate(q) mustEqual s"""sql"t = $${lift('a')}""""
      }
      "dynamic" in {
        val q = quote {
          sql"t = ${lift("a")}".as[Action[TestEntity]]
        }
        testContext.translate(q.dynamic) mustEqual s"""sql"t = $${lift('a')}""""
      }
      "dynamic type param" in {
        import language.reflectiveCalls
        def test[T <: { def i: Int }: SchemaMeta] = quote {
          query[T].filter(t => t.i == lift(1)).delete
        }
        testContext.translate(test[TestEntity]) mustEqual
          """querySchema("TestEntity").filter(t => t.i == lift(1)).delete"""
      }
    }
  }

  "runs queries" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        testContext.run(q).string mustEqual
          """querySchema("TestEntity").map(t => t.s)"""
      }
      "sql" in {
        val q = quote {
          sql"STRING".as[Query[TestEntity]].map(t => t.s)
        }
        testContext.run(q).string mustEqual
          """sql"STRING".map(t => t.s)"""
      }
      "dynamic" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        testContext.run(q.dynamic).string mustEqual
          """querySchema("TestEntity").map(t => t.s)"""
      }
      "dynamic type param" in {
        def test[T: SchemaMeta] = quote(query[T])
        val r                   = testContext.run(test[TestEntity])
        r.string mustEqual """querySchema("TestEntity")"""
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          qr1.filter(t => t.s == lift("a"))
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").filter(t => t.s == ?)"""
        r.prepareRow mustEqual Row("a")
      }

      "value class" in {
        case class Entity(x: ValueClass)
        val q = quote {
          query[Entity].filter(t => t.x == lift(ValueClass(1)))
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("Entity").filter(t => t.x == ?)"""
        r.prepareRow mustEqual Row(1)
      }
      "generic value class" in {
        case class Entity(x: GenericValueClass[Int])
        val q = quote {
          query[Entity].filter(t => t.x == lift(GenericValueClass(1)))
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("Entity").filter(t => t.x == ?)"""
        r.prepareRow mustEqual Row(1)
      }
      "sql" in {
        val q = quote {
          sql"SELECT ${lift("a")}".as[Query[String]]
        }
        val r = testContext.run(q)
        r.string mustEqual s"""sql"SELECT $${?}".map(x => x)"""
        r.prepareRow mustEqual Row("a")
      }
      "dynamic" in {
        val q = quote {
          qr1.filter(t => t.s == lift("a"))
        }
        val r = testContext.run(q.dynamic)
        r.string mustEqual """querySchema("TestEntity").filter(t => t.s == ?)"""
        r.prepareRow mustEqual Row("a")
      }
      "dynamic type param" in {
        def test[T: SchemaMeta: QueryMeta] = quote {
          query[T].map(t => lift(1))
        }
        val r = testContext.run(test[TestEntity])
        r.string mustEqual """querySchema("TestEntity").map(t => ?)"""
        r.prepareRow mustEqual Row(1)
      }
    }
    "aggregated" in {
      val q = quote {
        qr1.map(t => t.i).max
      }
      testContext.run(q).string mustEqual
        """querySchema("TestEntity").map(t => t.i).max"""
    }
  }

  "translate queries" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        testContext.translate(q) mustEqual
          """querySchema("TestEntity").map(t => t.s)"""
      }
      "sql" in {
        val q = quote {
          sql"STRING".as[Query[TestEntity]].map(t => t.s)
        }
        testContext.translate(q) mustEqual
          """sql"STRING".map(t => t.s)"""
      }
      "dynamic" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        testContext.translate(q.dynamic) mustEqual
          """querySchema("TestEntity").map(t => t.s)"""
      }
      "dynamic type param" in {
        def test[T: SchemaMeta] = quote(query[T])
        testContext.translate(test[TestEntity]) mustEqual
          """querySchema("TestEntity")"""
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          qr1.filter(t => t.s == lift("a"))
        }
        testContext.translate(q) mustEqual
          """querySchema("TestEntity").filter(t => t.s == lift('a'))"""
      }

      "value class" in {
        case class Entity(x: ValueClass)
        val q = quote {
          query[Entity].filter(t => t.x == lift(ValueClass(1)))
        }
        testContext.translate(q) mustEqual
          """querySchema("Entity").filter(t => t.x == lift(ValueClass(1)))"""
      }
      "generic value class" in {
        case class Entity(x: GenericValueClass[Int])
        val q = quote {
          query[Entity].filter(t => t.x == lift(GenericValueClass(1)))
        }
        testContext.translate(q) mustEqual
          """querySchema("Entity").filter(t => t.x == lift(GenericValueClass(1)))"""
      }
      "sql" in {
        val q = quote {
          sql"SELECT ${lift("a")}".as[Query[String]]
        }
        testContext.translate(q) mustEqual s"""sql"SELECT $${lift('a')}".map(x => x)"""
      }
      "dynamic" in {
        val q = quote {
          qr1.filter(t => t.s == lift("a"))
        }
        testContext.translate(q.dynamic) mustEqual
          """querySchema("TestEntity").filter(t => t.s == lift('a'))"""
      }
      "dynamic type param" in {
        def test[T: SchemaMeta: QueryMeta] = quote {
          query[T].map(t => lift(1))
        }
        testContext.translate(test[TestEntity]) mustEqual
          """querySchema("TestEntity").map(t => lift(1))"""
      }
    }
    "aggregated" in {
      val q = quote {
        qr1.map(t => t.i).max
      }
      testContext.translate(q) mustEqual
        """querySchema("TestEntity").map(t => t.i).max"""
    }
  }

  "fails if there's a free variable" in {
    val q = {
      val i = 1
      quote {
        qr1.filter(_.i == i)
      }
    }
    "testContext.run(q)" mustNot compile
    "testContext.translate(q)" mustNot compile
  }

  "falls back to dynamic queries if idiom/naming are not known" in {
    import language.existentials
    def test(ctx: MirrorContext[_ <: Idiom, _ <: NamingStrategy]) = {
      import ctx._
      ctx.run(query[TestEntity])
    }

    def translateTest(ctx: MirrorContext[_ <: Idiom, _ <: NamingStrategy]) = {
      import ctx._
      ctx.translate(query[TestEntity])
    }

    test(testContext).string mustEqual """querySchema("TestEntity")"""
    translateTest(testContext) mustEqual """querySchema("TestEntity")"""
  }

  "supports composite naming strategies" - {
    "two" in {
      object ctx extends MirrorContext(MirrorIdiom, NamingStrategy(Literal, Escape)) with TestEntities
      import ctx._
      ctx.run(query[TestEntity]).string mustEqual """querySchema("TestEntity")"""
      ctx.translate(query[TestEntity]) mustEqual """querySchema("TestEntity")"""
    }
    "three" in {
      object ctx extends MirrorContext(MirrorIdiom, NamingStrategy(Literal, Escape, UpperCase)) with TestEntities
      import ctx._
      ctx.run(query[TestEntity]).string mustEqual """querySchema("TestEntity")"""
      ctx.translate(query[TestEntity]) mustEqual """querySchema("TestEntity")"""
    }
    "four" in {
      object ctx
          extends MirrorContext(MirrorIdiom, NamingStrategy(Literal, Escape, UpperCase, SnakeCase))
          with TestEntities
      import ctx._
      ctx.run(query[TestEntity]).string mustEqual """querySchema("TestEntity")"""
      ctx.translate(query[TestEntity]) mustEqual """querySchema("TestEntity")"""
    }
  }
}
