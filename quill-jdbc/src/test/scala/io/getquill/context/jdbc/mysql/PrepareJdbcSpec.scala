package io.getquill.context.jdbc.mysql

import java.sql.{ Connection, ResultSet }
import io.getquill.context.jdbc.PrepareJdbcSpecBase
import org.scalatest.BeforeAndAfter

class PrepareJdbcSpec extends PrepareJdbcSpecBase with BeforeAndAfter {

  val context = testContext
  import testContext._

  before {
    testContext.run(query[Product].delete)
  }

  def productExtractor = (rs: ResultSet, conn: Connection) => materializeQueryMeta[Product].extract(rs, conn)
  val prepareQuery = prepare(query[Product])

  "single" in {
    val prepareInsert = prepare(query[Product].insert(lift(productEntries.head)))
    singleInsert(dataSource.getConnection)(prepareInsert) mustEqual false
    extractProducts(dataSource.getConnection)(prepareQuery) === List(productEntries.head)
  }

  "batch" in {
    val prepareBatchInsert = prepare(
      liftQuery(withOrderedIds(productEntries)).foreach(p => query[Product].insert(p))
    )

    batchInsert(dataSource.getConnection)(prepareBatchInsert).distinct mustEqual List(false)
    extractProducts(dataSource.getConnection)(prepareQuery) === withOrderedIds(productEntries)
  }
}
