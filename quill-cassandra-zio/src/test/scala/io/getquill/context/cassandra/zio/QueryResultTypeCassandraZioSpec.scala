package io.getquill.context.cassandra.zio

import io.getquill.context.cassandra.QueryResultTypeCassandraSpec

class QueryResultTypeCassandraZioSpec extends ZioCassandraSpec with QueryResultTypeCassandraSpec {

  val context = testZioDB

  import context._

  override def beforeAll = {
    super.beforeAll()
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
