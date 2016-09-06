package io.getquill.context.jdbc.postgres

import java.sql.Types
import java.util.UUID

import io.getquill.context.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec {

  val context = testContext
  import testContext._

  "encodes and decodes types" in {
    testContext.run(delete)
    testContext.run(liftQuery(insertValues).foreach(e => insert(e)))
    verify(testContext.run(query[EncodingTestEntity]))
  }

  "encodes sets" in {
    testContext.run(query[EncodingTestEntity].delete)
    testContext.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
    val q = quote {
      (set: Query[Int]) =>
        query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    verify(testContext.run(q(liftQuery(insertValues.map(_.v6)))))
  }

  "returning custom type" in {
    implicit val uuidDecoder: Decoder[UUID] =
      decoder[UUID] { row => index => UUID.fromString(row.getObject(index).toString)
      }
    implicit val uuidEncoder: Encoder[UUID] =
      encoder[UUID](row => (idx, uuid) =>
        row.setObject(idx, uuid, Types.OTHER), Types.OTHER)

    val uuid = testContext.run(insertBarCode.apply(lift(barCodeEntry))).get
    val (barCode :: Nil) = testContext.run(findBarCodeByUuid(uuid))

    verifyBarcode(barCode)
  }
}
