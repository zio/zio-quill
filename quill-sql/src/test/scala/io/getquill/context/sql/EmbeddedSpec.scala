package io.getquill.context.sql

import io.getquill._

class EmbeddedSpec extends Spec {

  val ctx = new SqlMirrorContext(MirrorSqlDialect, Literal) with TestEntities
  import ctx._

  "queries with embedded entities should" - {
    "function property inside of nested distinct queries" in {
      case class Parent(id: Int, emb1: Emb)
      case class Emb(a: Int, b: Int) extends Embedded
      val q = quote {
        query[Emb].map(e => Parent(1, e)).distinct
      }
      ctx.run(q).string mustEqual "SELECT e.id, e.a, e.b FROM (SELECT DISTINCT 1 AS id, e.a AS a, e.b AS b FROM Emb e) AS e"
    }
  }

}
