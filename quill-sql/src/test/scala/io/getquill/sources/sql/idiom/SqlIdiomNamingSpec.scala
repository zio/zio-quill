package io.getquill.sources.sql.idiom

import io.getquill.sources.sql.mirrorSource._
import io.getquill.naming._
import io.getquill.util.Show._
import io.getquill.ast.Ast
import FallbackDialect._
import io.getquill.sources.sql.mirrorSource._
import io.getquill.sources.sql.mirror.SqlMirrorSource
import io.getquill.sources.sql.SqlSpec

class SqlIdiomNamingSpec extends SqlSpec {

  "uses the naming strategy" - {

    case class TestEntity(someColumn: Int)

    "one transformation" in {
      val db = new SqlMirrorSource[SnakeCase]
      db.run(query[TestEntity]).sql mustEqual
        "SELECT x.some_column FROM test_entity x"
    }
    "mutiple transformations" in {
      val db = new SqlMirrorSource[SnakeCase with UpperCase with Escape]
      db.run(query[TestEntity]).sql mustEqual
        """SELECT "X"."SOME_COLUMN" FROM "TEST_ENTITY" "X""""
    }
    "specific table strategy" in {
      implicit object CustomTableStrategy extends SnakeCase {
        override def table(s: String) = s"t_$s".toLowerCase
      }

      val q = quote {
        query[TestEntity].map(t => t.someColumn)
      }

      (q.ast: Ast).show mustEqual
        "SELECT t.some_column FROM t_testentity t"
    }
    "specific column strategy" in {
      implicit object CustomTableStrategy extends SnakeCase {
        override def column(s: String) = s"c_$s".toLowerCase
      }

      val q = quote {
        query[TestEntity].map(t => t.someColumn)
      }

      (q.ast: Ast).show mustEqual
        "SELECT t.c_somecolumn FROM test_entity t"
    }

    val db = new SqlMirrorSource[SnakeCase]

    "actions" - {
      "insert" in {
        db.run(query[TestEntity].insert)(List()).sql mustEqual
          "INSERT INTO test_entity (some_column) VALUES (?)"
      }
      "update" in {
        db.run(query[TestEntity].update)(List()).sql mustEqual
          "UPDATE test_entity SET some_column = ?"
      }
      "delete" in {
        db.run(query[TestEntity].delete).sql mustEqual
          "DELETE FROM test_entity"
      }
    }

    "queries" - {
      "property empty check" in {
        case class TestEntity(optionValue: Option[Int])
        db.run(query[TestEntity].filter(t => t.optionValue.isEmpty)).sql mustEqual
          "SELECT t.option_value FROM test_entity t WHERE t.option_value IS NULL"
      }
    }
  }
}
