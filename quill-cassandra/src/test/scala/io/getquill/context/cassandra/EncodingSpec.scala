package io.getquill.context.cassandra

import java.time.{Instant, LocalDate, ZoneId, ZonedDateTime}
import io.getquill.Query

class EncodingSpec extends EncodingSpecHelper {

  "encodes and decodes types" - {

    "sync" in {
      import testSyncDB._
      testSyncDB.run(query[EncodingTestEntity].delete)
      testSyncDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
      verify(testSyncDB.run(query[EncodingTestEntity]))
    }

    "async" in {
      import testAsyncDB._
      import scala.concurrent.ExecutionContext.Implicits.global
      await {
        for {
          _      <- testAsyncDB.run(query[EncodingTestEntity].delete)
          _      <- testAsyncDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
          result <- testAsyncDB.run(query[EncodingTestEntity])
        } yield {
          verify(result)
        }
      }
    }
  }

  "encodes collections" - {
    "sync" in {
      import testSyncDB._
      val q = quote { (list: Query[Int]) =>
        query[EncodingTestEntity].filter(t => list.contains(t.id))
      }
      testSyncDB.run(query[EncodingTestEntity])
      testSyncDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
      verify(testSyncDB.run(q(liftQuery(insertValues.map(_.id)))))
    }

    "async" in {
      import testAsyncDB._
      import scala.concurrent.ExecutionContext.Implicits.global
      val q = quote { (list: Query[Int]) =>
        query[EncodingTestEntity].filter(t => list.contains(t.id))
      }
      await {
        for {
          _ <- testAsyncDB.run(query[EncodingTestEntity].delete)
          _ <- testAsyncDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
          r <- testAsyncDB.run(q(liftQuery(insertValues.map(_.id))))
        } yield {
          verify(r)
        }
      }
    }
  }

  "mappedEncoding" in {
    import testSyncDB._
    case class A()
    case class B()
    val a1: Encoder[A] = encoder((b, c, d, s) => d)
    val a2: Decoder[A] = decoder((b, c, s) => A())
    mappedDecoder(MappedEncoding[A, B](_ => B()), a2).isInstanceOf[CassandraDecoder[B]] mustBe true
    mappedEncoder(MappedEncoding[B, A](_ => A()), a1).isInstanceOf[CassandraEncoder[B]] mustBe true
  }

  "date and timestamps" - {
    case class Java8Types(v9: LocalDate, v11: Instant, o9: Option[ZonedDateTime], id: Int = 1, v1: String = "")
    case class CasTypes(v9: LocalDate, v11: Instant, o9: Option[ZonedDateTime], id: Int = 1, v1: String = "")

    "mirror" in {
      import mirrorContext._
      implicitly[Encoder[LocalDate]]
      implicitly[Decoder[LocalDate]]
      implicitly[Encoder[Instant]]
      implicitly[Decoder[Instant]]
      implicitly[Encoder[ZonedDateTime]]
      implicitly[Decoder[ZonedDateTime]]
    }

    "session" in {
      val ctx = testSyncDB
      import ctx._

      val epoch         = System.currentTimeMillis()
      val epochDay      = epoch / 86400000L
      val instant       = Instant.ofEpochMilli(epoch)
      val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault)

      val jq = quote(querySchema[Java8Types]("EncodingTestEntity"))
      val j  = Java8Types(LocalDate.ofEpochDay(epochDay), instant, Some(zonedDateTime))
      val cq = quote(querySchema[CasTypes]("EncodingTestEntity"))
      val c  = CasTypes(LocalDate.ofEpochDay(epochDay), Instant.ofEpochMilli(epoch), Some(zonedDateTime))

      ctx.run(jq.delete)
      ctx.run(jq.insertValue(lift(j)))
      ctx.run(cq).headOption mustBe Some(c)

      ctx.run(cq.delete)
      ctx.run(cq.insertValue(lift(c)))
      ctx.run(jq).headOption mustBe Some(j)
    }
  }
}
