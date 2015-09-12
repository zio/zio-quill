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
      sqlq.from mustEqual List(TableSource("TestEntity", "a"), TableSource("TestEntity2", "b"))
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
        sqlq.from mustEqual List(TableSource("TestEntity", "t"))
        sqlq.where mustEqual None
        sqlq.select.toString mustEqual "t.s"
        sqlq.orderBy.toString mustEqual "List(OrderByCriteria(t.s,false))"
      }
      "with filter" in {
        val q = quote {
          qr1.filter(t => t.s == "s").sortBy(t => t.s).map(t => (t.i))
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from mustEqual List(TableSource("TestEntity", "t"))
        sqlq.where.toString mustEqual """Some(t.s == "s")"""
        sqlq.select.toString mustEqual "t.i"
        sqlq.orderBy.toString mustEqual "List(OrderByCriteria(t.s,false))"
      }
      "with reverse" in {
        val q = quote {
          qr1.sortBy(t => t.s).reverse.map(t => t.s)
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from mustEqual List(TableSource("TestEntity", "t"))
        sqlq.where mustEqual None
        sqlq.select.toString mustEqual "t.s"
        sqlq.orderBy.toString mustEqual "List(OrderByCriteria(t.s,true))"
      }
      "with outer filter" in {
        val q = quote {
          qr1.sortBy(t => t.s).filter(t => t.s == "s").map(t => t.s)
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from.toString mustEqual "List(QuerySource(SqlQuery(List(TableSource(TestEntity,t)),None,List(OrderByCriteria(t.s,false)),*),t))"
        sqlq.where.toString mustEqual "Some(t.s == \"s\")"
        sqlq.select.toString mustEqual "t.s"
        sqlq.orderBy mustEqual List()
      }
      "with flatMap" in {
        val q = quote {
          qr1.sortBy(t => t.s).flatMap(t => qr2.map(t => t.s))
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from.toString mustEqual "List(QuerySource(SqlQuery(List(TableSource(TestEntity,t)),None,List(OrderByCriteria(t.s,false)),*),t), TableSource(TestEntity2,t))"
        sqlq.where mustEqual None
        sqlq.select.toString mustEqual "t.s"
        sqlq.orderBy mustEqual List()
      }
      "tuple criteria" in {
        val q = quote {
          qr1.sortBy(t => (t.s, t.i)).map(t => t.s)
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from mustEqual List(TableSource("TestEntity", "t"))
        sqlq.where mustEqual None
        sqlq.select.toString mustEqual "t.s"
        sqlq.orderBy.toString mustEqual "List(OrderByCriteria(t.s,false), OrderByCriteria(t.i,false))"
      }
      "multiple sortBy" in {
        val q = quote {
          qr1.sortBy(t => (t.s, t.i)).reverse.sortBy(t => t.l).map(t => t.s)
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from mustEqual List(TableSource("TestEntity", "t"))
        sqlq.where mustEqual None
        sqlq.select.toString mustEqual "t.s"
        sqlq.orderBy.toString mustEqual "List(OrderByCriteria(t.s,true), OrderByCriteria(t.i,true), OrderByCriteria(t.l,false))"
      }
      "fails if the sortBy criteria is malformed" in {
        val q = quote {
          qr1.sortBy(t => t)(null)
        }
        val e = intercept[IllegalStateException] {
          SqlQuery(q.ast)
        }
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
