package io.getquill.context.cassandra.zio

import io.getquill.context.cassandra.QueryResultTypeCassandraSpec
import org.scalatest.BeforeAndAfter

class StreamingWithFetchSpec extends ZioCassandraSpec with QueryResultTypeCassandraSpec with BeforeAndAfter {

  val context = testZioDB

  import context._

  before {
    result(context.run(deleteAll))
    ()
  }

  "streaming with fetch should work" - {
    def produceEntities(num: Int) =
      (1 to num).map(i => OrderTestEntity(i, i)).toList

    "with entities == 1/2 * fetch size" in {
      val entities = produceEntities(5)
      result(
        context.run(liftQuery(entities).foreach(e => insert(e))) *>
          context.stream(selectAll, 10).runCollect.map(_.toList)
      ) must contain theSameElementsAs entities
    }
    "with entities == fetch size" in {
      val entities = produceEntities(10)
      result(
        context.run(liftQuery(entities).foreach(e => insert(e))) *>
          context.stream(selectAll, 10).runCollect.map(_.toList)
      ) must contain theSameElementsAs entities
    }
    "with entities == 1.5 * fetch size" in {
      val entities = produceEntities(15)
      result(
        context.run(liftQuery(entities).foreach(e => insert(e))) *>
          context.stream(selectAll, 10).runCollect.map(_.toList)
      ) must contain theSameElementsAs entities
    }
  }
}
