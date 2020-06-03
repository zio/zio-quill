package io.getquill.context.orientdb

import io.getquill.Spec

class PeopleOrientDBSpec extends Spec {

  case class Person(id: Int, name: String, age: Int)

  override protected def beforeAll(): Unit = {
    val ctx = orientdb.testSyncDB
    import ctx._
    val entries = List(
      Person(1, "Bob", 30),
      Person(2, "Gus", 40),
      Person(3, "Pet", 20),
      Person(4, "Don", 50),
      Person(5, "Dre", 60)
    )
    ctx.run(query[Person].delete)
    ctx.run(liftQuery(entries).foreach(e => query[Person].insert(e)))
    ()
  }

  "Contains id" - {
    "empty" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      val q = quote {
        (ids: Query[Int]) => query[Person].filter(p => ids.contains(p.id))
      }
      ctx.run(q(liftQuery(Set.empty[Int]))) mustEqual List.empty[Person]
    }
  }
}