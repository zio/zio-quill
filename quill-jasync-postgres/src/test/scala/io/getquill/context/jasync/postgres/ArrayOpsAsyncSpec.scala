package io.getquill.context.jasync.postgres

import io.getquill.context.sql.ArrayOpsSpec

import scala.concurrent.ExecutionContext.Implicits.global

class ArrayOpsAsyncSpec extends ArrayOpsSpec {
  val ctx = testContext

  import ctx._

  "contains" in {
    await(ctx.run(`contains`.`Ex 1 return all`)) mustBe `contains`.`Ex 1 expected`
    await(ctx.run(`contains`.`Ex 2 return 1`)) mustBe `contains`.`Ex 2 expected`
    await(ctx.run(`contains`.`Ex 3 return 2,3`)) mustBe `contains`.`Ex 3 expected`
    await(ctx.run(`contains`.`Ex 4 return empty`)) mustBe `contains`.`Ex 4 expected`
  }

  override protected def beforeAll(): Unit = {
    await(ctx.run(entity.delete))
    await(ctx.run(insertEntries))
    ()
  }

}
