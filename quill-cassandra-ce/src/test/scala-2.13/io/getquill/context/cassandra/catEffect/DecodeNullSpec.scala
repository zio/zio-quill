package io.getquill.context.cassandra.catEffect

import io.getquill._
import io.getquill.context.cassandra.catsEffect.testCeDB._
import io.getquill.context.cassandra.catsEffect.testCeDB
import cats.effect.unsafe.implicits.global

class DecodeNullSpec extends Spec {

  "no default values when reading null" - {
    "stream" in {
      val writeEntities = quote(querySchema[DecodeNullTestWriteEntity]("DecodeNullTestEntity"))

      val result =
        for {
          _ <- testCeDB.run(writeEntities.delete)
          _ <- testCeDB.run(writeEntities.insert(lift(insertValue)))
          result <- testCeDB.run(query[DecodeNullTestEntity])
        } yield {
          result
        }
      intercept[IllegalStateException] {
        await {
          result.unsafeToFuture()
        }
      }
    }
  }

  case class DecodeNullTestEntity(id: Int, value: Int)

  case class DecodeNullTestWriteEntity(id: Int, value: Option[Int])

  val insertValue = DecodeNullTestWriteEntity(0, None)
}
