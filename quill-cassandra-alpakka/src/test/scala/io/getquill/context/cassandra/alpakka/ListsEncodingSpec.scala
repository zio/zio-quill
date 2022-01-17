package io.getquill.context.cassandra.alpakka

import io.getquill.context.cassandra.CollectionsSpec

import java.time.{ Instant, LocalDate }
import java.util.UUID

class ListsEncodingSpec extends CollectionsSpec with CassandraAlpakkaSpec {
  val ctx = testDB
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
    timestamps: List[Instant],
    uuids:      List[UUID]
  )

  val e = ListsEntity(1, List("c"), List(BigDecimal(1.33)), List(true), List(0, 1), List(3, 2), List(1, 2), List(2, 3),
    List(1f, 3f), List(5d), List(LocalDate.now()),
    List(Instant.now()), List(UUID.randomUUID()))

  val q = quote(query[ListsEntity])

  "List encoders/decoders for CassandraTypes and CassandraMappers" in {
    await {
      for {
        _ <- ctx.run(q.insert(lift(e)))
        res <- ctx.run(q.filter(_.id == 1))
      } yield {
        res.head mustBe e
      }
    }
  }

  "Empty lists and optional fields" in {
    case class Entity(id: Int, texts: Option[List[String]], bools: Option[List[Boolean]], ints: List[Int])
    val e = Entity(1, Some(List("1", "2")), None, Nil)
    val q = quote(querySchema[Entity]("ListsEntity"))

    await {
      for {
        _ <- ctx.run(q.insert(lift(e)))
        res <- ctx.run(q.filter(_.id == 1))
      } yield {
        res.head mustBe e
      }
    }
  }

  "Mapped encoding for CassandraType" in {
    case class StrEntity(id: Int, texts: List[StrWrap])
    val e = StrEntity(1, List("1", "2").map(StrWrap.apply))
    val q = quote(querySchema[StrEntity]("ListsEntity"))

    await {
      for {
        _ <- ctx.run(q.insert(lift(e)))
        res <- ctx.run(q.filter(_.id == 1))
      } yield {
        res.head mustBe e
      }
    }
  }

  "Mapped encoding for CassandraMapper types" in {
    case class IntEntity(id: Int, ints: List[IntWrap])
    val e = IntEntity(1, List(1, 2).map(IntWrap.apply))
    val q = quote(querySchema[IntEntity]("ListsEntity"))

    await {
      for {
        _ <- ctx.run(q.insert(lift(e)))
        res <- ctx.run(q.filter(_.id == 1))
      } yield {
        res.head mustBe e
      }
    }
  }

  "Blob (Array[Byte]) support" in {
    case class BlobsEntity(id: Int, blobs: List[Array[Byte]])
    val e = BlobsEntity(4, List(Array(1.toByte, 2.toByte), Array(2.toByte)))
    val q = quote(querySchema[BlobsEntity]("ListsEntity"))

    await {
      for {
        _ <- ctx.run(q.insert(lift(e)))
        res <- ctx.run(q.filter(_.id == 4))
      } yield {
        res.head.blobs.map(_.toList) mustBe e.blobs.map(_.toList)
      }
    }
  }

  "List in where clause / contains" in {
    val e = ListFrozen(List(1, 2))
    await {
      for {
        _ <- ctx.run(listFroz.insert(lift(e)))
        res1 <- ctx.run(listFroz.filter(_.id == lift(List(1, 2))))
        res2 <- ctx.run(listFroz.filter(_.id == lift(List(1))))
      } yield {
        res1 mustBe List(e)
        res2 mustBe Nil
      }
    }
    await {
      for {
        _ <- ctx.run(listFroz.insert(lift(e)))
        res1 <- ctx.run(listFroz.filter(_.id.contains(2)).allowFiltering)
        res2 <- ctx.run(listFroz.filter(_.id.contains(3)).allowFiltering)
      } yield {
        res1 mustBe List(e)
        res2 mustBe Nil
      }
    }
  }

  override protected def beforeEach(): Unit = {
    await {
      ctx.run(q.delete)
    }
    await {
      ctx.run(listFroz.delete)
    }
  }
}
