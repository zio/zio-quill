package io.getquill.context.sql

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.context.sql.testContext.TestEntity
import io.getquill.context.sql.testContext.query
import io.getquill.context.sql.testContext.quote

class BindVariablesSpec extends Spec {

  "binds values according to the sql terms order" - {
    "drop.take" in {
      val q =
        quote { (offset: Int, size: Int) =>
          query[TestEntity].drop(offset).take(size)
        }
      val mirror = testContext.run(q)(1, 2)
      mirror.sql mustEqual "SELECT x.s, x.i, x.l, x.o FROM TestEntity x LIMIT ? OFFSET ?"
      mirror.binds mustEqual Row(2, 1)
    }
    "drop.take with extra param" in {
      val q =
        quote { (offset: Int, size: Int, i: Int) =>
          query[TestEntity].filter(_.i == i).drop(offset).take(size)
        }
      val mirror = testContext.run(q)(1, 2, 3)
      mirror.sql mustEqual "SELECT x1.s, x1.i, x1.l, x1.o FROM TestEntity x1 WHERE x1.i = ? LIMIT ? OFFSET ?"
      mirror.binds mustEqual Row(3, 2, 1)
    }
  }
}
