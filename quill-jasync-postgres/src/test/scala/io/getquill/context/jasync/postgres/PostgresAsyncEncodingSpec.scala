package io.getquill.context.jasync.postgres

import java.time.{LocalDate, LocalDateTime}

import io.getquill.context.sql.EncodingSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.Date
import java.util.UUID
import io.getquill.Query

class PostgresAsyncEncodingSpec extends EncodingSpec {

  val context = testContext
  import testContext._

  "encodes and decodes types" in {
    val r =
      for {
        _      <- testContext.run(delete)
        _      <- testContext.run(liftQuery(insertValues).foreach(e => insert(e)))
        result <- testContext.run(query[EncodingTestEntity])
      } yield result

    verify(Await.result(r, Duration.Inf))
  }

  "encodes and decodes uuids" in {
    final case class EncodingUUIDTestEntity(v1: UUID)
    val testUUID = UUID.fromString("e5240c08-6ee7-474a-b5e4-91f79c48338f")

    // delete old values
    val q0 = quote(query[EncodingUUIDTestEntity].delete)
    Await.result(testContext.run(q0), Duration.Inf)

    // insert new uuid
    Await.result(
      testContext.run(query[EncodingUUIDTestEntity].insertValue(lift(EncodingUUIDTestEntity(testUUID)))),
      Duration.Inf
    )

    // verify you can get the uuid back from the db
    val q2   = quote(query[EncodingUUIDTestEntity].map(p => p.v1))
    val rez2 = Await.result(testContext.run(q2), Duration.Inf)

    rez2 mustEqual List(testUUID)
  }

  "fails if the column has the wrong type" - {
    "numeric" in {
      Await.result(testContext.run(liftQuery(insertValues).foreach(e => insert(e))), Duration.Inf)
      final case class EncodingTestEntity(v1: Int)
      intercept[IllegalStateException] {
        Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
      }
    }
    "non-numeric" in {
      Await.result(testContext.run(liftQuery(insertValues).foreach(e => insert(e))), Duration.Inf)
      final case class EncodingTestEntity(v1: Date)
      intercept[IllegalStateException] {
        Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
      }
    }
  }

  "encodes sets" in {
    val q = quote { (set: Query[Int]) =>
      query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    val fut =
      for {
        _ <- testContext.run(query[EncodingTestEntity].delete)
        _ <- testContext.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
        r <- testContext.run(q(liftQuery(insertValues.map(_.v6))))
      } yield {
        r
      }
    verify(Await.result(fut, Duration.Inf))
  }

  "returning UUID" in {
    val success = for {
      uuid    <- Await.result(testContext.run(insertBarCode(lift(barCodeEntry))), Duration.Inf)
      barCode <- Await.result(testContext.run(findBarCodeByUuid(uuid)), Duration.Inf).headOption
    } yield {
      verifyBarcode(barCode)
    }
    success must not be empty
  }

  "decodes LocalDate and LocalDateTime types" in {
    final case class DateEncodingTestEntity(v1: LocalDate, v2: LocalDateTime)
    val entity = DateEncodingTestEntity(LocalDate.now, LocalDateTime.now)
    val r = for {
      _      <- testContext.run(query[DateEncodingTestEntity].delete)
      _      <- testContext.run(query[DateEncodingTestEntity].insertValue(lift(entity)))
      result <- testContext.run(query[DateEncodingTestEntity])
    } yield result
    Await.result(r, Duration.Inf) mustBe Seq(entity)
  }

  "encodes custom type inside singleton object" in {
    object Singleton {
      def apply()(implicit c: TestContext) = {
        import c._
        for {
          _      <- c.run(query[EncodingTestEntity].delete)
          result <- c.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
        } yield result
      }
    }

    implicit val c = testContext
    Await.result(Singleton(), Duration.Inf)
  }
}
