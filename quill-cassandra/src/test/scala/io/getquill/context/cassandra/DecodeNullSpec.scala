package io.getquill.context.cassandra

import io.getquill._
import monix.reactive.Observable

class DecodeNullSpec extends Spec {

  "no default values when reading null" - {

    "sync" in {
      import testSyncDB._
      val writeEntities = quote(querySchema[DecodeNullTestWriteEntity]("DecodeNullTestEntity"))

      testSyncDB.run(writeEntities.delete)
      testSyncDB.run(writeEntities.insert(lift(insertValue)))
      intercept[IllegalStateException] {
        testSyncDB.run(query[DecodeNullTestEntity])
      }
    }

    "async" in {
      import testAsyncDB._
      import scala.concurrent.ExecutionContext.Implicits.global
      val writeEntities = quote(querySchema[DecodeNullTestWriteEntity]("DecodeNullTestEntity"))

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
      import monix.execution.Scheduler.Implicits.global
      val writeEntities = quote(querySchema[DecodeNullTestWriteEntity]("DecodeNullTestEntity"))

      val result =
        for {
          _ <- testStreamDB.run(writeEntities.delete)
          _ <- Observable.fromTask(testStreamDB.run(writeEntities.insert(lift(insertValue))).countL)
          result <- testStreamDB.run(query[DecodeNullTestEntity])
        } yield {
          result
        }
      intercept[IllegalStateException] {
        await {
          result.headL.runAsync
        }
      }
    }
  }

  case class DecodeNullTestEntity(id: Int, value: Int)

  case class DecodeNullTestWriteEntity(id: Int, value: Option[Int])

  val insertValue = DecodeNullTestWriteEntity(0, None)

}
