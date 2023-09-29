package io.getquill

import io.getquill.MirrorContexts.testContext._
import io.getquill.ast.{Query => _, _}
import io.getquill.base.Spec
import io.getquill.quat._

class OpsSpec extends Spec {

  "quotes asts" - {
    "explicitly" in {
      val q = quote {
        query[TestEntity]
      }
      q.ast mustEqual Entity("TestEntity", Nil, TestEntityQuat)
    }
    "implicitly" in {
      val q: Quoted[Query[TestEntity]] =
        query[TestEntity]
      q.ast mustEqual Entity("TestEntity", Nil, TestEntityQuat)
    }
  }

  "unquotes asts" - {
    "explicitly" in {
      val q = quote {
        unquote(qr1).map(t => t)
      }
      val quat = TestEntityQuat
      q.ast mustEqual Map(Entity("TestEntity", Nil, quat), Ident("t", quat), Ident("t", quat))
    }
    "implicitly" in {
      val q = quote {
        qr1.map(t => t)
      }
      val quat = TestEntityQuat
      q.ast mustEqual Map(Entity("TestEntity", Nil, quat), Ident("t", quat), Ident("t", quat))
    }
  }

  "provides the infix interpolator" - {
    "boolean values" - {
      "with `as`" in {
        val q = quote {
          sql"true".as[Boolean]
        }
        q.ast mustEqual Infix(List("true"), Nil, false, false, Quat.BooleanValue)
      }
    }
    "other values" - {
      "with `as`" in {
        val q = quote {
          sql"1".as[Int]
        }
        q.ast mustEqual Infix(List("1"), Nil, false, false, Quat.Value)
      }
      "without `as`" in {
        val q = quote {
          sql"1"
        }
        q.ast mustEqual Infix(List("1"), Nil, false, false, Quat.Value)
      }
    }
  }

  "unquotes double quotations" in {
    val q: Quoted[EntityQuery[TestEntity]] = quote {
      quote(query[TestEntity])
    }
    val n = quote {
      query[TestEntity]
    }
    q.ast mustEqual n.ast
  }

  implicit class QueryOps[Q <: Query[_]](q: Q) {
    def allowFiltering = quote(sql"$q ALLOW FILTERING".as[Q])
  }

  "unquotes quoted function bodies automatically" - {
    "one param" in {
      val q: Quoted[Int => EntityQuery[TestEntity]] = quote { (i: Int) =>
        query[TestEntity].allowFiltering
      }
      val n = quote { (i: Int) =>
        unquote(query[TestEntity].allowFiltering)
      }
      q.ast mustEqual n.ast
    }
    "multiple params" in {
      val q: Quoted[(Int, Int, Int) => EntityQuery[TestEntity]] = quote { (i: Int, j: Int, k: Int) =>
        query[TestEntity].allowFiltering
      }
      val n = quote { (i: Int, j: Int, k: Int) =>
        unquote(query[TestEntity].allowFiltering)
      }
      q.ast mustEqual n.ast
    }
  }
}
