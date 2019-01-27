package io.getquill.context.cassandra.lagom

import io.getquill.context.cassandra.EncodingSpecHelper

class EncodingSpec extends EncodingSpecHelper {
  "encodes and decodes types" - {
    "stream" in {
      import io.getquill.context.cassandra.utils.executionContext
      import testLagomAsyncDB._
      val result =
        for {
          _ <- testLagomAsyncDB.run(query[EncodingTestEntity].delete)
          _ <- testLagomAsyncDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
          result <- testLagomAsyncDB.run(query[EncodingTestEntity])
        } yield {
          result
        }
      val f = result.map(_.toList)
      verify(await(f))
    }
  }

  "encodes collections" - {
    "stream" in {
      import io.getquill.context.cassandra.utils.executionContext
      import testLagomAsyncDB._
      val q = quote {
        (list: Query[Int]) =>
          query[EncodingTestEntity].filter(t => list.contains(t.id))
      }
      val result =
        for {
          _ <- testLagomAsyncDB.run(query[EncodingTestEntity].delete)
          _ <- testLagomAsyncDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
          result <- testLagomAsyncDB.run(q(liftQuery(insertValues.map(_.id))))
        } yield {
          result
        }
      val f = result.map(_.toList)
      verify(await(f))
    }
  }
}
