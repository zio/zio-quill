package io.getquill.context.cassandra.monix

import io.getquill._

class DecodeNullSpec extends Spec {

  "no default values when reading null" - {
    "stream" in {
      import monix.execution.Scheduler.Implicits.global
      import testMonixDB._
      val writeEntities = quote(querySchema[DecodeNullTestWriteEntity]("DecodeNullTestEntity"))

      val result =
        for {
          _ <- testMonixDB.run(writeEntities.delete)
          _ <- testMonixDB.run(writeEntities.insert(lift(insertValue)))
          result <- testMonixDB.run(query[DecodeNullTestEntity])
        } yield {
          result
        }
      intercept[IllegalStateException] {
        await {
          result.runToFuture
        }
      }
    }
  }

  case class DecodeNullTestEntity(id: Int, value: Int)

  case class DecodeNullTestWriteEntity(id: Int, value: Option[Int])

  val insertValue = DecodeNullTestWriteEntity(0, None)
}
