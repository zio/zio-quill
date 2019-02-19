package io.getquill.context.cassandra.lagom

import io.getquill._

class DecodeNullSpec extends Spec {

  "no default values when reading null" - {
    "stream" in {
      import io.getquill.context.cassandra.utils.executionContext
      import testLagomAsyncDB._
      val writeEntities = quote(querySchema[DecodeNullTestWriteEntity]("DecodeNullTestEntity"))

      val result =
        for {
          _ <- testLagomAsyncDB.run(writeEntities.delete)
          _ <- testLagomAsyncDB.run(writeEntities.insert(lift(insertValue)))
          result <- testLagomAsyncDB.run(query[DecodeNullTestEntity])
        } yield {
          result
        }
      intercept[IllegalStateException] {
        await {
          result
        }
      }
    }
  }

  case class DecodeNullTestEntity(id: Int, value: Int)

  case class DecodeNullTestWriteEntity(id: Int, value: Option[Int])

  val insertValue = DecodeNullTestWriteEntity(0, None)
}
