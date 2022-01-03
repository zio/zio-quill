package io.getquill.context.cassandra.alpakka

import akka.Done
import io.getquill.context.ExecutionInfo

import scala.util.Try

class CassandraContextSpec extends CassandraAlpakkaSpec {
  import testDB._
  "run non-batched action" in {
    case class TestEntity(id: Int, s: String, i: Int, l: Long, o: Int)
    val update = quote {
      query[TestEntity].filter(_.id == lift(1)).update(_.i -> lift(1))
    }
    await(testDB.run(update)) mustEqual Done
  }

  "fail on returning" in {

    val p: Prepare = (x, session) => (Nil, x)
    val e: Extractor[Int] = (_, _) => 1

    intercept[IllegalStateException](executeActionReturning("", p, e, "")(ExecutionInfo.unknown, ())).getMessage mustBe
      intercept[IllegalStateException](executeBatchActionReturning(Nil, e)(ExecutionInfo.unknown, ())).getMessage
  }

  "return failed future on `prepare` error in async context" - {
    "query" in {
      val f = testDB.executeQuery("bad cql")(ExecutionInfo.unknown, ())
      Try(await(f)).isFailure mustEqual true
      ()
    }
    "action" in {
      val f = testDB.executeAction("bad cql")(ExecutionInfo.unknown, ())
      Try(await(f)).isFailure mustEqual true
      ()
    }
  }
}
