package io.getquill.context.jdbc.sqlite

import io.getquill.context.sql.OnConflictSpec

class OnConflictJdbcSpec extends OnConflictSpec {
  val ctx = testContext
  import ctx._

  override protected def beforeAll(): Unit = {
    ctx.run(qr1.delete)
    ()
  }

  "ON CONFLICT DO NOTHING" in {
    import `onConflictIgnore`._
    ctx.run(testQuery1) mustEqual res1
    ctx.run(testQuery2) mustEqual res2
    ctx.run(testQuery3) mustEqual res3
  }

  "ON CONFLICT (i) DO NOTHING" in {
    import `onConflictIgnore(_.i)`._
    ctx.run(testQuery1) mustEqual res1
    ctx.run(testQuery2) mustEqual res2
    ctx.run(testQuery3) mustEqual res3
  }

  "ON CONFLICT (i) DO UPDATE ..." in {
    import `onConflictUpdate(_.i)((t, e) => ...)`._
    ctx.run(testQuery(e1)) mustEqual res1
    ctx.run(testQuery(e2)) mustEqual res2
    ctx.run(testQuery(e3)) mustEqual res3
    ctx.run(testQuery4) mustEqual res4
  }
}
