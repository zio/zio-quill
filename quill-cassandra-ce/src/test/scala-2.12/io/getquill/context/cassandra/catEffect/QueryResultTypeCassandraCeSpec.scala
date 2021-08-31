package io.getquill.context.cassandra.catEffect

import io.getquill.context.cassandra.QueryResultTypeCassandraSpec
import io.getquill.context.cassandra.catsEffect.testCeDB._
import io.getquill.context.cassandra.catsEffect.testCeDB
import cats.effect.unsafe.implicits.global
import cats.effect.IO
import io.getquill.{ Ord, Spec }

class QueryResultTypeCassandraCeSpec extends Spec {

  def result[A](fa: IO[A]): A = fa.unsafeRunSync()
  case class OrderTestEntity(id: Int, i: Int)

  val entries = List(
    OrderTestEntity(1, 1),
    OrderTestEntity(2, 2),
    OrderTestEntity(3, 3)
  )

  val insert = quote((e: OrderTestEntity) => query[OrderTestEntity].insert(e))
  val deleteAll = quote(query[OrderTestEntity].delete)
  val selectAll = quote(query[OrderTestEntity])
  val map = quote(query[OrderTestEntity].map(_.id))
  val filter = quote(query[OrderTestEntity].filter(_.id == 1))
  val withFilter = quote(query[OrderTestEntity].withFilter(_.id == 1))
  val sortBy = quote(query[OrderTestEntity].filter(_.id == 1).sortBy(_.i)(Ord.asc))
  val take = quote(query[OrderTestEntity].take(10))
  val entitySize = quote(query[OrderTestEntity].size)
  val parametrizedSize = quote { (id: Int) =>
    query[OrderTestEntity].filter(_.id == id).size
  }
  val distinct = quote(query[OrderTestEntity].map(_.id).distinct)

  override def beforeAll: Unit = {
    testCeDB.run(deleteAll).unsafeRunSync()
    testCeDB.run(liftQuery(entries).foreach(e => insert(e))).unsafeRunSync()
    ()
  }

  "query" in {
    result(testCeDB.run(selectAll)) mustEqual entries
  }

  "querySingle" - {
    "size" in {
      result(testCeDB.run(entitySize)) mustEqual 3
    }

    "parametrized size" in {
      result(testCeDB.run(parametrizedSize(lift(10000)))) mustEqual 0
    }
  }
}
