package io.getquill.context.cassandra

import io.getquill._

class PeopleCassandraSpec extends Spec {

  import testSyncDB._

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
    testSyncDB.run(liftQuery(entries).foreach(e => query[Person].insert(e)))
    ()
  }

  val q = quote {
    (ids: Query[Int]) => query[Person].filter(p => ids.contains(p.id))
  }

  "Contains id" - {
    "empty" in {
      testSyncDB.run(q(liftQuery(Set.empty[Int]))) mustEqual List.empty[Person]
    }

  }
}
