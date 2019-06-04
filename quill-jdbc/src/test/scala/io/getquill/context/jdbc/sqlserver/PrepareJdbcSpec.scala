package io.getquill.context.jdbc.sqlserver

import java.sql.ResultSet

import io.getquill.context.jdbc.PrepareJdbcSpecBase
import org.scalatest.BeforeAndAfter

class PrepareJdbcSpec extends PrepareJdbcSpecBase with BeforeAndAfter {

  val context = testContext
  import testContext._

  before {
    testContext.run(query[Product].delete)
  }

  def productExtractor = (rs: ResultSet) => materializeQueryMeta[Product].extract(rs)
  val prepareQuery = prepare(query[Product])
  implicit val im = insertMeta[Product](_.id)

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
