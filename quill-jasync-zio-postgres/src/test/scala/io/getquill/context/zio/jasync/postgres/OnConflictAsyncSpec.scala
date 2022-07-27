package io.getquill.context.zio.jasync.postgres

import io.getquill.context.sql.base.OnConflictSpec

class OnConflictAsyncSpec extends OnConflictSpec with ZioSpec {

  import context._

  override protected def beforeAll(): Unit = {
    runSyncUnsafe(context.run(qr1.delete))
    ()
  }

  "ON CONFLICT DO NOTHING" in {
    import `onConflictIgnore`._
    runSyncUnsafe(context.run(testQuery1)) mustEqual res1
    runSyncUnsafe(context.run(testQuery2)) mustEqual res2
    runSyncUnsafe(context.run(testQuery3)) mustEqual res3
  }

  "ON CONFLICT (i) DO NOTHING" in {
    import `onConflictIgnore(_.i)`._
    runSyncUnsafe(context.run(testQuery1)) mustEqual res1
    runSyncUnsafe(context.run(testQuery2)) mustEqual res2
    runSyncUnsafe(context.run(testQuery3)) mustEqual res3
  }

  "ON CONFLICT (i) DO UPDATE ..." in {
    import `onConflictUpdate(_.i)((t, e) => ...)`._
    runSyncUnsafe(context.run(testQuery(e1))) mustEqual res1
    runSyncUnsafe(context.run(testQuery(e2))) mustEqual res2
    runSyncUnsafe(context.run(testQuery(e3))) mustEqual res3
    runSyncUnsafe(context.run(testQuery4)) mustEqual res4
  }
}
