package io.getquill.context.sql

import io.getquill.Spec
import io.getquill.context.sql.testContext._

class SqlStaticReductionSpec extends Spec {

  "trivial conditionals must" - {
    val q = quote {
      (value: String) =>
        if (value == "foo") qr1.filter(r => r.i == 1)
        else qr1.filter(r => r.s == "blah")
    }

    "reduce to 'the' clause when true" in {
      testContext.run(q("foo")).string mustEqual testContext.run(qr1.filter(r => r.i == 1)).string
    }

    "reduce to 'else' clause when false" in {
      testContext.run(q("bar")).string mustEqual testContext.run(qr1.filter(r => r.s == "blah")).string
    }

    // TODO Test some conditionals
  }
}
