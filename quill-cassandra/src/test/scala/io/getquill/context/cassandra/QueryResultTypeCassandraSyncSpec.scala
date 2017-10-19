package io.getquill.context.cassandra

class QueryResultTypeCassandraSyncSpec extends QueryResultTypeCassandraSpec {

  val context = testSyncDB

  import context._

  override def beforeAll = {
    context.run(deleteAll)
    context.run(liftQuery(entries).foreach(e => insert(e)))
    ()
  }

  "return list" - {
    "select" in {
      context.run(selectAll) must contain theSameElementsAs (entries)
    }
    "map" in {
      context.run(map) must contain theSameElementsAs (entries.map(_.id))
    }
    "filter" in {
      context.run(filter) mustEqual entries.take(1)
    }
    "withFilter" in {
      context.run(withFilter) mustEqual entries.take(1)
    }
    "sortBy" in {
      context.run(sortBy) mustEqual entries.take(1)
    }
    "take" in {
      context.run(take) must contain theSameElementsAs (entries)
    }
  }

  "return single result" - {
    "size" in {
      context.run(entitySize) mustEqual entries.size
    }
    "paramlize size" in {
      context.run(parametrizedSize(lift(10000))) mustEqual 0
    }
  }

  "io monad" in {
    performIO(runIO(selectAll)) mustEqual performIO(runIO(selectAll).transactional)
  }
}
