package io.getquill.source.sql.idiom

import io.getquill.Spec
import io.getquill.source.sql.mirror.MirrorSourceTemplate
import io.getquill.naming._
import io.getquill._
import io.getquill.util.Show._
import io.getquill.ast.Ast
import FallbackDialect._

class SqlIdiomNamingSpec extends Spec {

  "uses the naming strategy" - {

    case class TestEntity(someColumn: Int)

    "one transformation" in {
      object db extends MirrorSourceTemplate[SnakeCase]
      db.run(query[TestEntity]).sql mustEqual
        "SELECT x.some_column FROM test_entity x"
    }
    "mutiple transformations" in {
      object db extends MirrorSourceTemplate[SnakeCase with UpperCase with Escape]
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
    "actions" - {
      "insert" in {
        object db extends MirrorSourceTemplate[SnakeCase]
        db.run(query[TestEntity].insert)(List()).sql mustEqual
          "INSERT INTO test_entity (some_column) VALUES (?)"
      }
      "update" in {
        object db extends MirrorSourceTemplate[SnakeCase]
        db.run(query[TestEntity].update)(List()).sql mustEqual
          "UPDATE test_entity SET some_column = ?"
      }
      "delete" in {
        object db extends MirrorSourceTemplate[SnakeCase]
        db.run(query[TestEntity].delete).sql mustEqual
          "DELETE FROM test_entity"
      }
    }
  }
}
