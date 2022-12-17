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

    "with entities less than fetch size" in {
      val entities = produceEntities(3)
      result(
        context.run(liftQuery(entities).foreach(e => insert(e))) *>
          context.stream(selectAll, 7).runCollect.map(_.toList.size)
      ) must equal(entities.size) //must contain theSameElementsAs entities
    }
    "with entities == fetch size" in {
      val entities = produceEntities(11)
      result(
        context.run(liftQuery(entities).foreach(e => insert(e))) *>
          context.stream(selectAll, 11).runCollect.map(_.toList)
      ) must contain theSameElementsAs entities
    }
    "with entities more than  fetch size" in {
      val entities = produceEntities(23)
      result(
        context.run(liftQuery(entities).foreach(e => insert(e))) *>
          context.stream(selectAll, 7).runCollect.map(_.toList)
      ) must contain theSameElementsAs entities
    }
  }
}
