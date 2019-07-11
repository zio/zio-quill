package io.getquill.context.cassandra.lagom

import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.getquill.context.cassandra.QueryResultTypeCassandraSpec

import scala.concurrent.Future

class QueryResultTypeCassandraAsyncSpec extends QueryResultTypeCassandraSpec {

  import io.getquill.context.cassandra.utils.executionContext

  val context = testLagomAsyncDB

  import context._

  def result[T](function: CassandraSession => Future[T]): T = {
    await(function(context.session))
  }

  def result[T](future: Future[T]): T = {
    await(future)
  }

  override def beforeAll = {
    result(context.run(deleteAll))
    result(context.run(liftQuery(entries).foreach(e => insert(e))))
    ()
  }

  "bind" - {
    "action" - {
      "noArgs" in {
        val bs = result(context.prepare(insert(OrderTestEntity(1, 2))))
        bs.preparedStatement().getVariables.size() mustEqual 0
      }

      "withArgs" in {
        val bs = result(context.prepare(insert(lift(OrderTestEntity(1, 2)))))
        bs.preparedStatement().getVariables.size() mustEqual 2
        bs.getInt("id") mustEqual 1
        bs.getInt("i") mustEqual 2
      }
    }

    "query" - {
      "noArgs" in {
        val bs = result(context.prepare(deleteAll))
        bs.preparedStatement().getVariables.size() mustEqual 0
      }

      "withArgs" in {
        val batches = result(context.prepare(liftQuery(List(OrderTestEntity(1, 2))).foreach(e => insert(e))))
        batches.foreach { bs =>
          bs.preparedStatement().getVariables.size() mustEqual 2
          bs.getInt("id") mustEqual 1
        }
      }
    }
  }

  "query" in {
    result(context.run(selectAll)) mustEqual entries
  }

  "querySingle" - {
    "size" in {
      result(context.run(entitySize)) mustEqual Option(3)
    }
    "parametrized size" in {
      result(context.run(parametrizedSize(lift(10000)))) mustEqual Option(0)
    }
  }
}
