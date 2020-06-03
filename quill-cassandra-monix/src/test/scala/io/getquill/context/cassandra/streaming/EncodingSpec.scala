package io.getquill.context.cassandra.streaming

import io.getquill.context.cassandra.EncodingSpecHelper
import monix.reactive.Observable

class EncodingSpec extends EncodingSpecHelper {
  "encodes and decodes types" - {
    "stream" in {
      import monix.execution.Scheduler.Implicits.global
      import testStreamDB._
      val result =
        for {
          _ <- testStreamDB.run(query[EncodingTestEntity].delete)
          inserts = Observable(insertValues: _*)
          _ <- Observable.fromTask(testStreamDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e))).countL)
          result <- testStreamDB.run(query[EncodingTestEntity])
        } yield {
          result
        }
      val f = result.foldLeftL(List.empty[EncodingTestEntity])(_ :+ _).runToFuture
      verify(await(f))
    }
  }

  "encodes collections" - {
    "stream" in {
      import monix.execution.Scheduler.Implicits.global
      import testStreamDB._
      val q = quote {
        (list: Query[Int]) =>
          query[EncodingTestEntity].filter(t => list.contains(t.id))
      }
      val result =
        for {
          _ <- testStreamDB.run(query[EncodingTestEntity].delete)
          inserts = Observable(insertValues: _*)
          _ <- Observable.fromTask(testStreamDB.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e))).countL)
          result <- testStreamDB.run(q(liftQuery(insertValues.map(_.id))))
        } yield {
          result
        }
      val f = result.foldLeftL(List.empty[EncodingTestEntity])(_ :+ _).runToFuture
      verify(await(f))
    }
  }
}
