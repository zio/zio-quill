package zio.quill.context.cassandra.zio

import zio.quill.context.cassandra.EncodingSpecHelper
import zio.quill.Query

class EncodingSpec extends EncodingSpecHelper with ZioCassandraSpec {
  "encodes and decodes types" - {
    "stream" in {
      import testZioDB._
      val ret =
        for {
          _      <- testZioDB.run(query[EncodingTestEntity].delete)
          _      <- testZioDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
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
      val q = quote { (list: Query[Int]) =>
        query[EncodingTestEntity].filter(t => list.contains(t.id))
      }
      val ret =
        for {
          _      <- testZioDB.run(query[EncodingTestEntity].delete)
          _      <- testZioDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
          result <- testZioDB.run(q(liftQuery(insertValues.map(_.id))))
        } yield {
          result
        }
      val f = result(ret)
      verify(f)
    }
  }
}
