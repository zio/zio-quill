package io.getquill.context.cassandra

import java.util.{ Date, UUID }

import com.datastax.driver.core.LocalDate

class SetsEncodingSpec extends CollectionsSpec {
  val ctx = testSyncDB
  import ctx._

  case class SetsEntity(
    id:         Int,
    texts:      Set[String],
    decimals:   Set[BigDecimal],
    bools:      Set[Boolean],
    ints:       Set[Int],
    longs:      Set[Long],
    floats:     Set[Float],
    doubles:    Set[Double],
    dates:      Set[LocalDate],
    timestamps: Set[Date],
    uuids:      Set[UUID]
  )
  val e = SetsEntity(1, Set("c"), Set(BigDecimal(1.33)), Set(true), Set(1, 2), Set(2, 3), Set(1f, 3f),
    Set(5d), Set(LocalDate.fromMillisSinceEpoch(System.currentTimeMillis())),
    Set(new Date), Set(UUID.randomUUID()))
  val q = quote(query[SetsEntity])

  "Set encoders/decoders" in {
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Empty sets and optional fields" in {
    case class Entity(id: Int, texts: Option[Set[String]], bools: Option[Set[Boolean]], ints: Set[Int])
    val e = Entity(1, Some(Set("1", "2")), None, Set())
    val q = quote(querySchema[Entity]("SetsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Mapped encoding for CassandraType" in {
    case class StrEntity(id: Int, texts: Set[StrWrap])
    val e = StrEntity(1, Set("1", "2").map(StrWrap.apply))
    val q = quote(querySchema[StrEntity]("SetsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Mapped encoding for CassandraMapper types" in {
    case class IntEntity(id: Int, ints: Set[IntWrap])
    val e = IntEntity(1, Set(1, 2).map(IntWrap.apply))
    val q = quote(querySchema[IntEntity]("SetsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Blob (Array[Byte]) support" in {
    case class BlobsEntity(id: Int, blobs: Set[Array[Byte]])
    val e = BlobsEntity(1, Set(Array(1.toByte, 2.toByte), Array(2.toByte)))
    val q = quote(querySchema[BlobsEntity]("SetsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1))
      .head.blobs.map(_.toSet) mustBe e.blobs.map(_.toSet)
  }

  "Set in where clause" in {
    val e = SetFrozen(Set(1, 2))
    ctx.run(setFroz.insert(lift(e)))
    ctx.run(setFroz.filter(_.id == lift(Set(1, 2)))) mustBe List(e)
    ctx.run(setFroz.filter(_.id == lift(Set(1)))) mustBe List()
  }

  override protected def beforeEach(): Unit = {
    ctx.run(q.delete)
    ctx.run(setFroz.delete)
  }
}
