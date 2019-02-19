package io.getquill.context.cassandra.streaming

import io.getquill._
import io.getquill.context.cassandra.utils.executionContext
import io.getquill.context.cassandra.utils.materializer

class DecodeNullSpec extends Spec {

  "no default values when reading null" - {
    "stream" in {
      import testStreamDB._
      val writeEntities = quote(querySchema[DecodeNullTestWriteEntity]("DecodeNullTestEntity"))

      val result =
        for {
          _ <- testStreamDB.run(writeEntities.delete).runForeach(_ => ())
          _ <- testStreamDB.run(writeEntities.insert(lift(insertValue))).runForeach(_ => ())
          result <- testStreamDB.run(query[DecodeNullTestEntity]).runFold(List.empty[DecodeNullTestEntity])(_ :+ _)
        } yield {
          result
        }
      intercept[IllegalStateException] {
        await {
          result.map(_.head)
        }
      }
    }
  }

  case class DecodeNullTestEntity(id: Int, value: Int)

  case class DecodeNullTestWriteEntity(id: Int, value: Option[Int])

  val insertValue = DecodeNullTestWriteEntity(0, None)
}
