package io.getquill.context.sql.idiom

import FallbackDialect._
import io.getquill.Spec
import io.getquill.ast.Ast
import io.getquill.context.sql.mirror.SqlMirrorContext
import io.getquill.context.sql.testContext
import io.getquill.naming.Escape
import io.getquill.naming.SnakeCase
import io.getquill.naming.UpperCase
import io.getquill.util.Show._

class SqlIdiomNamingSpec extends Spec {

  "uses the naming strategy" - {

    case class SomeEntity(someColumn: Int)

    "one transformation" in {
      val db = new SqlMirrorContext[SnakeCase]
      import db._
      db.run(query[SomeEntity]).sql mustEqual
        "SELECT x.some_column FROM some_entity x"
    }
    "mutiple transformations" in {
      val db = new SqlMirrorContext[SnakeCase with UpperCase with Escape]
      import db._
      db.run(query[SomeEntity]).sql mustEqual
        """SELECT "X"."SOME_COLUMN" FROM "SOME_ENTITY" "X""""
    }
    "specific table strategy" in {
      import testContext._
      implicit object CustomTableStrategy extends SnakeCase {
        override def table(s: String) = s"t_$s".toLowerCase
      }

      val q = quote {
        query[SomeEntity].map(t => t.someColumn)
      }

      (q.ast: Ast).show mustEqual
        "SELECT t.some_column FROM t_someentity t"
    }
    "specific column strategy" in {
      import testContext._
      implicit object CustomTableStrategy extends SnakeCase {
        override def column(s: String) = s"c_$s".toLowerCase
      }

      val q = quote {
        query[SomeEntity].map(t => t.someColumn)
      }

      (q.ast: Ast).show mustEqual
        "SELECT t.c_somecolumn FROM some_entity t"
    }

    val db = new SqlMirrorContext[SnakeCase]

    import db._

    "actions" - {
      "insert" in {
        db.run(query[SomeEntity].insert)(List()).sql mustEqual
          "INSERT INTO some_entity (some_column) VALUES (?)"
      }
      "update" in {
        db.run(query[SomeEntity].update)(List()).sql mustEqual
          "UPDATE some_entity SET some_column = ?"
      }
      "delete" in {
        db.run(query[SomeEntity].delete).sql mustEqual
          "DELETE FROM some_entity"
      }
    }

    "queries" - {
      "property empty check" in {
        case class SomeEntity(optionValue: Option[Int])
        db.run(query[SomeEntity].filter(t => t.optionValue.isEmpty)).sql mustEqual
          "SELECT t.option_value FROM some_entity t WHERE t.option_value IS NULL"
      }
    }
  }
}
