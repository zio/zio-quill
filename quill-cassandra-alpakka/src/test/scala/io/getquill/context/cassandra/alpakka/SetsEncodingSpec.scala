package io.getquill.context.cassandra.alpakka

import io.getquill.context.cassandra.CollectionsSpec

import java.time.{ Instant, LocalDate }
import java.util.UUID

class SetsEncodingSpec extends CollectionsSpec with CassandraAlpakkaSpec {
  val ctx = testDB

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
    timestamps: Set[Instant],
    uuids:      Set[UUID]
  )
  val e = SetsEntity(1, Set("c"), Set(BigDecimal(1.33)), Set(true), Set(1, 2), Set(2, 3), Set(1f, 3f),
    Set(5d), Set(LocalDate.now()),
    Set(Instant.now()), Set(UUID.randomUUID()))
  val q = quote(query[SetsEntity])

  "Set encoders/decoders for CassandraTypes and CassandraMappers" in {
    await {
      for {
        _ <- ctx.run(q.insert(lift(e)))
        res <- ctx.run(q.filter(_.id == 1))
      } yield {
        res.head mustBe e
      }
    }
  }

  "Empty sets and optional fields" in {
    case class Entity(id: Int, texts: Option[Set[String]], bools: Option[Set[Boolean]], ints: Set[Int])
    val e = Entity(1, Some(Set("1", "2")), None, Set.empty)
    val q = quote(querySchema[Entity]("SetsEntity"))

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
    case class StrEntity(id: Int, texts: Set[StrWrap])
    val e = StrEntity(1, Set("1", "2").map(StrWrap.apply))
    val q = quote(querySchema[StrEntity]("SetsEntity"))

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
    case class IntEntity(id: Int, ints: Set[IntWrap])
    val e = IntEntity(1, Set(1, 2).map(IntWrap.apply))
    val q = quote(querySchema[IntEntity]("SetsEntity"))

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
    case class BlobsEntity(id: Int, blobs: Set[Array[Byte]])
    val e = BlobsEntity(4, Set(Array(1.toByte, 2.toByte), Array(2.toByte)))
    val q = quote(querySchema[BlobsEntity]("SetsEntity"))

    await {
      for {
        _ <- ctx.run(q.insert(lift(e)))
        res <- ctx.run(q.filter(_.id == 4))
      } yield {
        res.head.blobs.map(_.toSet) mustBe e.blobs.map(_.toSet)
      }
    }
  }

  "Set in where clause" in {
    val e = SetFrozen(Set(1, 2))
    await {
      for {
        _ <- ctx.run(setFroz.insert(lift(e)))
        res1 <- ctx.run(setFroz.filter(_.id == lift(Set(1, 2))))
        res2 <- ctx.run(setFroz.filter(_.id == lift(Set(1))))
      } yield {
        res1 mustBe List(e)
        res2 mustBe List.empty
      }
    }
  }

  override protected def beforeEach(): Unit = {
    await {
      ctx.run(q.delete)
    }
    await {
      ctx.run(setFroz.delete)
    }
  }
}
