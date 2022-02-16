package io.getquill.context.zio.jasync.postgres

import java.time.{ LocalDate, LocalDateTime, ZonedDateTime }
import io.getquill.context.sql.EncodingSpec

import java.util.Date
import java.util.UUID
import io.getquill.Query
import zio.FiberFailure

class PostgresAsyncEncodingSpec extends EncodingSpec with ZioSpec {

  import context._

  "encodes and decodes types" in {
    val r =
      for {
        _ <- context.run(delete)
        _ <- context.run(liftQuery(insertValues).foreach(e => insert(e)))
        result <- context.run(query[EncodingTestEntity])
      } yield result

    verify(runSyncUnsafe(r))
  }

  "encodes and decodes uuids" in {
    case class EncodingUUIDTestEntity(v1: UUID)
    val testUUID = UUID.fromString("e5240c08-6ee7-474a-b5e4-91f79c48338f")

    //delete old values
    val q0 = quote(query[EncodingUUIDTestEntity].delete)
    val rez0 = runSyncUnsafe(testContext.run(q0))

    //insert new uuid
    val rez1 = runSyncUnsafe(testContext.run(query[EncodingUUIDTestEntity].insertValue(lift(EncodingUUIDTestEntity(testUUID)))))

    //verify you can get the uuid back from the db
    val q2 = quote(query[EncodingUUIDTestEntity].map(p => p.v1))
    val rez2 = runSyncUnsafe(testContext.run(q2))

    rez2 mustEqual List(testUUID)
  }

  "fails if the column has the wrong type" - {
    "numeric" in {
      runSyncUnsafe(testContext.run(liftQuery(insertValues).foreach(e => insert(e))))
      case class EncodingTestEntity(v1: Int)
      val e = intercept[FiberFailure] {
        runSyncUnsafe(testContext.run(query[EncodingTestEntity]))
      }
    }
    "non-numeric" in {
      runSyncUnsafe(testContext.run(liftQuery(insertValues).foreach(e => insert(e))))
      case class EncodingTestEntity(v1: Date)
      val e = intercept[FiberFailure] {
        runSyncUnsafe(testContext.run(query[EncodingTestEntity]))
      }
    }
  }

  "encodes sets" in {
    val q = quote {
      (set: Query[Int]) =>
        query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    val fut =
      for {
        _ <- testContext.run(query[EncodingTestEntity].delete)
        _ <- testContext.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
        r <- testContext.run(q(liftQuery(insertValues.map(_.v6))))
      } yield r
    verify(runSyncUnsafe(fut))
  }

  "returning UUID" in {
    val success = for {
      uuid <- runSyncUnsafe(testContext.run(insertBarCode(lift(barCodeEntry))))
      barCode <- runSyncUnsafe(testContext.run(findBarCodeByUuid(uuid))).headOption
    } yield {
      verifyBarcode(barCode)
    }
    success must not be empty
  }

  "decodes LocalDate and LocalDateTime types" in {
    case class DateEncodingTestEntity(v1: LocalDate, v2: LocalDateTime)
    val entity = DateEncodingTestEntity(LocalDate.now, LocalDateTime.now)
    val r = for {
      _ <- testContext.run(query[DateEncodingTestEntity].delete)
      _ <- testContext.run(query[DateEncodingTestEntity].insertValue(lift(entity)))
      result <- testContext.run(query[DateEncodingTestEntity])
    } yield result
    runSyncUnsafe(r) mustBe Seq(entity)
  }

  "encodes custom type inside singleton object" in {
    object Singleton {
      def apply()(implicit c: TestContext) = {
        import c._
        for {
          _ <- c.run(query[EncodingTestEntity].delete)
          result <- c.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
        } yield result
      }
    }

    implicit val c = testContext
    runSyncUnsafe(Singleton())
  }
}
