package io.getquill.context.cassandra.streaming

import akka.{ Done, NotUsed }
import akka.stream.scaladsl.Source
import io.getquill.context.cassandra.{ EncodingSpecHelper, utils }
import io.getquill.Query

import scala.concurrent.Future

class EncodingSpec extends EncodingSpecHelper {

  import utils.executionContext
  import utils.materializer

  def actionResult(stream: Source[Done, NotUsed]): Future[Done] = {
    stream.runForeach(_ => ())
  }

  def queryResult[T](stream: Source[T, NotUsed]): Future[List[T]] = {
    stream.runFold(List.empty[T])(_ :+ _)
  }

  "encodes and decodes types" - {
    "stream" in {
      import testStreamDB._
      val result =
        for {
          _ <- actionResult(testStreamDB.run(query[EncodingTestEntity].delete))
          _ <- actionResult(testStreamDB.run(
            liftQuery(insertValues)
              .foreach(e =>
                query[EncodingTestEntity].insert(e))
          ))
          result <- queryResult(testStreamDB.run(query[EncodingTestEntity]))
        } yield {
          result
        }
      verify(await(result))
    }
  }

  "encodes collections" - {
    "stream" in {
      import testStreamDB._
      val q = quote {
        (list: Query[Int]) =>
          query[EncodingTestEntity].filter(t => list.contains(t.id))
      }
      val result =
        for {
          _ <- actionResult(testStreamDB.run(query[EncodingTestEntity].delete))
          _ <- actionResult(testStreamDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e))))
          result <- queryResult(testStreamDB.run(q(liftQuery(insertValues.map(_.id)))))
        } yield {
          result
        }
      verify(await(result))
    }
  }
}
