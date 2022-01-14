package io.getquill.context.cassandra.catEffect

import io.getquill.Query
import io.getquill.context.cassandra.EncodingSpecHelper
import io.getquill.context.cassandra.catsEffect.testCeDB._
import io.getquill.context.cassandra.catsEffect.testCeDB
import cats.effect.unsafe.implicits.global

class EncodingSpec extends EncodingSpecHelper {
  "encodes and decodes types" - {
    "stream" in {
      val result =
        for {
          _ <- testCeDB.run(query[EncodingTestEntity].delete)
          _ <- testCeDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
          result <- testCeDB.run(query[EncodingTestEntity])
        } yield {
          result
        }
      val f = result.unsafeToFuture()
      val r = await(f)
      verify(r)
    }
  }

  "encodes collections" - {
    "stream" in {
      val q = quote {
        (list: Query[Int]) =>
          query[EncodingTestEntity].filter(t => list.contains(t.id))
      }
      val result =
        for {
          _ <- testCeDB.run(query[EncodingTestEntity].delete)
          _ <- testCeDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
          result <- testCeDB.run(q(liftQuery(insertValues.map(_.id))))
        } yield {
          result
        }
      val f = result.unsafeToFuture()
      verify(await(f))
    }
  }
}
