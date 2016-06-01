package io.getquill.context.cassandra

import io.getquill._
import io.getquill.context.mirror.Row

class CassandraContextMacroSpec extends Spec {

  val context = mirrorContext
  import mirrorContext._

  "runs queries" - {
    "static" in {
      val q = quote {
        (a: Int) => qr1.filter(t => t.i == a)
      }
      val mirror = mirrorContext.run(q)(1)
      mirror.cql mustEqual "SELECT s, i, l, o FROM TestEntity WHERE i = ?"
      mirror.binds mustEqual Row(1)
    }
    "dynamic" in {
      val q: Quoted[Int => Query[TestEntity]] = quote {
        (a: Int) => qr1.filter(t => t.i == a)
      }
      val mirror = mirrorContext.run(q)(1)
      mirror.cql mustEqual "SELECT s, i, l, o FROM TestEntity WHERE i = ?"
      mirror.binds mustEqual Row(1)
    }
  }

  "probes queries" in {
    val ctx = new CassandraMirrorContextWithQueryProbing
    import ctx._
    val q = quote {
      query[TestEntity].filter(_.s == "fail")
    }
    "ctx.run(q)" mustNot compile
  }

  "binds inputs according to the cql terms order" - {
    "filter.update" in {
      val q = quote {
        (i: Int, l: Long) =>
          qr1.filter(t => t.i == i).update(t => t.l -> l)
      }
      val mirror = mirrorContext.run(q)(List((1, 2L)))
      mirror.cql mustEqual "UPDATE TestEntity SET l = ? WHERE i = ?"
      mirror.bindList mustEqual List(Row(2l, 1))
    }
    "filter.map" in {
      val q = quote {
        (i: Int, l: Long) =>
          qr1.filter(t => t.i == i).map(t => l)
      }
      val mirror = mirrorContext.run(q)(1, 2L)
      mirror.cql mustEqual "SELECT ? FROM TestEntity WHERE i = ?"
      mirror.binds mustEqual Row(2l, 1)
    }
  }
}
