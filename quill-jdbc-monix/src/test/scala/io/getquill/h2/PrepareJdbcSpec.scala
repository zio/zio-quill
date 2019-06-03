package io.getquill.h2

import java.sql.ResultSet

import io.getquill.PrepareMonixJdbcSpecBase
import monix.execution.Scheduler
import org.scalatest.BeforeAndAfter

class PrepareJdbcSpec extends PrepareMonixJdbcSpecBase with BeforeAndAfter {

  val context = testContext
  import testContext._
  implicit val scheduler = Scheduler.global

  before {
    testContext.run(query[Product].delete).runSyncUnsafe()
  }

  def productExtractor = (rs: ResultSet) => materializeQueryMeta[Product].extract(rs)
  val prepareQuery = prepare(query[Product])

  "single" in {
    val prepareInsert = prepare(query[Product].insert(lift(productEntries.head)))
    singleInsert(dataSource.getConnection)(prepareInsert).runSyncUnsafe() mustEqual false
    extractProducts(dataSource.getConnection)(prepareQuery).runSyncUnsafe() === List(productEntries.head)
  }

  "batch" in {
    val prepareBatchInsert = prepare(
      liftQuery(withOrderedIds(productEntries)).foreach(p => query[Product].insert(p))
    )

    batchInsert(dataSource.getConnection)(prepareBatchInsert).runSyncUnsafe().distinct mustEqual List(false)
    extractProducts(dataSource.getConnection)(prepareQuery).runSyncUnsafe() === withOrderedIds(productEntries)
  }
}
