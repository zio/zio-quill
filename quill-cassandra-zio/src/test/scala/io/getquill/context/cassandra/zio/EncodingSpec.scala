package io.getquill.context.cassandra.zio

import io.getquill.context.cassandra.EncodingSpecHelper
import io.getquill.Query

class EncodingSpec extends EncodingSpecHelper with ZioCassandraSpec {
  "encodes and decodes types" - {
    "stream" in {
      import testZioDB._
      val ret =
        for {
          _ <- testZioDB.run(query[EncodingTestEntity].delete)
          _ <- testZioDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
          result <- testZioDB.run(query[EncodingTestEntity])
        } yield {
          result
        }
      val f = result(ret)
      verify(f)
    }
  }

  "encodes collections" - {
    "stream" in {
      import testZioDB._
      val q = quote {
        (list: Query[Int]) =>
          query[EncodingTestEntity].filter(t => list.contains(t.id))
      }
      val ret =
        for {
          _ <- testZioDB.run(query[EncodingTestEntity].delete)
          _ <- testZioDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
          result <- testZioDB.run(q(liftQuery(insertValues.map(_.id))))
        } yield {
          result
        }
      val f = result(ret)
      verify(f)
    }
  }
}

