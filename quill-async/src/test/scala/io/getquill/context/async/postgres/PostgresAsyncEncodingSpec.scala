package io.getquill.context.async.postgres

import io.getquill.context.sql.EncodingSpec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.Date
import java.util.UUID

class PostgresAsyncEncodingSpec extends EncodingSpec {

  val context = testContext
  import testContext._

  "encodes and decodes types" in {
    val r =
      for {
        _ <- testContext.run(delete)
        _ <- testContext.run(insert)(insertValues)
        result <- testContext.run(query[EncodingTestEntity])
      } yield result

    verify(Await.result(r, Duration.Inf).toList)
  }

  "encodes and decodes uuids" in {
    case class EncodingUUIDTestEntity(v1: UUID)
    val testUUID = UUID.fromString("e5240c08-6ee7-474a-b5e4-91f79c48338f")

    //delete old values
    val q0 = quote(query[EncodingUUIDTestEntity].delete)
    val rez0 = Await.result(testContext.run(q0), Duration.Inf)

    //insert new uuid
    val q1 = quote(query[EncodingUUIDTestEntity].insert)
    val rez1 = Await.result(testContext.run(q1)(List(EncodingUUIDTestEntity(testUUID))), Duration.Inf)

    //verify you can get the uuid back from the db
    val q2 = quote(query[EncodingUUIDTestEntity].map(p => p.v1))
    val rez2 = Await.result(testContext.run(q2), Duration.Inf)

    rez2 mustEqual List(testUUID)
  }

  "fails if the column has the wrong type" - {
    "numeric" in {
      Await.result(testContext.run(insert)(insertValues), Duration.Inf)
      case class EncodingTestEntity(v1: Int)
      val e = intercept[IllegalStateException] {
        Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
      }
    }
    "non-numeric" in {
      Await.result(testContext.run(insert)(insertValues), Duration.Inf)
      case class EncodingTestEntity(v1: Date)
      val e = intercept[IllegalStateException] {
        Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
      }
    }
  }

  "encodes sets" in {
    val q = quote {
      (set: Set[Int]) =>
        query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    val fut =
      for {
        _ <- testContext.run(query[EncodingTestEntity].delete)
        _ <- testContext.run(query[EncodingTestEntity].insert)(insertValues)
        r <- testContext.run(q)(insertValues.map(_.v6).toSet)
      } yield {
        r
      }
    verify(Await.result(fut, Duration.Inf))
  }

  "returning UUID" in {
    val success = for {
      uuidOpt <- Await.result(testContext.run(insertBarCode)(barCodeEntry), Duration.Inf).headOption
      uuid <- uuidOpt
      barCode <- Await.result(testContext.run(findBarCodeByUuid(uuid)), Duration.Inf).headOption
    } yield {
      verifyBarcode(barCode)
    }
    success must not be empty
  }
}
