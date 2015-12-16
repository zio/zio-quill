package io.getquill.source.jdbc.h2

import io.getquill._

class JdbcSourceSpec extends Spec {

  "probes sqls" - {
    val p = testH2DB.probe("DELETE FROM TestEntity")
  }

  "provides transaction support" - {
    "success" in {
      testH2DB.run(qr1.delete)
      testH2DB.transaction {
        testH2DB.run(qr1.insert(_.i -> 33))
      }
      testH2DB.run(qr1).map(_.i) mustEqual List(33)
    }
    "failure" in {
      testH2DB.run(qr1.delete)
      intercept[IllegalStateException] {
        testH2DB.transaction {
          testH2DB.run(qr1.insert(_.i -> 33))
          throw new IllegalStateException
        }
      }
      testH2DB.run(qr1).isEmpty mustEqual true
    }
  }
}
