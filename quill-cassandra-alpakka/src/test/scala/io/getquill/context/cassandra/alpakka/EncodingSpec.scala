package io.getquill.context.cassandra.alpakka

import io.getquill.Query
import io.getquill.context.cassandra.EncodingSpecHelper

import java.time.{Instant, LocalDate, ZoneId, ZonedDateTime}

class EncodingSpec extends EncodingSpecHelper with CassandraAlpakkaSpec {

  "encodes and decodes types" in {
    await {
      import testDB._
      for {
        _      <- testDB.run(query[EncodingTestEntity].delete)
        _      <- testDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
        result <- testDB.run(query[EncodingTestEntity])
      } yield {
        verify(result)
      }
    }
  }

  "encodes collections" in {
    import testDB._
    val q = quote { (list: Query[Int]) =>
      query[EncodingTestEntity].filter(t => list.contains(t.id))
    }
    await {
      for {
        _ <- testDB.run(query[EncodingTestEntity].delete)
        _ <- testDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
        r <- testDB.run(q(liftQuery(insertValues.map(_.id))))
      } yield {
        verify(r)
      }
    }

  }

  "mappedEncoding" in {
    import testDB._
    case class A()
    case class B()
    val a1: Encoder[A] = encoder((b, c, d, s) => d)
    val a2: Decoder[A] = decoder((b, c, s) => A())
    mappedDecoder(MappedEncoding[A, B](_ => B()), a2).isInstanceOf[CassandraDecoder[B]] mustBe true
    mappedEncoder(MappedEncoding[B, A](_ => A()), a1).isInstanceOf[CassandraEncoder[B]] mustBe true
  }

  "date and timestamps" - {
    import testDB._
    case class Java8Types(v9: LocalDate, v11: Instant, o9: Option[ZonedDateTime], id: Int = 1, v1: String = "")
    case class CasTypes(v9: LocalDate, v11: Instant, o9: Option[ZonedDateTime], id: Int = 1, v1: String = "")

    "mirror" in {
      implicitly[Encoder[LocalDate]]
      implicitly[Decoder[LocalDate]]
      implicitly[Encoder[Instant]]
      implicitly[Decoder[Instant]]
      implicitly[Encoder[ZonedDateTime]]
      implicitly[Decoder[ZonedDateTime]]
    }

    "session" in {
      val ctx = testDB
      import ctx._

      val epoch         = System.currentTimeMillis()
      val epochDay      = epoch / 86400000L
      val instant       = Instant.ofEpochMilli(epoch)
      val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault)

      val jq = quote(querySchema[Java8Types]("EncodingTestEntity"))
      val j  = Java8Types(LocalDate.ofEpochDay(epochDay), instant, Some(zonedDateTime))
      val cq = quote(querySchema[CasTypes]("EncodingTestEntity"))
      val c  = CasTypes(LocalDate.ofEpochDay(epochDay), Instant.ofEpochMilli(epoch), Some(zonedDateTime))

      await {
        for {
          _ <- ctx.run(jq.delete)
          _ <- ctx.run(jq.insertValue(lift(j)))
          r <- ctx.run(cq)
        } yield {
          r.headOption mustBe Some(c)
        }
      }

      await {
        for {
          _ <- ctx.run(cq.delete)
          _ <- ctx.run(cq.insertValue(lift(c)))
          r <- ctx.run(jq)
        } yield {
          r.headOption mustBe Some(j)
        }
      }
    }
  }
}
