
package io.getquill.source.sql

import io.getquill._
import io.getquill.ast._
import io.getquill.norm.QueryGenerator
import io.getquill.norm.Normalize

class SqlQuerySpec extends Spec {

  "transforms the ast into a flatten sql-like structure" - {

    "generated query" - {
      val gen = new QueryGenerator(1)
      for (i <- (3 to 15)) {
        for (j <- (0 until 30)) {
          val query = Normalize(gen(i))
          s"$i levels ($j) - $query" in {
            VerifySqlQuery(SqlQuery(query)) match {
              case None        =>
              case Some(error) => println(error)
            }
          }
        }
      }
    }

    "join query" in {
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
      sqlq.limit mustEqual None
    }
    "nested infix query" - {
      "as source" in {
        val q = quote {
          infix"SELECT * FROM TestEntity".as[Queryable[TestEntity]].filter(t => t.i == 1)
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from mustEqual List(InfixSource(Infix(List("SELECT * FROM TestEntity"), List()), "t"))
        sqlq.where.toString mustEqual "Some(t.i == 1)"
        sqlq.select.toString mustEqual "*"
        sqlq.orderBy mustEqual List()
        sqlq.limit mustEqual None
      }
      "fails if used as the flatMap body" in {
        val q = quote {
          qr1.flatMap(a => infix"SELECT * FROM TestEntity2 t where t.s = ${a.s}".as[Queryable[TestEntity2]])
        }
        val e = intercept[IllegalStateException] {
          SqlQuery(q.ast)
        }
      }
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
        sqlq.limit mustEqual None
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
        sqlq.limit mustEqual None
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
        sqlq.limit mustEqual None
      }
      "with outer filter" in {
        val q = quote {
          qr1.sortBy(t => t.s).filter(t => t.s == "s").map(t => t.s)
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from.toString mustEqual "List(QuerySource(SqlQuery(List(TableSource(TestEntity,t)),None,List(OrderByCriteria(t.s,false)),None,*),t))"
        sqlq.where.toString mustEqual "Some(t.s == \"s\")"
        sqlq.select.toString mustEqual "t.s"
        sqlq.orderBy mustEqual List()
        sqlq.limit mustEqual None
      }
      "with flatMap" in {
        val q = quote {
          qr1.sortBy(t => t.s).flatMap(t => qr2.map(t => t.s))
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from.toString mustEqual "List(QuerySource(SqlQuery(List(TableSource(TestEntity,t)),None,List(OrderByCriteria(t.s,false)),None,*),t), TableSource(TestEntity2,t))"
        sqlq.where mustEqual None
        sqlq.select.toString mustEqual "t.s"
        sqlq.orderBy mustEqual List()
        sqlq.limit mustEqual None
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
        sqlq.limit mustEqual None
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
        sqlq.limit mustEqual None
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
    "limited query" - {
      "simple" in {
        val q = quote {
          qr1.take(10)
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from mustEqual List(TableSource("TestEntity", "x"))
        sqlq.where mustEqual None
        sqlq.select.toString mustEqual "*"
        sqlq.orderBy mustEqual List()
        sqlq.limit.toString mustEqual "Some(10)"
      }
      "nested" in {
        val q = quote {
          qr1.take(10).flatMap(a => qr2)
        }
        val sqlq = SqlQuery(q.ast)
        sqlq.from.toString mustEqual "List(QuerySource(SqlQuery(List(TableSource(TestEntity,x)),None,List(),Some(10),*),a), TableSource(TestEntity2,x))"
        sqlq.where mustEqual None
        sqlq.select.toString mustEqual "*"
        sqlq.orderBy mustEqual List()
        sqlq.limit mustEqual None
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
