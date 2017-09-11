package io.getquill.context.cassandra

import java.util.{ Date, UUID }

import com.datastax.driver.core.LocalDate

class MapsEncodingSpec extends CollectionsSpec {
  val ctx = testSyncDB
  import ctx._

  case class MapsEntity(
    id:            Int,
    textDecimal:   Map[String, BigDecimal],
    intDouble:     Map[Int, Double],
    longFloat:     Map[Long, Float],
    boolDate:      Map[Boolean, LocalDate],
    uuidTimestamp: Map[UUID, Date]
  )
  val e = MapsEntity(1, Map("1" -> BigDecimal(1)), Map(1 -> 1d, 2 -> 2d, 3 -> 3d), Map(1l -> 3f),
    Map(true -> LocalDate.fromMillisSinceEpoch(System.currentTimeMillis())),
    Map(UUID.randomUUID() -> new Date))
  val q = quote(query[MapsEntity])

  "Map encoders/decoders" in {
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Empty maps and optional fields" in {
    case class Entity(
      id:          Int,
      textDecimal: Option[Map[String, BigDecimal]],
      intDouble:   Option[Map[Int, Double]],
      longFloat:   Map[Long, Float]
    )
    val e = Entity(1, Some(Map("1" -> BigDecimal(1))), None, Map())
    val q = quote(querySchema[Entity]("MapsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Mapped encoding for CassandraType" in {
    case class StrEntity(id: Int, textDecimal: Map[StrWrap, BigDecimal])
    val e = StrEntity(1, Map(StrWrap("1") -> BigDecimal(1)))
    val q = quote(querySchema[StrEntity]("MapsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Mapped encoding for CassandraMapper types" in {
    case class IntEntity(id: Int, intDouble: Map[IntWrap, Double])
    val e = IntEntity(1, Map(IntWrap(1) -> 1d))
    val q = quote(querySchema[IntEntity]("MapsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Map in where clause / contains" in {
    val e = MapFrozen(Map(1 -> true))
    ctx.run(mapFroz.insert(lift(e)))
    ctx.run(mapFroz.filter(_.id == lift(Map(1 -> true)))) mustBe List(e)
    ctx.run(mapFroz.filter(_.id == lift(Map(1 -> false)))) mustBe Nil

    ctx.run(mapFroz.filter(_.id.contains(1)).allowFiltering) mustBe List(e)
    ctx.run(mapFroz.filter(_.id.contains(2)).allowFiltering) mustBe Nil
  }

  "Map.containsValue" in {
    val e = MapFrozen(Map(1 -> true))
    ctx.run(mapFroz.insert(lift(e)))

    ctx.run(mapFroz.filter(_.id.containsValue(true)).allowFiltering) mustBe List(e)
    ctx.run(mapFroz.filter(_.id.containsValue(false)).allowFiltering) mustBe Nil
  }

  override protected def beforeEach(): Unit = {
    ctx.run(q.delete)
    ctx.run(mapFroz.delete)
  }
}
