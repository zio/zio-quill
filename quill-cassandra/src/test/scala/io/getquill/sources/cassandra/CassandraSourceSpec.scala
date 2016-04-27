package io.getquill.sources.cassandra

import com.datastax.driver.core.ResultSet
import io.getquill._
import scala.concurrent.ExecutionContext.Implicits.{ global => ec }

class CassandraSourceSpec extends Spec {

  "run non-batched action" - {

    case class TestEntity(id: Int, s: String, i: Int, l: Long, o: Int)
    val update = quote { (id: Int, i: Int) =>
      query[TestEntity].filter(_.id == id).update(_.i -> i)
    }

    "for async source" in {
      await(testAsyncDB.run(update)(1 -> 1)) mustBe an[ResultSet]
    }
    "for sync source" in {
      testSyncDB.run(update)(1 -> 1) mustBe an[ResultSet]
    }
  }
}
