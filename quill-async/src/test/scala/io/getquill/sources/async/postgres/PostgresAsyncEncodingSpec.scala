package io.getquill.sources.async.postgres

import io.getquill._
import io.getquill.sources.sql.EncodingSpec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.Date
import java.util.UUID

class PostgresAsyncEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    val r =
      for {
        _ <- testPostgresDB.run(delete)
        _ <- testPostgresDB.run(insert)(insertValues)
        result <- testPostgresDB.run(query[EncodingTestEntity])
      } yield result

    verify(Await.result(r, Duration.Inf).toList)
  }

  "encodes and decodes uuids" in {
    case class EncodingUUIDTestEntity(v1: UUID)
    val testUUID = UUID.fromString("e5240c08-6ee7-474a-b5e4-91f79c48338f")

    //delete old values
    val q0 = quote( query[EncodingUUIDTestEntity].delete )
    val rez0 = Await.result(testPostgresDB.run(q0), Duration.Inf)

    //insert new uuid
    val q1 = quote(query[EncodingUUIDTestEntity].insert)
    val rez1 = Await.result(testPostgresDB.run(q1)(List(EncodingUUIDTestEntity(testUUID))), Duration.Inf)

    //verify you can get the uuid back from the db
    val q2 = quote(query[EncodingUUIDTestEntity].map(p => p.v1))
    val rez2 = Await.result(testPostgresDB.run(q2), Duration.Inf)

    rez2 mustEqual List(testUUID)
  }

  "fails if the column has the wrong type" - {
    "numeric" in {
      Await.result(testPostgresDB.run(insert)(insertValues), Duration.Inf)
      case class EncodingTestEntity(v1: Int)
      val e = intercept[IllegalStateException] {
        Await.result(testPostgresDB.run(query[EncodingTestEntity]), Duration.Inf)
      }
    }
    "non-numeric" in {
      Await.result(testPostgresDB.run(insert)(insertValues), Duration.Inf)
      case class EncodingTestEntity(v1: Date)
      val e = intercept[IllegalStateException] {
        Await.result(testPostgresDB.run(query[EncodingTestEntity]), Duration.Inf)
      }
    }
  }
}
