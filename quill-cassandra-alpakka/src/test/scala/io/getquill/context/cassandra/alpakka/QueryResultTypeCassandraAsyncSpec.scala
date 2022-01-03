package io.getquill.context.cassandra.alpakka

import akka.stream.scaladsl.Sink
import io.getquill.context.cassandra.QueryResultTypeCassandraSpec

class QueryResultTypeCassandraAsyncSpec extends QueryResultTypeCassandraSpec with CassandraAlpakkaSpec {

  val context = testDB
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

  "stream" - {
    "select" in {
      await(context.stream(selectAll).runWith(Sink.seq)) must contain theSameElementsAs (entries)
    }
    "map" in {
      await(context.stream(map).runWith(Sink.seq)) must contain theSameElementsAs (entries.map(_.id))
    }
    "filter" in {
      await(context.stream(filter).runWith(Sink.seq)) mustEqual entries.take(1)
    }
    "withFilter" in {
      await(context.stream(withFilter).runWith(Sink.seq)) mustEqual entries.take(1)
    }
    "sortBy" in {
      await(context.stream(sortBy).runWith(Sink.seq)) mustEqual entries.take(1)
    }
    "take" in {
      await(context.stream(take).runWith(Sink.seq)) must contain theSameElementsAs (entries)
    }
  }

  "io monad" in {
    await(performIO(runIO(selectAll))) mustEqual await(performIO(runIO(selectAll).transactional))
  }
}
