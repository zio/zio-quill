package io.getquill.context.cassandra

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.context.cassandra.mirrorContext._

class BindVariablesSpec extends Spec {

  "binds lifted values" in {
    def q(i: Int) =
      quote {
        query[TestEntity].filter(e => e.i == lift(i))
      }
    val mirror = mirrorContext.run(q(2))
    mirror.string mustEqual "SELECT s, i, l, o, b FROM TestEntity WHERE i = ?"
    mirror.prepareRow mustEqual Row(2)
  }
}
