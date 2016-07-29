package io.getquill.context.cassandra

import io.getquill._

class DecodeNullSpec extends Spec {

  "no default values when reading null" - {

    "sync" in {
      import testSyncDB._
      val writeEntities = quote(query[DecodeNullTestWriteEntity].schema(_.entity("DecodeNullTestEntity")))

      testSyncDB.run(writeEntities.delete)
      testSyncDB.run(writeEntities.insert(lift(insertValue)))
      intercept[IllegalStateException] {
        testSyncDB.run(query[DecodeNullTestEntity])
      }
    }

    "async" in {
      import testAsyncDB._
      import scala.concurrent.ExecutionContext.Implicits.global
      val writeEntities = quote(query[DecodeNullTestWriteEntity].schema(_.entity("DecodeNullTestEntity")))

      val result =
        for {
          _ <- testAsyncDB.run(writeEntities.delete)
          _ <- testAsyncDB.run(writeEntities.insert(lift(insertValue)))
          result <- testAsyncDB.run(query[DecodeNullTestEntity])
        } yield {
          result
        }
      intercept[IllegalStateException] {
        await {
          result
        }
      }
    }

    "stream" in {
      import testStreamDB._
      import monifu.concurrent.Implicits.globalScheduler
      val writeEntities = quote(query[DecodeNullTestWriteEntity].schema(_.entity("DecodeNullTestEntity")))

      val result =
        for {
          _ <- testStreamDB.run(writeEntities.delete)
          _ <- testStreamDB.run(writeEntities.insert(lift(insertValue))).count
          result <- testStreamDB.run(query[DecodeNullTestEntity])
        } yield {
          result
        }
      intercept[IllegalStateException] {
        await {
          result.asFuture
        }
      }
    }
  }

  case class DecodeNullTestEntity(id: Int, value: Int)

  case class DecodeNullTestWriteEntity(id: Int, value: Option[Int])

  val insertValue = DecodeNullTestWriteEntity(0, None)

}
