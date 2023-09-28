package io.getquill.context.cassandra

import io.getquill._
import io.getquill.base.Spec

class PeopleCassandraSpec extends Spec {

  import testSyncDB._

  case class Person(id: Int, name: String, age: Int)

  override def beforeAll: Unit = {
    val entries = List(
      Person(1, "Bob", 30),
      Person(2, "Gus", 40),
      Person(3, "Pet", 20),
      Person(4, "Don", 50),
      Person(5, "Dre", 60)
    )
    testSyncDB.run(query[Person].delete)
    testSyncDB.run(liftQuery(entries).foreach(e => query[Person].insertValue(e)))
    ()
  }

  val q: Quoted[Query[Int] => EntityQuery[Person]] = quote { (ids: Query[Int]) =>
    query[Person].filter(p => ids.contains(p.id))
  }

  "Contains id" - {
    "empty" in {
      testSyncDB.run(q(liftQuery(Set.empty[Int]))) mustEqual List.empty[Person]
    }

  }
}
