package io.getquill.context.orientdb

import io.getquill.Spec
import io.getquill.context.mirror.Row

class BindVariablesSpec extends Spec {

  "binds lifted values" in {
    val mirrorContext = orientdb.mirrorContext
    import mirrorContext._
    def q(i: Int) =
      quote {
        query[TestEntity].filter(e => e.i == lift(i))
      }
    val mirror = mirrorContext.run(q(2))
    mirror.string mustEqual "SELECT s, i, l, o FROM TestEntity WHERE i = ?"
    mirror.prepareRow mustEqual Row(2)
  }
}