package io.getquill.ast

import scala.language.reflectiveCalls

import io.getquill._
import io.getquill.{ Query => QueryInterface }
import io.getquill.ast.AstShow.astShow
import io.getquill.util.Show.Shower

class AstShowSpec extends Spec {

  import io.getquill.util.Show._
  import io.getquill.ast.AstShow._

  "shows queries" in {
    val q = quote {
      query[TestEntity].filter(t => t.s == "test").flatMap(t => query[TestEntity]).drop(9).take(10).map(t => t)
    }
    (q.ast: Ast).show mustEqual
      """query[TestEntity].filter(t => t.s == "test").flatMap(t => query[TestEntity]).drop(9).take(10).map(t => t)"""
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

  "shows outer join queries" - {
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

  "shows sorted queries" in {
    val q = quote {
      qr1.sortBy(t => t.i).reverse
    }
    (q.ast: Ast).show mustEqual
      """query[TestEntity].sortBy(t => t.i).reverse"""
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
        null
      }
      (q.ast: Ast).show mustEqual
        """null"""
    }
    "tuple" in {
      val q = quote {
        (null, 1, "a")
      }
      (q.ast: Ast).show mustEqual
        """(null, 1, "a")"""
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
    "update" in {
      val q = quote {
        query[TestEntity].filter(t => t.s == "test").update(_.s -> "a")
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].filter(t => t.s == "test").update(_.s -> "a")"""
    }
    "insert" in {
      val q = quote {
        query[TestEntity].insert(_.s -> "a")
      }
      (q.ast: Ast).show mustEqual
        """query[TestEntity].insert(_.s -> "a")"""
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
}
