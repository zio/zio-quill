package io.getquill.context.sql.idiom

import io.getquill.Spec
import io.getquill.PostgresDialect
import io.getquill.SqlMirrorContext
import io.getquill.Literal
import io.getquill.util.Show._
import io.getquill.ast.Operation

class PostgresDialectSpec extends Spec {

  import PostgresDialect._

  "supports the `prepare` statement" in {
    val sql = "test"
    prepare(sql) mustEqual
      s"PREPARE p${preparedStatementId} AS $sql"
  }

  "applies explicit casts" - {
    implicit val naming = Literal
    val ctx = new SqlMirrorContext[Literal]
    import ctx._
    "toLong" in {
      (quote("a".toLong).ast: Operation).show mustEqual "'a'::bigint"
    }
    "toInt" in {
      (quote("a".toInt).ast: Operation).show mustEqual "'a'::integer"
    }
  }
}
