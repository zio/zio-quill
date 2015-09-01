package io.getquill.source.sql

import io.getquill.Spec
import io.getquill.quote
import io.getquill.unquote

class SqlQuerySpec extends Spec {

  "transforms the ast into a flatten sql-like structure" - {
    "non-sorted query" in {
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
      sqlq.where.toString mustEqual "Some((a.s != null) && (b.i > a.i))"
      sqlq.select.toString mustEqual "(a, b)"
      sqlq.orderBy mustEqual List()
    }
    "sorted query" - {
      "with map" in {
        val q = quote {
          qr1.sortBy(t => t.s).map(t => t.s)
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from mustEqual List(Source("TestEntity", "t"))
        sqlq.where mustEqual None
        sqlq.select.toString mustEqual "t.s"
        sqlq.orderBy.toString mustEqual "List(OrderByCriteria(t.s,false))"
      }
      "with filter" in {
        val q = quote {
          qr1.filter(t => t.s == "s").sortBy(t => t.s).map(t => (t.i))
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from mustEqual List(Source("TestEntity", "t"))
        sqlq.where.toString mustEqual """Some(t.s == "s")"""
        sqlq.select.toString mustEqual "t.i"
        sqlq.orderBy.toString mustEqual "List(OrderByCriteria(t.s,false))"
      }
    }
  }

  "fails if the query is not normalized" in {
    val q = quote {
      qr1.map(_.s).filter(_ == "s")
    }
    val e = intercept[IllegalStateException] {
      SqlQuery(q.ast)
    }
  }
}
