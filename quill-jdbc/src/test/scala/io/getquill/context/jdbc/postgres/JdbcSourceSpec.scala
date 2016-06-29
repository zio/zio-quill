package io.getquill.context.jdbc.postgres

import io.getquill._

class JdbcSourceSpec extends Spec {

  val context = testContext
  import testContext._

  "probes sqls" in {
    val p = testContext.probe("DELETE FROM TestEntity")
  }

  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    testContext.run(insert)(1) mustEqual (1)
  }

  "provides transaction support" - {
    "success" in {
      testContext.run(qr1.delete)
      testContext.transaction {
        testContext.run(qr1.insert(_.i -> 33))
      }
      testContext.run(qr1).map(_.i) mustEqual List(33)
    }
    "failure" in {
      testContext.run(qr1.delete)
      intercept[IllegalStateException] {
        testContext.transaction {
          testContext.run(qr1.insert(_.i -> 33))
          throw new IllegalStateException
        }
      }
      testContext.run(qr1).isEmpty mustEqual true
    }
  }
}
