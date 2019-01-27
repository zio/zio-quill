package io.getquill.context.cassandra.streaming

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.getquill.context.cassandra.QueryResultTypeCassandraSpec

class QueryResultTypeCassandraStreamSpec extends QueryResultTypeCassandraSpec {

  import io.getquill.context.cassandra.utils._

  val context = testStreamDB
  import context._

  def result[T](stream: Source[T, NotUsed]): List[T] = {
    await(stream.runFold(List.empty[T])(_ :+ _))
  }

  override def beforeAll = {
    result(context.run(deleteAll))
    result(context.run(liftQuery(entries).foreach(e => insert(e))))
    ()
  }

  "query" in {
    result(context.run(selectAll)) mustEqual entries
  }

  "querySingle" - {
    "size" in {
      result(context.run(entitySize)) mustEqual List(3)
    }
    "parametrized size" in {
      result(context.run(parametrizedSize(lift(10000)))) mustEqual List(0)
    }
  }
}
