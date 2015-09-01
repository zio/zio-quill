package io.getquill.ast

import scala.language.reflectiveCalls

import io.getquill.Queryable
import io.getquill.Spec
import io.getquill.ast.AstShow.astShow
import io.getquill.queryable
import io.getquill.quote
import io.getquill.unquote
import io.getquill.util.Show.Shower

class AstShowSpec extends Spec {

  import io.getquill.util.Show._
  import io.getquill.ast.AstShow._

  "shows queries" in {
    val q = quote {
      queryable[TestEntity].filter(t => t.s == "test").flatMap(t => queryable[TestEntity]).map(t => t)
    }
    (q.ast: Ast).show mustEqual
      """queryable[TestEntity].filter(t => t.s == "test").flatMap(t => queryable[TestEntity]).map(t => t)"""
  }

  "shows sorted queries" in {
    val q = quote {
      qr1.sortBy(t => t.i).reverse
    }
    (q.ast: Ast).show mustEqual
      """queryable[TestEntity].sortBy(t => t.i).reverse"""
  }

  "shows functions" in {
    val q = quote {
      (s: String) => s
    }
    (q.ast: Ast).show mustEqual
      """(s) => s"""
  }

  "shows function applies" - {
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

  "shows operations" in {
    val q = quote {
      (xs: Queryable[_]) => !(xs.nonEmpty && xs != null)
    }
    (q.ast: Ast).show mustEqual
      """(xs) => !(xs.nonEmpty && (xs != null))"""
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
          (xs: Queryable[_]) => xs.isEmpty
        }
        (q.ast: Ast).show mustEqual
          """(xs) => xs.isEmpty"""
      }
      "nonEmpty" in {
        val q = quote {
          (xs: Queryable[_]) => xs.nonEmpty
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
        queryable[TestEntity].filter(t => t.s == "test").update(_.s -> "a")
      }
      (q.ast: Ast).show mustEqual
        """queryable[TestEntity].filter(t => t.s == "test").update(_.s -> "a")"""
    }
    "insert" in {
      val q = quote {
        queryable[TestEntity].insert(_.s -> "a")
      }
      (q.ast: Ast).show mustEqual
        """queryable[TestEntity].insert(_.s -> "a")"""
    }

    "delete" in {
      val q = quote {
        queryable[TestEntity].delete
      }
      (q.ast: Ast).show mustEqual
        """queryable[TestEntity].delete"""
    }
  }
}
