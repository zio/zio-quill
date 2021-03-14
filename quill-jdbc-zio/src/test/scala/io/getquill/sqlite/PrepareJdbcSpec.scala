package io.getquill.sqlite

import java.sql.ResultSet
import io.getquill.PrepareZioJdbcSpecBase
import io.getquill.context.ZioJdbc.Prefix
import org.scalatest.BeforeAndAfter

class PrepareJdbcSpec extends PrepareZioJdbcSpecBase with BeforeAndAfter {

  def prefix = Prefix("testSqliteDB")
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
