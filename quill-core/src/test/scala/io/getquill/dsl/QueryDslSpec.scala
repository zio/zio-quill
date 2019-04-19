package io.getquill.dsl

import io.getquill.ast._
import io.getquill.norm.BetaReduction
import io.getquill.testContext._
import io.getquill.Spec

class QueryDslSpec extends Spec {

  "expands inserts" - {
    "default meta" in {
      val q = quote {
        (t: TestEntity) => qr1.insert(t)
      }
      val u = quote {
        (t: TestEntity) => qr1.insert(v => v.s -> t.s, v => v.i -> t.i, v => v.l -> t.l, v => v.o -> t.o)
      }
      val reduction = q.ast match {
        case Function(params, Insert(entity, assigments)) =>
          val ass = assigments.map(normAssign)
          Function(params, Insert(entity, ass))
        case ast => ast
      }
      reduction mustEqual u.ast
    }
    "custom meta" in {
      implicit val insertMeta = new InsertMeta[TestEntity] {
        override val expand = quote((q: EntityQuery[TestEntity], value: TestEntity) => q.insert(v => v.i -> value.i))
      }
      val q = quote {
        (t: TestEntity) => qr1.insert(t)
      }
      val u = quote {
        (t: TestEntity) => qr1.insert(v => v.i -> t.i)
      }
      val reduction = q.ast match {
        case Function(params, Insert(entity, assigments)) =>
          val ass = assigments.map(normAssign)
          Function(params, Insert(entity, ass))
        case ast => ast
      }
      reduction mustEqual u.ast
    }
  }

  "expands updates" - {
    "default meta" in {
      val q = quote {
        (t: TestEntity) => qr1.update(t)
      }
      val u = quote {
        (t: TestEntity) => qr1.update(v => v.s -> t.s, v => v.i -> t.i, v => v.l -> t.l, v => v.o -> t.o)
      }
      val reduction = q.ast match {
        case Function(params, Update(entity, assigments)) =>
          val ass = assigments.map(normAssign)
          Function(params, Update(entity, ass))
        case ast => ast
      }
      reduction mustEqual u.ast
    }
    "custom meta" in {
      implicit val updateMeta = new UpdateMeta[TestEntity] {
        override val expand = quote((q: EntityQuery[TestEntity], value: TestEntity) => q.update(v => v.i -> value.i))
      }
      val q = quote {
        (t: TestEntity) => qr1.update(t)
      }
      val u = quote {
        (t: TestEntity) => qr1.update(v => v.i -> t.i)
      }
      val reduction = q.ast match {
        case Function(params, Update(entity, assigments)) =>
          val ass = assigments.map(normAssign)
          Function(params, Update(entity, ass))
        case ast => ast
      }
      reduction mustEqual u.ast
    }
  }
  private def normAssign(as: Assignment) = {
    val Assignment(a, p, v) = as
    Assignment(Ident("v"), BetaReduction(p, (a -> Ident("v"))), v)
  }
}
