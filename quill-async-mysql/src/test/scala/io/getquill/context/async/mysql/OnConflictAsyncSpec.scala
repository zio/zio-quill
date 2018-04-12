package io.getquill.context.async.mysql

import io.getquill.context.sql.OnConflictSpec

import scala.concurrent.ExecutionContext.Implicits.global

class OnConflictAsyncSpec extends OnConflictSpec {
  val ctx = testContext
  import ctx._

  override protected def beforeAll(): Unit = {
    await(ctx.run(qr1.delete))
    ()
  }

  "INSERT IGNORE" in {
    import `onConflictIgnore`._
    await(ctx.run(testQuery1)) mustEqual res1
    await(ctx.run(testQuery2)) mustEqual res2
    await(ctx.run(testQuery3)) mustEqual res3
  }

  "ON DUPLICATE KEY UPDATE i=i " in {
    import `onConflictIgnore(_.i)`._
    await(ctx.run(testQuery1)) mustEqual res1
    await(ctx.run(testQuery2)) mustEqual res2
    await(ctx.run(testQuery3)) mustEqual res3
  }

  "ON DUPLICATE KEY UPDATE ..." in {
    import `onConflictUpdate((t, e) => ...)`._
    await(ctx.run(testQuery(e1))) mustEqual res1
    await(ctx.run(testQuery(e2))) mustEqual res2 + 1
    await(ctx.run(testQuery(e3))) mustEqual res3 + 1
    await(ctx.run(testQuery4)) mustEqual res4
  }
}

