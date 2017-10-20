package io.getquill.context.cassandra

import scala.concurrent.ExecutionContext.Implicits.global

class QueryResultTypeCassandraAsyncSpec extends QueryResultTypeCassandraSpec {

  val context = testAsyncDB

  import context._

  override def beforeAll = {
    await(context.run(deleteAll))
    await(context.run(liftQuery(entries).foreach(e => insert(e))))
    ()
  }

  "return list" - {
    "select" in {
      await(context.run(selectAll)) must contain theSameElementsAs (entries)
    }
    "map" in {
      await(context.run(map)) must contain theSameElementsAs (entries.map(_.id))
    }
    "filter" in {
      await(context.run(filter)) mustEqual entries.take(1)
    }
    "withFilter" in {
      await(context.run(withFilter)) mustEqual entries.take(1)
    }
    "sortBy" in {
      await(context.run(sortBy)) mustEqual entries.take(1)
    }
    "take" in {
      await(context.run(take)) must contain theSameElementsAs (entries)
    }
  }

  "return single result" - {
    "size" in {
      await(context.run(entitySize)) mustEqual entries.size
    }
    "paramlize size" in {
      await(context.run(parametrizedSize(lift(10000)))) mustEqual 0
    }
  }

  "io monad" in {
    await(performIO(runIO(selectAll))) mustEqual await(performIO(runIO(selectAll).transactional))
  }
}
