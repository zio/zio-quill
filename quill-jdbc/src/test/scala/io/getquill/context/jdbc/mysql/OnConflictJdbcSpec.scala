package io.getquill.context.jdbc.mysql

import io.getquill.context.sql.OnConflictSpec

class OnConflictJdbcSpec extends OnConflictSpec {
  val ctx = testContext
  import ctx._

  override protected def beforeAll(): Unit = {
    ctx.run(qr1.delete)
    ()
  }

  "INSERT IGNORE" in {
    import `onConflictIgnore`._
    ctx.run(testQuery1) mustEqual res1
    ctx.run(testQuery2) mustEqual res2
    ctx.run(testQuery3) mustEqual res3
  }

  "ON DUPLICATE KEY UPDATE i=i " in {
    import `onConflictIgnore(_.i)`._
    ctx.run(testQuery1) mustEqual res1
    ctx.run(testQuery2) mustEqual res2 + 1
    ctx.run(testQuery3) mustEqual res3
  }

  "ON DUPLICATE KEY UPDATE ..." in {
    import `onConflictUpdate((t, e) => ...)`._
    ctx.run(testQuery(e1)) mustEqual res1
    ctx.run(testQuery(e2)) mustEqual res2 + 1
    ctx.run(testQuery(e3)) mustEqual res3 + 1
    ctx.run(testQuery4) mustEqual res4
  }
}
