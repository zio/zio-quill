package io.getquill.context.cassandra

import io.getquill._
import scala.concurrent.ExecutionContext.Implicits.{ global => ec }
import scala.util.Try

class CassandraContextSpec extends Spec {

  "run non-batched action" - {

    "async" in {
      import testAsyncDB._
      case class TestEntity(id: Int, s: String, i: Int, l: Long, o: Int)
      val update = quote {
        query[TestEntity].filter(_.id == lift(1)).update(_.i -> lift(1))
      }
      await(testAsyncDB.run(update)) mustEqual (())
    }
    "sync" in {
      import testSyncDB._
      case class TestEntity(id: Int, s: String, i: Int, l: Long, o: Int)
      val update = quote {
        query[TestEntity].filter(_.id == lift(1)).update(_.i -> lift(1))
      }
      testSyncDB.run(update) mustEqual (())
    }
  }

  "async - returns failed future if prepare fails" in {
    import testAsyncDB._
    case class InvalidTestEntity(id: Int, s: String, i: Int, l: Long, o: Int)
    val update = quote {
      query[InvalidTestEntity].filter(_.id == lift(1)).update(_.i -> lift(1))
    }
    val fut = testAsyncDB.run(update)
    Try(await(fut)).isFailure mustEqual true
  }
}
