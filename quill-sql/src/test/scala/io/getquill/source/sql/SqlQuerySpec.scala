package io.getquill.source.sql

import io.getquill.ast._
import io.getquill._
import io.getquill.source.sql.test.mirrorSource

class SqlQuerySpec extends Spec {

  "transforms the ast into a flatten sql-like structure" - {
    val q = quote {
      for {
        a <- qr1
        b <- qr2 if (a.s != null && b.i > a.i)
      } yield {
        (a, b)
      }
    }
    val sqlq = SqlQuery(q.ast)
    sqlq.from mustEqual List(Source("TestEntity", "a"), Source("TestEntity2", "b"))
    sqlq.where.get.toString mustEqual "(a.s != null) && (b.i > a.i)"
    sqlq.select.toString mustEqual "(a, b)"
  }

  "fails if the query is not normalized" in {
    val q = quote {
      qr1.map(_.s).filter(_ == "s")
    }
    intercept[IllegalStateException] {
      SqlQuery(q.ast)
    }
  }
}
