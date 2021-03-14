package io.getquill.postgres

import io.getquill.context.ZioJdbc.Prefix
import io.getquill.{ PrepareZioJdbcSpecBase, ZioSpec }
import org.scalatest.BeforeAndAfter

import java.sql.ResultSet

class PrepareJdbcSpec extends PrepareZioJdbcSpecBase with ZioSpec with BeforeAndAfter {

  override def prefix: Prefix = Prefix("testPostgresDB")
  val context = testContext
  import testContext._

  before {
    testContext.run(query[Product].delete).runSyncUnsafe()
  }

  def productExtractor = (rs: ResultSet) => materializeQueryMeta[Product].extract(rs)
  val prepareQuery = prepare(query[Product])

  "single" in {
    val prepareInsert = prepare(query[Product].insert(lift(productEntries.head)))
    singleInsert(prepareInsert) mustEqual false
    extractProducts(prepareQuery) === List(productEntries.head)
  }

  "batch" in {
    val prepareBatchInsert = prepare(
      liftQuery(withOrderedIds(productEntries)).foreach(p => query[Product].insert(p))
    )

    batchInsert(prepareBatchInsert).distinct mustEqual List(false)
    extractProducts(prepareQuery) === withOrderedIds(productEntries)
  }
}
