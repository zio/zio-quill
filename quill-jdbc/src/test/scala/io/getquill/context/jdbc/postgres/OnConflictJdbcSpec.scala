package io.getquill.context.jdbc.postgres

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

  "ON CONFLICT (i) DO UPDATE with schemaMeta ..." in {
    case class TestEntityRename(s2: String, i2: Int, l2: Long, o2: Option[Int])

    def testQuery(e: TestEntityRename) = quote {
      querySchema[TestEntityRename](
        "TestEntity",
        _.s2 -> "s",
        _.i2 -> "i",
        _.l2 -> "l",
        _.o2 -> "o"
      ).insert(lift(e)).onConflictUpdate(_.i2)(
          (t, _) => t.l2 -> (t.l2 + 1)
        )
    }

    val e1Rename = TestEntityRename("r1", 4, 0, None)

    ctx.run(testQuery(e1Rename)) mustEqual 1
  }
}
