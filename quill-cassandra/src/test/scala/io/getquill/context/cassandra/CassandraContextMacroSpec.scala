package io.getquill.context.cassandra

import io.getquill._
import io.getquill.context.mirror.Row

class CassandraContextMacroSpec extends Spec {

  val context = mirrorContext
  import mirrorContext._

  "runs queries" - {
    "static" in {
      val q = quote {
        qr1.filter(t => t.i == lift(1))
      }
      val mirror = mirrorContext.run(q)
      mirror.string mustEqual "SELECT s, i, l, o, b FROM TestEntity WHERE i = ?"
      mirror.prepareRow mustEqual Row(1)
    }
    "dynamic" in {
      val q: Quoted[Query[TestEntity]] = quote {
        qr1.filter(t => t.i == lift(1))
      }
      val mirror = mirrorContext.run(q)
      mirror.string mustEqual "SELECT s, i, l, o, b FROM TestEntity WHERE i = ?"
      mirror.prepareRow mustEqual Row(1)
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
        qr1.filter(t => t.i == lift(1)).update(t => t.l -> lift(2L))
      }
      val mirror = mirrorContext.run(q)
      mirror.string mustEqual "UPDATE TestEntity SET l = ? WHERE i = ?"
      mirror.prepareRow mustEqual Row(2L, 1)
    }
    "filter.map" in {
      val q = quote {
        qr1.filter(t => t.i == lift(1)).map(t => lift(2L))
      }
      val mirror = mirrorContext.run(q)
      mirror.string mustEqual "SELECT ? FROM TestEntity WHERE i = ?"
      mirror.prepareRow mustEqual Row(2L, 1)
    }
  }
}
