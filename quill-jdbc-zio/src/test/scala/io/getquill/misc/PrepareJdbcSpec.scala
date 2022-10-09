package io.getquill.misc

import io.getquill.{ PrepareZioJdbcSpecBase, ZioProxySpec }
import org.scalatest.BeforeAndAfter

import java.sql.{ Connection, ResultSet }

class PrepareJdbcSpec extends PrepareZioJdbcSpecBase with ZioProxySpec with BeforeAndAfter {

  val context = testContext
  import context._

  before {
    testContext.run(query[Product].delete).runSyncUnsafe()
  }

  def productExtractor = (rs: ResultSet, conn: Connection) => materializeQueryMeta[Product].extract(rs, conn)
  val prepareQuery = prepare(query[Product])

  "single" in {
    val prepareInsert = prepare(query[Product].insertValue(lift(productEntries.head)))
    singleInsert(prepareInsert) mustEqual false
    extractProducts(prepareQuery) === List(productEntries.head)
  }

  "batch" in {
    val prepareBatchInsert = prepare(
      liftQuery(withOrderedIds(productEntries)).foreach(p => query[Product].insertValue(p))
    )

    batchInsert(prepareBatchInsert).distinct mustEqual List(false)
    extractProducts(prepareQuery) === withOrderedIds(productEntries)
  }
}
