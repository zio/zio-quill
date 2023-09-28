package io.getquill.sqlite

import java.sql.{Connection, ResultSet}
import io.getquill.PrepareZioJdbcSpecBase
import io.getquill.context.qzio.ImplicitSyntax.Implicit
import org.scalatest.BeforeAndAfter
import io.getquill.context.ZioJdbc
import java.sql.PreparedStatement
import javax.sql.DataSource

class PrepareJdbcSpec extends PrepareZioJdbcSpecBase with BeforeAndAfter {

  val context = testContext.underlying
  import context._
  implicit val implicitPool: Implicit[DataSource] = Implicit(pool)

  before {
    testContext.run(query[Product].delete).runSyncUnsafe()
  }

  def productExtractor: (ResultSet, Connection) => Product = (rs: ResultSet, conn: Connection) => materializeQueryMeta[Product].extract(rs, conn)
  val prepareQuery: ZioJdbc.QCIO[PreparedStatement]     = prepare(query[Product])

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
