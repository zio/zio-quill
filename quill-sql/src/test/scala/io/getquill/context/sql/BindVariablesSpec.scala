package io.getquill.context.sql

import mirrorContext._
import io.getquill.context.mirror.Row

class BindVariablesSpec extends SqlSpec {

  "binds values according to the sql terms order" - {
    "drop.take" in {
      val q =
        quote { (offset: Int, size: Int) =>
          query[TestEntity].drop(offset).take(size)
        }
      val mirror = mirrorContext.run(q)(1, 2)
      mirror.sql mustEqual "SELECT x.s, x.i, x.l, x.o FROM TestEntity x LIMIT ? OFFSET ?"
      mirror.binds mustEqual Row(2, 1)
    }
  }
}
