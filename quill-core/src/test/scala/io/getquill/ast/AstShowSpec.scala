package io.getquill.ast

import io.getquill._
import io.getquill.{ Query => QueryInterface }
import io.getquill.ast.AstShow.astShow
import io.getquill.util.Show.Shower

class AstShowSpec extends Spec {

  import io.getquill.util.Show._
  import io.getquill.ast.AstShow._

  "shows entity" - {
    "table" - {
      val q = quote {
        query[TestEntity](_.entity("test"))
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity](_.entity("test"))"""
    }
    "columns" - {
      val q = quote {
        query[TestEntity](_.columns(_.i -> "'i", _.o -> "'o"))
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity](_.columns(_.i -> "'i", _.o -> "'o"))"""
    }
    "generated" - {
      val q = quote {
        query[TestEntity](_.generated(c => c.i))
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity](_.generated(_.i))"""
    }
    "composed" - {
      val q = quote {
        query[TestEntity](_.entity("entity_alias").columns(_.s -> "s_alias", _.i -> "i_alias").generated(_.i))
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity](_.entity("entity_alias").columns(_.s -> "s_alias", _.i -> "i_alias").generated(_.i))"""
    }
  }

  "shows queries" - {

    "complex" in {
      val q = quote {
        query[TestEntity].filter(t => t.s == "test").flatMap(t => query[TestEntity]).drop(9).take(10).map(t => t)
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].filter(t => t.s == "test").flatMap(t => query[TestEntity]).drop(9).take(10).map(t => t)"""
    }
  }

  "shows set operation queries" - {
    "union" in {
      val q = quote {
        qr1.filter(a => a.s == "s").union(qr1.filter(b => b.i == 1))
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].filter(a => a.s == "s").union(query[TestEntity].filter(b => b.i == 1))"""
    }
    "unionAll" in {
      val q = quote {
        qr1.filter(a => a.s == "s").unionAll(qr1.filter(b => b.i == 1))
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].filter(a => a.s == "s").unionAll(query[TestEntity].filter(b => b.i == 1))"""
    }
  }

  "shows join queries" - {
    "inner join" in {
      val q = quote {
        qr1.join(qr2).on((a, b) => a.s == b.s)
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].join(query[TestEntity2]).on((a, b) => a.s == b.s)"""
    }
    "left join" in {
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => a.s == b.s)
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].leftJoin(query[TestEntity2]).on((a, b) => a.s == b.s)"""
    }
    "right join" in {
      val q = quote {
        qr1.rightJoin(qr2).on((a, b) => a.s == b.s)
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].rightJoin(query[TestEntity2]).on((a, b) => a.s == b.s)"""
    }
    "full join" in {
      val q = quote {
        qr1.fullJoin(qr2).on((a, b) => a.s == b.s)
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].fullJoin(query[TestEntity2]).on((a, b) => a.s == b.s)"""
    }
  }

  "shows sorted queries" - {
    "asc" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.asc)
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].sortBy(t => t.i)(Ord.asc)"""
    }
    "desc" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.desc)
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].sortBy(t => t.i)(Ord.desc)"""
    }
    "ascNullsFirst" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.ascNullsFirst)
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].sortBy(t => t.i)(Ord.ascNullsFirst)"""
    }
    "descNullsFirst" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.descNullsFirst)
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].sortBy(t => t.i)(Ord.descNullsFirst)"""
    }
    "ascNullsLast" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.ascNullsLast)
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].sortBy(t => t.i)(Ord.ascNullsLast)"""
    }
    "descNullsLast" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.descNullsLast)
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].sortBy(t => t.i)(Ord.descNullsLast)"""
    }
    "tuple" in {
      val q = quote {
        qr1.sortBy(t => (t.i, t.s))(Ord(Ord.descNullsLast, Ord.ascNullsLast))
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].sortBy(t => (t.i, t.s))(Ord(Ord.descNullsLast, Ord.ascNullsLast))"""
    }
  }

  "shows grouped queries" in {
    val q = quote {
      qr1.groupBy(t => t.i)
    }
    (q.ast: Ast).show mustEqual
      """query[TestEntity].groupBy(t => t.i)"""
  }

  "shows functions" in {
    val q = quote {
      (s: String) => s
    }
    (q.ast: Ast).show mustEqual
      """(s) => s"""
  }

  "shows operations" - {
    "unary" in {
      val q = quote {
        (xs: QueryInterface[_]) => !xs.nonEmpty
      }
      (q.ast: Ast).show mustEqual
        """(xs) => !xs.nonEmpty"""
    }
    "binary" in {
      val q = quote {
        (xs: QueryInterface[_]) => xs.nonEmpty && xs != null
      }
      (q.ast: Ast).show mustEqual
        """(xs) => xs.nonEmpty && (xs != null)"""
    }
    "function apply" - {
      "function reference" in {
        val q = quote {
          (s: String => String) => s("a")
        }
        (q.ast: Ast).show mustEqual
          """(s) => s.apply("a")"""
      }
      "local function" in {
        val q = quote {
          ((s: String) => s)("s")
        }
        (q.ast: Ast).show mustEqual
          """((s) => s).apply("s")"""
      }
    }
  }

  "shows aggregations" - {
    "min" in {
      val q = quote {
        qr1.map(t => t.i).min
      }
      (q.ast: Ast).show mustEqual
        "query[TestEntity].map(t => t.i).min"
    }
    "max" in {
      val q = quote {
        qr1.map(t => t.i).max
      }
      (q.ast: Ast).show mustEqual
        "query[TestEntity].map(t => t.i).max"
    }
    "avg" in {
      val q = quote {
        qr1.map(t => t.i).avg
      }
      (q.ast: Ast).show mustEqual
        "query[TestEntity].map(t => t.i).avg"
    }
    "sum" in {
      val q = quote {
        qr1.map(t => t.i).sum
      }
      (q.ast: Ast).show mustEqual
        "query[TestEntity].map(t => t.i).sum"
    }
    "size" in {
      val q = quote {
        qr1.map(t => t.i).size
      }
      (q.ast: Ast).show mustEqual
        "query[TestEntity].map(t => t.i).size"
    }
  }

  "shows unary operators" - {
    "prefix" - {
      "!" in {
        val q = quote {
          (s: String) => !(s == "s")
        }
        (q.ast: Ast).show mustEqual
          """(s) => !(s == "s")"""
      }
    }
    "prostfix" - {
      "isEmpty" in {
        val q = quote {
          (xs: QueryInterface[_]) => xs.isEmpty
        }
        (q.ast: Ast).show mustEqual
          """(xs) => xs.isEmpty"""
      }
      "nonEmpty" in {
        val q = quote {
          (xs: QueryInterface[_]) => xs.nonEmpty
        }
        (q.ast: Ast).show mustEqual
          """(xs) => xs.nonEmpty"""
      }
    }
  }

  "shows binary operators" - {
    "-" in {
      val q = quote {
        (a: Int, b: Int) => a - b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a - b"""
    }
    "+" in {
      val q = quote {
        (a: Int, b: Int) => a + b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a + b"""
    }
    "*" in {
      val q = quote {
        (a: Int, b: Int) => a * b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a * b"""
    }
    "==" in {
      val q = quote {
        (a: Int, b: Int) => a == b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a == b"""
    }
    "!=" in {
      val q = quote {
        (a: Int, b: Int) => a != b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a != b"""
    }
    "&&" in {
      val q = quote {
        (a: Boolean, b: Boolean) => a && b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a && b"""
    }
    "||" in {
      val q = quote {
        (a: Boolean, b: Boolean) => a || b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a || b"""
    }
    ">" in {
      val q = quote {
        (a: Int, b: Int) => a > b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a > b"""
    }
    ">=" in {
      val q = quote {
        (a: Int, b: Int) => a >= b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a >= b"""
    }
    "<" in {
      val q = quote {
        (a: Int, b: Int) => a < b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a < b"""
    }
    "<=" in {
      val q = quote {
        (a: Int, b: Int) => a <= b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a <= b"""
    }
    "/" in {
      val q = quote {
        (a: Int, b: Int) => a / b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a / b"""
    }
    "%" in {
      val q = quote {
        (a: Int, b: Int) => a % b
      }
      (q.ast: Ast).show mustEqual
        """(a, b) => a % b"""
    }
  }

  "shows properties" in {
    val q = quote {
      (e: TestEntity) => e.s
    }
    (q.ast: Ast).show mustEqual
      """(e) => e.s"""
  }

  "shows values" - {
    "constant" - {
      "string" in {
        val q = quote {
          "test"
        }
        (q.ast: Ast).show mustEqual
          """"test""""
      }
      "unit" in {
        val q = quote {
          {}
        }
        (q.ast: Ast).show mustEqual
          """{}"""
      }
      "value" in {
        val q = quote {
          1
        }
        (q.ast: Ast).show mustEqual
          """1"""
      }
    }
    "null" in {
      val q = quote {
        1 != null
      }
      (q.ast: Ast).show mustEqual
        """1 != null"""
    }
    "tuple" in {
      val q = quote {
        (null, 1, "a")
      }
      (q.ast: Ast).show mustEqual
        """(null, 1, "a")"""
    }
    "collection" - {
      def verify(ast: Ast) = ast.show mustEqual "Collection(1, 2, 3)"
      "set" in {
        val q = quote {
          Set(1, 2, 3)
        }
        verify(q.ast)
      }
      "list" in {
        val q = quote {
          List(1, 2, 3)
        }
        verify(q.ast)
      }
      "seq" in {
        val q = quote {
          Seq(1, 2, 3)
        }
        verify(q.ast)
      }
    }
  }

  "shows idents" in {
    val q = quote {
      (a: String) => a
    }
    (q.ast: Ast).show mustEqual
      """(a) => a"""
  }

  "shows actions" - {
    "update" - {
      "assigned" in {
        val q = quote {
          query[TestEntity].filter(t => t.s == "test").update(t => t.s -> "a")
        }
        (q.ast: Ast).show mustEqual
          """query[TestEntity].filter(t => t.s == "test").update(t => t.s -> "a")"""
      }
      "unassigned" in {
        val q = quote {
          query[TestEntity].filter(t => t.s == "test").update
        }
        (q.ast: Ast).show mustEqual
          """(x1) => query[TestEntity].filter(t => t.s == "test").update"""
      }
    }
    "insert" - {
      "assigned" in {
        val q = quote {
          query[TestEntity].insert(t => t.s -> "a")
        }
        (q.ast: Ast).show mustEqual
          """query[TestEntity].insert(t => t.s -> "a")"""
      }
      "unassigned" in {
        val q = quote {
          query[TestEntity].insert
        }
        (q.ast: Ast).show mustEqual
          """(x1) => query[TestEntity].insert"""
      }
    }

    "delete" in {
      val q = quote {
        query[TestEntity].delete
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].delete"""
    }
  }

  "shows infix" - {
    "as part of the query" in {
      val q = quote {
        qr1.filter(t => infix"true".as[Boolean])
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].filter(t => infix"true")"""
    }
    "with params" in {
      val q = quote {
        qr1.filter(t => infix"${t.s} == 's'".as[Boolean])
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].filter(t => infix"""" + "$" + """{t.s} == 's'")"""
    }
  }

  "shows inline statement" - {
    "block" in {
      val block = Block(List(
        Val(Ident("a"), Entity("a")),
        Val(Ident("b"), Entity("b"))
      ))
      (block: Ast).show mustEqual "[val a = query[a] val b = query[b]]"
    }
    "val" in {
      (Val(Ident("a"), Entity("a")): Ast).show mustEqual "val a = query[a]"
    }
  }

  "shows option operations" - {
    "map" in {
      val q = quote {
        (o: Option[Int]) => o.map(v => v)
      }
      (q.ast: Ast).show mustEqual
        "(o) => o.map((v) => v)"
    }
    "forall" in {
      val q = quote {
        (o: Option[Boolean]) => o.forall(v => v)
      }
      (q.ast: Ast).show mustEqual
        "(o) => o.forall((v) => v)"
    }
    "exists" in {
      val q = quote {
        (o: Option[Boolean]) => o.exists(v => v)
      }
      (q.ast: Ast).show mustEqual
        "(o) => o.exists((v) => v)"
    }
  }

  "shows bindings" - {
    "quotedReference" in {
      val ast: Ast = QuotedReference("ignore", Filter(Ident("a"), Ident("b"), Ident("c")))
      ast.show mustEqual
        """a.filter(b => c)"""
    }

    "compileTimeBinding" in {
      val ast: Ast = Filter(Ident("a"), Ident("b"), CompileTimeBinding("c"))
      ast.show mustEqual
        """a.filter(b => lift(c))"""
    }

    "runtimeBindings" in {
      val i = 1
      val q = quote {
        qr1.filter(x => x.i == i)
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].filter(x => x.i == i)"""
    }
  }

  "shows dynamic asts" - {
    (Dynamic(1): Ast).show mustEqual "1"
  }

  "shows if" - {
    val q = quote {
      (i: Int) =>
        if (i > 10) "a" else "b"
    }
    (q.ast.body: Ast).show mustEqual
      """if(i > 10) "a" else "b""""
  }

  "shows distinct" - {
    val q = quote {
      query[TestEntity].distinct
    }
    (q.ast: Ast).show mustEqual
      """query[TestEntity].distinct"""
  }

}
