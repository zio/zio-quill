package io.getquill.source.cassandra

import io.getquill._
import io.getquill.source.cassandra.mirror.mirrorSource
import io.getquill.source.mirror.Row

class CassandraSourceMacroSpec extends Spec {

  "runs queries" - {
    "static" in {
      val q = quote {
        (a: Int) => qr1.filter(t => t.i == a)
      }
      val mirror = mirrorSource.run(q)(1)
      mirror.cql mustEqual "SELECT s, i, l, o FROM TestEntity WHERE i = ?"
      mirror.binds mustEqual Row(1)
    }
    "dynamic" in {
      val q: Quoted[Int => Query[TestEntity]] = quote {
        (a: Int) => qr1.filter(t => t.i == a)
      }
      val mirror = mirrorSource.run(q)(1)
      mirror.cql mustEqual "SELECT s, i, l, o FROM TestEntity WHERE i = ?"
      mirror.binds mustEqual Row(1)
    }
  }

  "probes queries" in {
    val q = quote {
      qr1.filter(_.s == "fail")
    }
    "mirrorSource.run(q)" mustNot compile
  }
}
