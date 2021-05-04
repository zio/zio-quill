package io.getquill.context.sql

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.context.sql.testContext._

class BindVariablesSpec extends Spec {

  "binds values according to the sql terms order" - {
    "drop.take" in {
      val q =
        quote {
          query[TestEntity].drop(lift(1)).take(lift(2))
        }
      val mirror = testContext.run(q)
      mirror.string mustEqual "SELECT x.s, x.i, x.l, x.o, x.b FROM TestEntity x LIMIT ? OFFSET ?"
      mirror.prepareRow mustEqual Row(2, 1)
    }
    "drop.take with extra param" in {
      val q =
        quote {
          query[TestEntity].filter(_.i == lift(3)).drop(lift(1)).take(lift(2))
        }
      val mirror = testContext.run(q)
      mirror.string mustEqual "SELECT x1.s, x1.i, x1.l, x1.o, x1.b FROM TestEntity x1 WHERE x1.i = ? LIMIT ? OFFSET ?"
      mirror.prepareRow mustEqual Row(3, 2, 1)
    }
  }
}
