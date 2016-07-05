package io.getquill.context.cassandra

import com.datastax.driver.core.ResultSet
import io.getquill._
import scala.concurrent.ExecutionContext.Implicits.{ global => ec }

class CassandraContextSpec extends Spec {

  "run non-batched action" - {

    "async" in {
      import testAsyncDB._
      case class TestEntity(id: Int, s: String, i: Int, l: Long, o: Int)
      val update = quote { (id: Int, i: Int) =>
        query[TestEntity].filter(_.id == id).update(_.i -> i)
      }
      await(testAsyncDB.run(update)(1 -> 1)) mustBe an[ResultSet]
    }
    "sync" in {
      import testSyncDB._
      case class TestEntity(id: Int, s: String, i: Int, l: Long, o: Int)
      val update = quote { (id: Int, i: Int) =>
        query[TestEntity].filter(_.id == id).update(_.i -> i)
      }
      testSyncDB.run(update)(1 -> 1) mustBe an[ResultSet]
    }
  }
}
