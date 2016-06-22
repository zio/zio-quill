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
  }
}
