package io.getquill.context.orientdb

import java.util.Date

import io.getquill.Spec

class MapsEncodingSpec extends Spec {

  case class MapsEntity(
    id:         Int,
    longDouble: Map[Long, Double],
    intDouble:  Map[Int, Double],
    boolDate:   Map[Boolean, Date]
  )
  val e = MapsEntity(1, Map(1L -> 1.1), Map(1 -> 1.1d), Map(true -> new Date()))

  private def verify(expected: MapsEntity, actual: MapsEntity): Boolean = {
    expected.id mustEqual actual.id
    expected.longDouble.head._2 mustEqual actual.longDouble.head._2
    expected.intDouble.head._2 mustEqual actual.intDouble.head._2
    actual.boolDate.head._2.isInstanceOf[Date]
    true
  }

  "mirror" in {
    val ctx = orientdb.mirrorContext
    import ctx._
    val q = quote(query[MapsEntity])
    ctx.run(q.insert(lift(e)))
    ctx.run(q)
  }

  "Map encoders/decoders" in {
    val ctx = orientdb.testSyncDB
    import ctx._
    val q = quote(query[MapsEntity])
    ctx.run(q.delete)
    ctx.run(q.insert(lift(e)))
    verify(e, ctx.run(q.filter(_.id == 1)).head)
  }

  "Empty maps and optional fields" in {
    val ctx = orientdb.testSyncDB
    import ctx._
    case class Entity(
      id:         Int,
      intDouble:  Option[Map[Int, Double]],
      longDouble: Option[Map[Long, Double]]
    )
    val e = Entity(1, None, None)
    val q = quote(querySchema[Entity]("MapEntity"))

    ctx.run(q.delete)
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Map in where clause" in {
    val ctx = orientdb.testSyncDB
    import ctx._
    case class MapFrozen(id: Map[Int, Boolean])
    val e = MapFrozen(Map(1 -> true))
    val q = quote(query[MapFrozen])
    ctx.run(q.delete)
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(p => liftQuery(Set(1))
      .contains(p.id))).head.id.head._2 mustBe e.id.head._2
  }
}