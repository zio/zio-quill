package io.getquill.context.mirror

import io.getquill.MirrorIdiom
import io.getquill.MirrorContexts.testContext._
import io.getquill.idiom.StatementInterpolator._
import io.getquill.Literal
import io.getquill.ast.Ast
import io.getquill.ast._
import io.getquill.Ord
import io.getquill.Query
import io.getquill.Quoted
import io.getquill.base.Spec

class MirrorIdiomSpec extends Spec {

  import MirrorIdiom._

  implicit val naming = Literal

  "shows schema query" - {
    "entity" in {
      val q = quote {
        querySchema[TestEntity]("test")
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""`querySchema`("test")"""
    }
    "columns" in {
      val q = quote {
        querySchema[TestEntity]("TestEntity", _.i -> "'i", _.o -> "'o")
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""`querySchema`("TestEntity", _.i -> "'i", _.o -> "'o")"""
    }
  }

  "shows queries" - {
    "complex" in {
      val q = quote {
        query[TestEntity].filter(t => t.s == "test").flatMap(t => query[TestEntity]).drop(9).take(10).map(t => t)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").filter(t => t.s == "test").flatMap(t => querySchema("TestEntity")).drop(9).take(10).map(t => t)"""
    }
  }

  "shows set operation queries" - {
    "union" in {
      val q = quote {
        qr1.filter(a => a.s == "s").union(qr1.filter(b => b.i == 1))
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").filter(a => a.s == "s").union(querySchema("TestEntity").filter(b => b.i == 1))"""
    }
    "unionAll" in {
      val q = quote {
        qr1.filter(a => a.s == "s").unionAll(qr1.filter(b => b.i == 1))
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").filter(a => a.s == "s").unionAll(querySchema("TestEntity").filter(b => b.i == 1))"""
    }
  }

  "shows join queries" - {
    "inner join" in {
      val q = quote {
        qr1.join(qr2).on((a, b) => a.s == b.s)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").join(querySchema("TestEntity2")).on((a, b) => a.s == b.s)"""
    }
    "left join" in {
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => a.s == b.s)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").leftJoin(querySchema("TestEntity2")).on((a, b) => a.s == b.s)"""
    }
    "right join" in {
      val q = quote {
        qr1.rightJoin(qr2).on((a, b) => a.s == b.s)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").rightJoin(querySchema("TestEntity2")).on((a, b) => a.s == b.s)"""
    }
    "full join" in {
      val q = quote {
        qr1.fullJoin(qr2).on((a, b) => a.s == b.s)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").fullJoin(querySchema("TestEntity2")).on((a, b) => a.s == b.s)"""
    }
    "flat join" in {
      val q = quote {
        qr1.join(x => true)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").join((x) => true)"""
    }
  }

  "shows sorted queries" - {
    "asc" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.asc)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").sortBy(t => t.i)(Ord.asc)"""
    }
    "desc" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.desc)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").sortBy(t => t.i)(Ord.desc)"""
    }
    "ascNullsFirst" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.ascNullsFirst)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").sortBy(t => t.i)(Ord.ascNullsFirst)"""
    }
    "descNullsFirst" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.descNullsFirst)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").sortBy(t => t.i)(Ord.descNullsFirst)"""
    }
    "ascNullsLast" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.ascNullsLast)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").sortBy(t => t.i)(Ord.ascNullsLast)"""
    }
    "descNullsLast" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.descNullsLast)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").sortBy(t => t.i)(Ord.descNullsLast)"""
    }
    "tuple" in {
      val q = quote {
        qr1.sortBy(t => (t.i, t.s))(Ord(Ord.descNullsLast, Ord.ascNullsLast))
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").sortBy(t => (t.i, t.s))(Ord(Ord.descNullsLast, Ord.ascNullsLast))"""
    }
  }

  "shows grouped queries" in {
    val q = quote {
      qr1.groupBy(t => t.i)
    }
    stmt"${(q.ast: Ast).token}" mustEqual
      stmt"""querySchema("TestEntity").groupBy(t => t.i)"""
  }

  "shows functions" in {
    val q = quote { (s: String) =>
      s
    }
    stmt"${(q.ast: Ast).token}" mustEqual
      stmt"""(s) => s"""
  }

  "shows operations" - {
    "unary" in {
      val q = quote { (xs: Query[_]) =>
        !xs.nonEmpty
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(xs) => !xs.nonEmpty"""
    }
    "binary" in {
      val q = quote { (xs: Query[_]) =>
        xs.nonEmpty && xs != null
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(xs) => xs.nonEmpty && (xs != null)"""
    }
    "function apply" - {
      "function reference" in {
        val q = quote { (s: String => String) =>
          s("a")
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"""(s) => s.apply("a")"""
      }
      "local function" in {
        val q = quote {
          ((s: String) => s)("s")
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt""""s""""
      }
    }
  }

  "shows aggregations" - {
    "min" in {
      val q = quote {
        qr1.map(t => t.i).min
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").map(t => t.i).min"""
    }
    "max" in {
      val q = quote {
        qr1.map(t => t.i).max
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").map(t => t.i).max"""
    }
    "avg" in {
      val q = quote {
        qr1.map(t => t.i).avg
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").map(t => t.i).avg"""
    }
    "sum" in {
      val q = quote {
        qr1.map(t => t.i).sum
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").map(t => t.i).sum"""
    }
    "size" in {
      val q = quote {
        qr1.map(t => t.i).size
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").map(t => t.i).size"""
    }
  }

  "shows unary operators" - {
    "prefix" - {
      "!" in {
        val q = quote { (s: String) =>
          !(s == "s")
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"""(s) => !(s == "s")"""
      }
    }
    "postfix" - {
      "isEmpty" in {
        val q = quote { (xs: Query[_]) =>
          xs.isEmpty
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"""(xs) => xs.isEmpty"""
      }
      "nonEmpty" in {
        val q = quote { (xs: Query[_]) =>
          xs.nonEmpty
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"""(xs) => xs.nonEmpty"""
      }
    }
  }

  "shows binary operators" - {
    "-" in {
      val q = quote { (a: Int, b: Int) =>
        a - b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a - b"""
    }
    "+" in {
      val q = quote { (a: Int, b: Int) =>
        a + b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a + b"""
    }
    "*" in {
      val q = quote { (a: Int, b: Int) =>
        a * b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a * b"""
    }
    "==" in {
      val q = quote { (a: Int, b: Int) =>
        a == b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a == b"""
    }
    "!=" in {
      val q = quote { (a: Int, b: Int) =>
        a != b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a != b"""
    }
    "&&" in {
      val q = quote { (a: Boolean, b: Boolean) =>
        a && b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a && b"""
    }
    "||" in {
      val q = quote { (a: Boolean, b: Boolean) =>
        a || b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a || b"""
    }
    ">" in {
      val q = quote { (a: Int, b: Int) =>
        a > b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a > b"""
    }
    ">=" in {
      val q = quote { (a: Int, b: Int) =>
        a >= b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a >= b"""
    }
    "<" in {
      val q = quote { (a: Int, b: Int) =>
        a < b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a < b"""
    }
    "<=" in {
      val q = quote { (a: Int, b: Int) =>
        a <= b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a <= b"""
    }
    "/" in {
      val q = quote { (a: Int, b: Int) =>
        a / b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a / b"""
    }
    "%" in {
      val q = quote { (a: Int, b: Int) =>
        a % b
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(a, b) => a % b"""
    }
  }

  "shows properties" in {
    val q = quote { (e: TestEntity) =>
      e.s
    }
    stmt"${(q.ast: Ast).token}" mustEqual
      stmt"""(e) => e.s"""
  }

  "shows values" - {
    "constant" - {
      "string" in {
        val q = quote {
          "test"
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt""""test""""
      }
      "unit" in {
        // format: off
        val q = quote {
          {}
        }
        // format: on
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"""{}"""
      }
      "value" in {
        val q = quote {
          1
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"""1"""
      }
    }
    "null" in {
      val q = quote {
        "a" != null
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt""""a" != null"""
    }
    "tuple" in {
      val q = quote {
        (null, 1, "a")
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""(null, 1, "a")"""
    }
  }

  "shows idents" in {
    val q = quote { (a: String) =>
      a
    }
    stmt"${(q.ast: Ast).token}" mustEqual
      stmt"""(a) => a"""
  }

  "shows actions" - {
    "update" in {
      val q = quote {
        query[TestEntity].filter(t => t.s == "test").update(t => t.s -> "a")
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").filter(t => t.s == "test").update(t => t.s -> "a")"""
    }
    "insert" - {
      "normal" in {
        val q = quote {
          query[TestEntity].insert(t => t.s -> "a")
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"""querySchema("TestEntity").insert(t => t.s -> "a")"""
      }

      "returning" in {
        val q = quote {
          query[TestEntity].insert(t => t.s -> "a").returning(t => t.l)
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"""querySchema("TestEntity").insert(t => t.s -> "a").returning((t) => t.l)"""
      }
    }

    "delete" in {
      val q = quote {
        query[TestEntity].delete
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").delete"""
    }

    "onConflict" - {
      val i = quote {
        query[TestEntity].insert(t => t.s -> "a")
      }
      val t = stmt"""querySchema("TestEntity").insert(t => t.s -> "a")"""
      "onConflictIgnore" in {
        stmt"${(i.onConflictIgnore.ast: Ast).token}" mustEqual
          stmt"$t.onConflictIgnore"
      }
      "onConflictIgnore(targets*)" in {
        stmt"${(i.onConflictIgnore(_.i, _.s).ast: Ast).token}" mustEqual
          stmt"$t.onConflictIgnore(_.i, _.s)"
      }
      "onConflictUpdate(assigns*)" in {
        stmt"${(i.onConflictUpdate((t, e) => t.s -> e.s, (t, e) => t.i -> (t.i + 1)).ast: Ast).token}" mustEqual
          stmt"$t.onConflictUpdate((t, e) => t.s -> e.s, (t, e) => t.i -> (t.i + 1))"
      }
      "onConflictUpdate(targets*)(assigns*)" in {
        stmt"${(i.onConflictUpdate(_.i)((t, e) => t.s -> e.s).ast: Ast).token}" mustEqual
          stmt"$t.onConflictUpdate(_.i)((t, e) => t.s -> e.s)"
      }
    }
  }

  "shows sql" - {
    "as part of the query" in {
      val q = quote {
        qr1.filter(t => sql"true".as[Boolean])
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").filter(t => sql"true")"""
    }
    "with params" in {
      val q = quote {
        qr1.filter(t => sql"${t.s} == 's'".as[Boolean])
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""querySchema("TestEntity").filter(t => sql"$${t.s} == 's'")"""
    }
    "as quoted" in {
      implicit class RichQuoted[T](q: Quoted[Query[T]]) {
        def func = quote(sql"$q.func".as[Query[T]])
      }
      val q = quote {
        qr1.func
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"""sql"$${querySchema("TestEntity")}.func""""

    }
  }

  "shows inline statement" in {
    val q = quote {
      val a = 1
      1 / a
    }
    stmt"${(q.ast: Ast).token}" mustEqual stmt"1 / 1"
  }

  "shows option operations" - {
    case class Row(id: Int, value: String)

    "getOrElse" in {
      val q = quote { (o: Option[Int]) =>
        o.getOrElse(1)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"(o) => o.getOrElse(1)"
    }
    "flatten" in {
      val q = quote { (o: Option[Option[Int]]) =>
        o.flatten
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"(o) => o.flatten"
    }
    "flatMap" - {
      "regular" in {
        val q = quote { (o: Option[Option[Int]]) =>
          o.flatMap(v => v)
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"(o) => o.flatMap((v) => v)"
      }
      "row" in {
        val q = quote { (o: Option[Option[Row]]) =>
          o.flatMap(v => v)
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"(o) => o.flatMap((v) => v)"
      }
    }
    "map" - {
      "regular" in {
        val q = quote { (o: Option[Int]) =>
          o.map(v => v)
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"(o) => o.map((v) => v)"
      }
      "row" in {
        val q = quote { (o: Option[Row]) =>
          o.map(v => v)
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"(o) => o.map((v) => v)"
      }
    }
    "forall" - {
      "regular" in {
        val q = quote { (o: Option[Boolean]) =>
          o.forall(v => v)
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"(o) => o.forall((v) => v)"
      }
      "row" in {
        val q = quote { (o: Option[Row]) =>
          o.exists(v => v.id == 1)
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"(o) => o.exists((v) => v.id == 1)"
      }
    }
    "exists" - {
      "regular" in {
        val q = quote { (o: Option[Boolean]) =>
          o.exists(v => v)
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"(o) => o.exists((v) => v)"
      }
      "row" in {
        val q = quote { (o: Option[Row]) =>
          o.exists(v => v.id == 1)
        }
        stmt"${(q.ast: Ast).token}" mustEqual
          stmt"(o) => o.exists((v) => v.id == 1)"
      }
    }
    "contains" in {
      val q = quote { (o: Option[Boolean]) =>
        o.contains(true)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"(o) => o.contains(true)"
    }
    "isEmpty" in {
      val q = quote { (o: Option[Boolean]) =>
        o.isEmpty
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"(o) => o.isEmpty"
    }
    "nonEmpty" in {
      val q = quote { (o: Option[Boolean]) =>
        o.nonEmpty
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"(o) => o.nonEmpty"
    }
    "isDefined" in {
      val q = quote { (o: Option[Boolean]) =>
        o.isDefined
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"(o) => o.isDefined"
    }
  }

  "show traversable operation" - {
    "MapContains" in {
      val q = quote { (m: Predef.Map[Int, Int]) =>
        m.contains(1)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"(m) => m.contains(1)"
    }
    "SetContains" in {
      val q = quote { (s: Set[Int]) =>
        s.contains(1)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"(s) => s.contains(1)"
    }
    "ListContains" in {
      val q = quote { (l: List[Int]) =>
        l.contains(1)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"(l) => l.contains(1)"
    }
  }

  "shows bindings" - {
    "quotedReference" in {
      val ast: Ast = QuotedReference("ignore", Filter(Ident("a"), Ident("b"), Ident("c")))
      stmt"${(ast: Ast).token}" mustEqual
        stmt"""a.filter(b => c)"""
    }

    "lift" in {
      val q = quote {
        lift(1)
      }
      stmt"${(q.ast: Ast).token}" mustEqual
        stmt"${(q.ast: Lift).token}"
    }
  }

  "shows dynamic asts" in {
    stmt"${(Dynamic(1): Ast).token}" mustEqual
      stmt"1"
  }

  "shows if" in {
    val q = quote { (i: Int) =>
      if (i > 10) "a" else "b"
    }
    stmt"${(q.ast.body: Ast).token}" mustEqual
      stmt"""if(i > 10) "a" else "b""""
  }

  "shows distinct" in {
    val q = quote {
      query[TestEntity].distinct
    }
    stmt"${(q.ast: Ast).token}" mustEqual
      stmt"""querySchema("TestEntity").distinct"""
  }

  "shows nested" in {
    val q = quote {
      query[TestEntity].nested
    }
    stmt"${(q.ast: Ast).token}" mustEqual
      stmt"""querySchema("TestEntity").nested"""
  }

  "liftTokenizer" in {
    val q = quote {
      qr1.map(x => lift(1))
    }
    q.ast.toString mustEqual """querySchema("TestEntity").map(x => ?)"""
  }
}
