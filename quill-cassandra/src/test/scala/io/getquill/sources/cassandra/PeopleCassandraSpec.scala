package io.getquill.sources.cassandra

import io.getquill._

class PeopleCassandraSpec extends Spec {
  case class Person(id: Int, name: String, age: Int)

  override def beforeAll = {
    val entries = List(
      Person(1, "Bob", 30),
      Person(2, "Gus", 40),
      Person(3, "Pet", 20),
      Person(4, "Don", 50),
      Person(5, "Dre", 60)
    )
    testSyncDB.run(query[Person].delete)
    testSyncDB.run(query[Person].insert)(entries)
    ()
  }

  val q = quote {
    (ids: Set[Int]) => query[Person].filter(p => ids.contains(p.id))
  }

  "Contains id" - {
    "empty" in {
      testSyncDB.run(q)(Set.empty[Int]) mustEqual List.empty[Person]
    }

  }
}
