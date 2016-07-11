package io.getquill.context.jdbc.postgres

import java.sql.Types
import java.util.UUID

import io.getquill.context.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec {

  val context = testContext
  import testContext._

  "encodes and decodes types" in {
    testContext.run(delete)
    testContext.run(insert)(insertValues)
    verify(testContext.run(query[EncodingTestEntity]))
  }

  "encodes sets" in {
    testContext.run(query[EncodingTestEntity].delete)
    testContext.run(query[EncodingTestEntity].insert)(insertValues)
    val q = quote {
      (set: Set[Int]) =>
        query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    verify(testContext.run(q)(insertValues.map(_.v6).toSet))
  }

  "returning custom type" in {
    implicit val uuidDecoder: Decoder[UUID] =
      decoder[UUID] { row => index => UUID.fromString(row.getObject(index).toString)
      }
    implicit val uuidEncoder: Encoder[UUID] =
      encoder[UUID] { row => (idx, uuid) =>
        row.setObject(idx, uuid, Types.OTHER)
      }

    val success = for {
      uuidOpt <- testContext.run(insertBarCode)(barCodeEntry).headOption
      uuid <- uuidOpt
      barCode <- testContext.run(findBarCodeByUuid(uuid)).headOption
    } yield {
      verifyBarcode(barCode)
    }

    success must not be empty

  }
}
