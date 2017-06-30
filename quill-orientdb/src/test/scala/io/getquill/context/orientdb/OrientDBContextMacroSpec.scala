package io.getquill.context.orientdb

import io.getquill.Spec
import io.getquill.context.mirror.Row

class OrientDBContextMacroSpec extends Spec {

  "runs queries" - {
    "static" in {
      val mirrorContext = orientdb.mirrorContext
      import mirrorContext._
      val q = quote {
        qr1.filter(t => t.i == lift(1))
      }
      val mirror = mirrorContext.run(q)
      mirror.string mustEqual "SELECT s, i, l, o FROM TestEntity WHERE i = ?"
      mirror.prepareRow mustEqual Row(1)
    }
    "dynamic" in {
      val mirrorContext = orientdb.mirrorContext
      import mirrorContext._
      val q: Quoted[Query[TestEntity]] = quote {
        qr1.filter(t => t.i == lift(1))
      }
      val mirror = mirrorContext.run(q)
      mirror.string mustEqual "SELECT s, i, l, o FROM TestEntity WHERE i = ?"
      mirror.prepareRow mustEqual Row(1)
    }
  }

  "binds inputs according to the orientQl terms order" - {
    "filter.update" in {
      val mirrorContext = orientdb.mirrorContext
      import mirrorContext._
      val q = quote {
        qr1.filter(t => t.i == lift(1)).update(t => t.l -> lift(2L))
      }
      val mirror = mirrorContext.run(q)
      mirror.string mustEqual "UPDATE TestEntity SET l = ? WHERE i = ?"
      mirror.prepareRow mustEqual Row(2L, 1)
    }
    "filter.map" in {
      val mirrorContext = orientdb.mirrorContext
      import mirrorContext._
      val q = quote {
        qr1.filter(t => t.i == lift(1)).map(t => lift(2L))
      }
      val mirror = mirrorContext.run(q)
      mirror.string mustEqual "SELECT ? FROM TestEntity WHERE i = ?"
      mirror.prepareRow mustEqual Row(2L, 1)
    }
  }
}