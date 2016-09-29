package io.getquill.context.cassandra

import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive.Observable

class QueryResultTypeCassandraStreamSpec extends QueryResultTypeCassandraSpec {

  val context = testStreamDB

  import context._

  def result[T](t: Observable[T]) =
    await(t.foldLeft(List.empty[T])(_ :+ _).asFuture)

  override def beforeAll = {
    result(context.run(deleteAll))
    result(context.run(liftQuery(entries).foreach(e => insert(e))))
    ()
  }

  "query" in {
    result(context.run(selectAll)) mustEqual Some(entries)
  }

  "querySingle" - {
    "size" in {
      result(context.run(entitySize)) mustEqual Some(List(3))
    }
    "parametrized size" in {
      result(context.run(parametrizedSize(lift(10000)))) mustEqual Some(List(0))
    }
  }
}
