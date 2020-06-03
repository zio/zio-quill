package io.getquill.context.cassandra.monix

import io.getquill.context.cassandra.EncodingSpecHelper
import io.getquill.Query

class EncodingSpec extends EncodingSpecHelper {
  "encodes and decodes types" - {
    "stream" in {
      import monix.execution.Scheduler.Implicits.global
      import testMonixDB._
      val result =
        for {
          _ <- testMonixDB.run(query[EncodingTestEntity].delete)
          _ <- testMonixDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
          result <- testMonixDB.run(query[EncodingTestEntity])
        } yield {
          result
        }
      val f = result.runToFuture
      verify(await(f))
    }
  }

  "encodes collections" - {
    "stream" in {
      import monix.execution.Scheduler.Implicits.global
      import testMonixDB._
      val q = quote {
        (list: Query[Int]) =>
          query[EncodingTestEntity].filter(t => list.contains(t.id))
      }
      val result =
        for {
          _ <- testMonixDB.run(query[EncodingTestEntity].delete)
          _ <- testMonixDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
          result <- testMonixDB.run(q(liftQuery(insertValues.map(_.id))))
        } yield {
          result
        }
      val f = result.runToFuture
      verify(await(f))
    }
  }
}
