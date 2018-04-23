package io.getquill.context.cassandra

import java.util.{ Date, UUID }

import com.datastax.driver.core.LocalDate

class ListsEncodingSpec extends CollectionsSpec {
  val ctx = testSyncDB
  import ctx._

  case class ListsEntity(
    id:         Int,
    texts:      List[String],
    decimals:   List[BigDecimal],
    bools:      List[Boolean],
    bytes:      List[Byte],
    shorts:     List[Short],
    ints:       List[Int],
    longs:      List[Long],
    floats:     List[Float],
    doubles:    List[Double],
    dates:      List[LocalDate],
    timestamps: List[Date],
    uuids:      List[UUID]
  )
  val e = ListsEntity(1, List("c"), List(BigDecimal(1.33)), List(true), List(0, 1), List(3, 2), List(1, 2), List(2, 3),
    List(1f, 3f), List(5d), List(LocalDate.fromMillisSinceEpoch(System.currentTimeMillis())),
    List(new Date), List(UUID.randomUUID()))
  val q = quote(query[ListsEntity])

  "List encoders/decoders for CassandraTypes and CassandraMappers" in {
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Empty lists and optional fields" in {
    case class Entity(id: Int, texts: Option[List[String]], bools: Option[List[Boolean]], ints: List[Int])
    val e = Entity(1, Some(List("1", "2")), None, Nil)
    val q = quote(querySchema[Entity]("ListsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Mapped encoding for CassandraType" in {
    case class StrEntity(id: Int, texts: List[StrWrap])
    val e = StrEntity(1, List("1", "2").map(StrWrap.apply))
    val q = quote(querySchema[StrEntity]("ListsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Mapped encoding for CassandraMapper types" in {
    case class IntEntity(id: Int, ints: List[IntWrap])
    val e = IntEntity(1, List(1, 2).map(IntWrap.apply))
    val q = quote(querySchema[IntEntity]("ListsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Blob (Array[Byte]) support" in {
    case class BlobsEntity(id: Int, blobs: List[Array[Byte]])
    val e = BlobsEntity(1, List(Array(1.toByte, 2.toByte), Array(2.toByte)))
    val q = quote(querySchema[BlobsEntity]("ListsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1))
      .head.blobs.map(_.toList) mustBe e.blobs.map(_.toList)
  }

  "List in where clause / contains" in {
    val e = ListFrozen(List(1, 2))
    ctx.run(listFroz.insert(lift(e)))
    ctx.run(listFroz.filter(_.id == lift(List(1, 2)))) mustBe List(e)
    ctx.run(listFroz.filter(_.id == lift(List(1)))) mustBe Nil

    ctx.run(listFroz.filter(_.id.contains(2)).allowFiltering) mustBe List(e)
    ctx.run(listFroz.filter(_.id.contains(3)).allowFiltering) mustBe Nil
  }

  override protected def beforeEach(): Unit = {
    ctx.run(q.delete)
    ctx.run(listFroz.delete)
  }
}
