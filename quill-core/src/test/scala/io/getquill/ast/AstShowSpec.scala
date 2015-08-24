package io.getquill.ast

import io.getquill._
import test.Spec
import language.reflectiveCalls

class AstShowSpec extends Spec {

  import io.getquill.util.Show._
  import io.getquill.ast.AstShow._

  "shows queries" in {
    val q = quote {
      queryable[TestEntity].filter(t => t.s == "test").flatMap(t => queryable[TestEntity]).map(t => t)
    }
    q.ast.show mustEqual
      """queryable[TestEntity].filter(t => t.s == "test").flatMap(t => queryable[TestEntity]).map(t => t)"""
  }

  "shows functions" in {
    val q = quote {
      (s: String) => s
    }
    q.ast.show mustEqual
      """((s) => s)"""
  }

  "shows operations" in {
    val q = quote {
      (xs: Queryable[_]) => !(xs.nonEmpty && xs != null)
    }
    q.ast.show mustEqual
      """((xs) => !(xs.nonEmpty && xs != null))"""
  }

  "shows unary operators" - {
    "prefix" - {
      "!" in {
        val q = quote {
          (s: String) => !(s == "s")
        }
        q.ast.show mustEqual
          """((s) => !(s == "s"))"""
      }
    }
    "prostfix" - {
      "isEmpty" in {
        val q = quote {
          (xs: Queryable[_]) => xs.isEmpty
        }
        q.ast.show mustEqual
          """((xs) => xs.isEmpty)"""
      }
      "nonEmpty" in {
        val q = quote {
          (xs: Queryable[_]) => xs.nonEmpty
        }
        q.ast.show mustEqual
          """((xs) => xs.nonEmpty)"""
      }
    }
  }

  "shows binary operators" - {
    "-" in {
      val q = quote {
        (a: Int, b: Int) => a - b
      }
      q.ast.show mustEqual
        """((a, b) => a - b)"""
    }
    "+" in {
      val q = quote {
        (a: Int, b: Int) => a + b
      }
      q.ast.show mustEqual
        """((a, b) => a + b)"""
    }
    "*" in {
      val q = quote {
        (a: Int, b: Int) => a * b
      }
      q.ast.show mustEqual
        """((a, b) => a * b)"""
    }
    "==" in {
      val q = quote {
        (a: Int, b: Int) => a == b
      }
      q.ast.show mustEqual
        """((a, b) => a == b)"""
    }
    "!=" in {
      val q = quote {
        (a: Int, b: Int) => a != b
      }
      q.ast.show mustEqual
        """((a, b) => a != b)"""
    }
    "&&" in {
      val q = quote {
        (a: Boolean, b: Boolean) => a && b
      }
      q.ast.show mustEqual
        """((a, b) => a && b)"""
    }
    "||" in {
      val q = quote {
        (a: Boolean, b: Boolean) => a || b
      }
      q.ast.show mustEqual
        """((a, b) => a || b)"""
    }
    ">" in {
      val q = quote {
        (a: Int, b: Int) => a > b
      }
      q.ast.show mustEqual
        """((a, b) => a > b)"""
    }
    ">=" in {
      val q = quote {
        (a: Int, b: Int) => a >= b
      }
      q.ast.show mustEqual
        """((a, b) => a >= b)"""
    }
    "<" in {
      val q = quote {
        (a: Int, b: Int) => a < b
      }
      q.ast.show mustEqual
        """((a, b) => a < b)"""
    }
    "<=" in {
      val q = quote {
        (a: Int, b: Int) => a <= b
      }
      q.ast.show mustEqual
        """((a, b) => a <= b)"""
    }
    "/" in {
      val q = quote {
        (a: Int, b: Int) => a / b
      }
      q.ast.show mustEqual
        """((a, b) => a / b)"""
    }
    "%" in {
      val q = quote {
        (a: Int, b: Int) => a % b
      }
      q.ast.show mustEqual
        """((a, b) => a % b)"""
    }
  }

  "shows properties" in {
    val q = quote {
      (e: TestEntity) => e.s
    }
    q.ast.show mustEqual
      """((e) => e.s)"""
  }

  "shows values" - {
    "constant" - {
      "string" in {
        val q = quote {
          "test"
        }
        q.ast.show mustEqual
          """"test""""
      }
      "unit" in {
        val q = quote {
          {}
        }
        q.ast.show mustEqual
          """{}"""
      }
      "value" in {
        val q = quote {
          1
        }
        q.ast.show mustEqual
          """1"""
      }
    }
    "null" in {
      val q = quote {
        null
      }
      q.ast.show mustEqual
        """null"""
    }
    "tuple" in {
      val q = quote {
        (null, 1, "a")
      }
      q.ast.show mustEqual
        """(null, 1, "a")"""
    }
  }

  "shows idents" in {
    val q = quote {
      (a: String) => a
    }
    q.ast.show mustEqual
      """((a) => a)"""
  }

  //  "shows actions" - {
  //    "update" in {
  //      val q = quote {
  //        queryable[TestEntity].filter(t => t.s == "test").update(t => t.s -> "a")
  //      }
  //      q.ast.show mustEqual
  //        """queryable[TestEntity].filter(t => t.s == "test").update(t => t.s ->  "a")"""
  //    }
  //  }
}
