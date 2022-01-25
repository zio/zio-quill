package io.getquill.context.cassandra.alpakka

import akka.stream.scaladsl.Sink
import io.getquill._

class PeopleCassandraSpec extends CassandraAlpakkaSpec {

  import testDB._

  case class Person(id: Int, name: String, age: Int)

  val entries = List(
    Person(1, "Bob", 30),
    Person(2, "Gus", 40),
    Person(3, "Pet", 20),
    Person(4, "Don", 50),
    Person(5, "Dre", 60)
  )

  override def beforeAll = {
    await {
      testDB.run(query[Person].delete)
    }
    await {
      testDB.run(liftQuery(entries).foreach(e => query[Person].insert(e)))
    }
    ()
  }

  val qByIds = quote {
    (ids: Query[Int]) => query[Person].filter(p => ids.contains(p.id))
  }

  val q = quote {
    query[Person]
  }

  "Contains id" - {
    "query empty" in {
      await {
        testDB.run(qByIds(liftQuery(Set.empty[Int]))).map(res => res mustEqual List.empty[Person])
      }
    }

    "stream empty" in {
      await {
        testDB.stream(qByIds(liftQuery(Set.empty[Int]))).runWith(Sink.seq).map(res => res mustEqual Seq.empty[Person])
      }
    }

    val evenEntries = entries.filter(e => e.id % 2 == 0).toSet
    val evenIds = evenEntries.map(_.id)

    "query" in {
      await {
        testDB.run(qByIds(liftQuery(evenIds))).map(res => res.toSet mustEqual evenEntries)
      }
    }

    "stream" in {
      await {
        testDB.stream(qByIds(liftQuery(evenIds))).runWith(Sink.seq).map(res => res.toSet mustEqual evenEntries)
      }
      await {
        testDB.stream(q).filter(e => evenIds.contains(e.id)).runWith(Sink.seq).map(res => res.toSet mustEqual evenEntries)
      }
    }
  }

  "All" - {
    "query" in {
      await {
        testDB.run(q).map(res => res.toSet mustEqual entries.toSet)
      }
    }

    "stream" in {
      await {
        testDB.stream(q).runWith(Sink.seq).map(res => res.toSet mustEqual entries.toSet)
      }
    }
  }
}
