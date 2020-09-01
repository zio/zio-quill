package io.getquill.dsl

import io.getquill.testContext._
import io.getquill.Spec
import io.getquill.EntityQuery

class QueryDslSpec extends Spec {

  "expands inserts" - {
    "default meta" in {
      val q = quote {
        (t: TestEntity) => qr1.insert(t)
      }
      val u = quote {
        (t: TestEntity) =>
          qr1.insert(
            v => v.s -> t.s,
            v => v.i -> t.i,
            v => v.l -> t.l,
            v => v.o -> t.o,
            v => v.b -> t.b
          )
      }
      q.ast mustEqual u.ast
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
      q.ast mustEqual u.ast
    }
  }

  "expands updates" - {
    "default meta" in {
      val q = quote {
        (t: TestEntity) => qr1.update(t)
      }
      val u = quote {
        (t: TestEntity) =>
          qr1.update(
            v => v.s -> t.s,
            v => v.i -> t.i,
            v => v.l -> t.l,
            v => v.o -> t.o,
            v => v.b -> t.b
          )
      }
      q.ast mustEqual u.ast
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
      q.ast mustEqual u.ast
    }
  }
}