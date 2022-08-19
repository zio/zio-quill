package io.getquill.context.jdbc.postgres

import io.getquill.context.sql.base.BatchUpdateValuesSpec
import io.getquill.norm.EnableTrace
import io.getquill.util.Messages.TraceType

class BatchUpdateValuesJdbcSpec extends BatchUpdateValuesSpec { //

  val context = testContext
  import testContext._

  override def beforeEach(): Unit = {
    val schema = quote(querySchema[ContactBase]("Contact"))
    testContext.run(schema.delete)
    super.beforeEach()
  }

  "Ex 1 - Simple Contact" in {
    import `Ex 1 - Simple Contact`._
    context.run(insert)
    context.run(update, 2)
    context.run(get).toSet mustEqual (expect.toSet)
  }

  "Ex 1.1 - Simple Contact With Lift" in {
    import `Ex 1.1 - Simple Contact With Lift`._
    context.run(insert)
    context.run(update, 2)
    context.run(get).toSet mustEqual (expect.toSet)
  }

  "Ex 1.2 - Simple Contact Mixed Lifts" in {
    import `Ex 1.2 - Simple Contact Mixed Lifts`._
    context.run(insert)
    context.run(update, 2)
    context.run(get).toSet mustEqual (expect.toSet)
  }

  "Ex 1.3 - Simple Contact with Multi-Lift-Kinds" in {
    import `Ex 1.3 - Simple Contact with Multi-Lift-Kinds`._
    context.run(insert)
    context.run(update, 2)
    context.run(get).toSet mustEqual (expect.toSet)
  }

  "Ex 2 - Optional Embedded with Renames" in {
    import `Ex 2 - Optional Embedded with Renames`._
    context.run(insert)
    context.run(update, 2)
    context.run(get).toSet mustEqual (expect.toSet)
  }

  "Ex 3 - Deep Embedded Optional" in {
    import `Ex 3 - Deep Embedded Optional`._
    context.run(insert)
    context.run(update, 2)
    context.run(get).toSet mustEqual (expect.toSet)
  }

  "Ex 4 - Returning" in {
    import `Ex 4 - Returning`._
    context.run(insert)
    val agesReturned = context.run(update, 2)
    agesReturned mustEqual expectedReturn
    context.run(get).toSet mustEqual (expect.toSet)
  }

  "Ex 4 - Returning Multiple" in {
    import `Ex 4 - Returning Multiple`._
    context.run(insert)
    val agesReturned = context.run(update, 2)
    agesReturned mustEqual expectedReturn
    context.run(get).toSet mustEqual (expect.toSet)
  }

  "Ex 5 - Append Data" in {
    System.setProperty("quill.binds.log", "true")
    io.getquill.util.Messages.resetCache()
    import `Ex 5 - Append Data`._
    context.run(insert)
    context.run(update, 2)
    context.run(get).toSet mustEqual (expectSpecific.toSet)
  }

  "Ex 6 - Append Data No Condition" in {
    import `Ex 6 - Append Data No Condition`._
    context.run(insert)
    context.run(update, 2)
    context.run(get).toSet mustEqual (expectSpecific.toSet)
  }
}
