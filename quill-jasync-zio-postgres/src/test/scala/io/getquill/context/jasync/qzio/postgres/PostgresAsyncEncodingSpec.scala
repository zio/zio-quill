package io.getquill.context.jasync.qzio.postgres

import java.time.{ LocalDate, LocalDateTime, ZonedDateTime }

import io.getquill.context.sql.EncodingSpec
import org.joda.time.{ DateTime => JodaDateTime, LocalDate => JodaLocalDate, LocalDateTime => JodaLocalDateTime }

import java.util.Date
import java.util.UUID
import io.getquill.Query

class PostgresAsyncEncodingSpec extends EncodingSpec with ZioSpec {

  val context = testContext
  import testContext._

  "encodes and decodes types" in {
    val r =
      for {
        _ <- testContext.run(delete)
        _ <- testContext.run(liftQuery(insertValues).foreach(e => insert(e)))
        result <- testContext.run(query[EncodingTestEntity])
      } yield result

    verify(await(r))
  }

  "encodes and decodes uuids" in {
    case class EncodingUUIDTestEntity(v1: UUID)
    val testUUID = UUID.fromString("e5240c08-6ee7-474a-b5e4-91f79c48338f")

    //delete old values
    val q0 = quote(query[EncodingUUIDTestEntity].delete)
    val rez0 = await(testContext.run(q0))

    //insert new uuid
    val rez1 = await(testContext.run(query[EncodingUUIDTestEntity].insert(lift(EncodingUUIDTestEntity(testUUID)))))

    //verify you can get the uuid back from the db
    val q2 = quote(query[EncodingUUIDTestEntity].map(p => p.v1))
    val rez2 = await(testContext.run(q2))

    rez2 mustEqual List(testUUID)
  }

  "fails if the column has the wrong type" - {
    "numeric" in {
      await(testContext.run(liftQuery(insertValues).foreach(e => insert(e))))
      case class EncodingTestEntity(v1: Int)
      val e = intercept[IllegalStateException] {
        await(testContext.run(query[EncodingTestEntity]))
      }
    }
    "non-numeric" in {
      await(testContext.run(liftQuery(insertValues).foreach(e => insert(e))))
      case class EncodingTestEntity(v1: Date)
      val e = intercept[IllegalStateException] {
        await(testContext.run(query[EncodingTestEntity]))
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
        _ <- testContext.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
        r <- testContext.run(q(liftQuery(insertValues.map(_.v6))))
      } yield {
        r
      }
    verify(await(fut))
  }

  "returning UUID" in {
    val success = for {
      uuid <- await(testContext.run(insertBarCode(lift(barCodeEntry))))
      barCode <- await(testContext.run(findBarCodeByUuid(uuid))).headOption
    } yield {
      verifyBarcode(barCode)
    }
    success must not be empty
  }

  "decodes joda DateTime, LocalDate and LocalDateTime types" in {
    case class DateEncodingTestEntity(v1: JodaLocalDate, v2: JodaLocalDateTime, v3: JodaDateTime)
    val entity = DateEncodingTestEntity(JodaLocalDate.now, JodaLocalDateTime.now, JodaDateTime.now)
    val r = for {
      _ <- testContext.run(query[DateEncodingTestEntity].delete)
      _ <- testContext.run(query[DateEncodingTestEntity].insert(lift(entity)))
      result <- testContext.run(query[DateEncodingTestEntity])
    } yield result
    await(r) mustBe Seq(entity)
  }

  "decodes ZonedDateTime, LocalDate and LocalDateTime types" in {
    case class DateEncodingTestEntity(v1: LocalDate, v2: LocalDateTime, v3: ZonedDateTime)
    val entity = DateEncodingTestEntity(LocalDate.now, LocalDateTime.now, ZonedDateTime.now)
    val r = for {
      _ <- testContext.run(query[DateEncodingTestEntity].delete)
      _ <- testContext.run(query[DateEncodingTestEntity].insert(lift(entity)))
      result <- testContext.run(query[DateEncodingTestEntity])
    } yield result
    await(r) mustBe Seq(entity)
  }

  "encodes custom type inside singleton object" in {
    object Singleton {
      def apply()(implicit c: TestContext) = {
        import c._
        for {
          _ <- c.run(query[EncodingTestEntity].delete)
          result <- c.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
        } yield result
      }
    }

    implicit val c = testContext
    await(Singleton())
  }
}
