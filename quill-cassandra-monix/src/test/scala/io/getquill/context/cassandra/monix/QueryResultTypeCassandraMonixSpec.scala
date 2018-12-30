package io.getquill.context.cassandra.monix

import io.getquill.context.cassandra.QueryResultTypeCassandraSpec
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

class QueryResultTypeCassandraMonixSpec extends QueryResultTypeCassandraSpec {

  val context = testMonixDB

  import context._

  def result[T](t: Task[T]) =
    await(t.runToFuture(global))

  def result[T](t: Observable[T]) =
    await(t.foldLeftL(List.empty[T])(_ :+ _).runToFuture)

  override def beforeAll = {
    result(context.run(deleteAll))
    result(context.run(liftQuery(entries).foreach(e => insert(e))))
    ()
  }

  "query" in {
    result(context.run(selectAll)) mustEqual entries
  }

  "stream" in {
    result(context.stream(selectAll)) mustEqual entries
  }

  "querySingle" - {
    "size" in {
      result(context.run(entitySize)) mustEqual 3
    }
    "parametrized size" in {
      result(context.run(parametrizedSize(lift(10000)))) mustEqual 0
    }
  }
}
