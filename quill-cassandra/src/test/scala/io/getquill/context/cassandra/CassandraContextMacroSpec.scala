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
      mirror.string mustEqual "SELECT s, i, l, o FROM TestEntity WHERE i = ?"
      mirror.prepareRow mustEqual Row(1)
    }
    "dynamic" in {
      val q: Quoted[Query[TestEntity]] = quote {
        qr1.filter(t => t.i == lift(1))
      }
      val mirror = mirrorContext.run(q)
      mirror.string mustEqual "SELECT s, i, l, o FROM TestEntity WHERE i = ?"
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
      mirror.prepareRow mustEqual Row(2l, 1)
    }
    "filter.map" in {
      val q = quote {
        qr1.filter(t => t.i == lift(1)).map(t => lift(2L))
      }
      val mirror = mirrorContext.run(q)
      mirror.string mustEqual "SELECT ? FROM TestEntity WHERE i = ?"
      mirror.prepareRow mustEqual Row(2l, 1)
    }
  }

  "multi-level embedded case class with optionals lifting" in {
    case class TestEntity(level1: Level1)
    case class Level1(level2: Level2, optionalLevel2: Option[Level2]) extends Embedded
    case class Level2(level3: Level3, optionalLevel3: Option[Level3]) extends Embedded
    case class Level3(value: Option[String], optionalLevel4: Option[Level4]) extends Embedded
    case class Level4(id: Int) extends Embedded

    val e = TestEntity(Level1(Level2(Level3(Some("test"), None), Some(Level3(None, Some(Level4(1))))), None))
    val q = quote {
      query[TestEntity].insert(lift(e))
    }
    val r = mirrorContext.run(q)
    r.string mustEqual "INSERT INTO TestEntity (value,id,value,id,value,id,value,id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    r.prepareRow mustEqual Row(Some("test"), None, Some(None), Some(Some(1)), None, None, None, None)
  }
}
