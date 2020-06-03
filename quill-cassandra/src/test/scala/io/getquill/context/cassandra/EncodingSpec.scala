package io.getquill.context.cassandra

import java.time.{ Instant, ZoneId, ZonedDateTime, LocalDate => Java8LocalDate }
import java.util.Date
import io.getquill.Query

import com.datastax.driver.core.LocalDate

class EncodingSpec extends EncodingSpecHelper {

  "encodes and decodes types" - {

    "sync" in {
      import testSyncDB._
      testSyncDB.run(query[EncodingTestEntity].delete)
      testSyncDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
      verify(testSyncDB.run(query[EncodingTestEntity]))
    }

    "async" in {
      import testAsyncDB._
      import scala.concurrent.ExecutionContext.Implicits.global
      await {
        for {
          _ <- testAsyncDB.run(query[EncodingTestEntity].delete)
          _ <- testAsyncDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
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
      val q = quote {
        (list: Query[Int]) =>
          query[EncodingTestEntity].filter(t => list.contains(t.id))
      }
      testSyncDB.run(query[EncodingTestEntity])
      testSyncDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
      verify(testSyncDB.run(q(liftQuery(insertValues.map(_.id)))))
    }

    "async" in {
      import testAsyncDB._
      import scala.concurrent.ExecutionContext.Implicits.global
      val q = quote {
        (list: Query[Int]) =>
          query[EncodingTestEntity].filter(t => list.contains(t.id))
      }
      await {
        for {
          _ <- testAsyncDB.run(query[EncodingTestEntity].delete)
          _ <- testAsyncDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
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
    val a1: Encoder[A] = encoder((b, c, d) => d)
    val a2: Decoder[A] = decoder((b, c) => A())
    mappedDecoder(MappedEncoding[A, B](_ => B()), a2).isInstanceOf[CassandraDecoder[B]] mustBe true
    mappedEncoder(MappedEncoding[B, A](_ => A()), a1).isInstanceOf[CassandraEncoder[B]] mustBe true
  }

  "date and timestamps" - {
    case class Java8Types(v9: Java8LocalDate, v11: Instant, o9: Option[ZonedDateTime], id: Int = 1, v1: String = "")
    case class CasTypes(v9: LocalDate, v11: Date, o9: Option[Date], id: Int = 1, v1: String = "")

    "mirror" in {
      import mirrorContext._
      implicitly[Encoder[Java8LocalDate]]
      implicitly[Decoder[Java8LocalDate]]
      implicitly[Encoder[Instant]]
      implicitly[Decoder[Instant]]
      implicitly[Encoder[ZonedDateTime]]
      implicitly[Decoder[ZonedDateTime]]
    }

    "session" in {
      val ctx = testSyncDB
      import ctx._

      val epoh = System.currentTimeMillis()
      val epohDay = epoh / 86400000L
      val instant = Instant.ofEpochMilli(epoh)
      val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault)

      val jq = quote(querySchema[Java8Types]("EncodingTestEntity"))
      val j = Java8Types(Java8LocalDate.ofEpochDay(epohDay), instant, Some(zonedDateTime))
      val cq = quote(querySchema[CasTypes]("EncodingTestEntity"))
      val c = CasTypes(LocalDate.fromMillisSinceEpoch(epoh), new Date(epoh), Some(new Date(epoh)))

      ctx.run(jq.delete)
      ctx.run(jq.insert(lift(j)))
      ctx.run(cq).headOption mustBe Some(c)

      ctx.run(cq.delete)
      ctx.run(cq.insert(lift(c)))
      ctx.run(jq).headOption mustBe Some(j)
    }
  }
}

