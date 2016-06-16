package io.getquill.sources.jdbc.postgres

import io.getquill._

class JdbcSourceSpec extends Spec {

  "probes sqls" - {
    val p = testPostgresDB.probe("DELETE FROM TestEntity")
  }

  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    testPostgresDB.run(insert)(1) mustEqual (1)
  }

  "provides transaction support" - {
    "success" in {
      testPostgresDB.run(qr1.delete)
      testPostgresDB.transaction {
        testPostgresDB.run(qr1.insert(_.i -> 33))
      }
      testPostgresDB.run(qr1).map(_.i) mustEqual List(33)
    }
    "failure" in {
      testPostgresDB.run(qr1.delete)
      intercept[IllegalStateException] {
        testPostgresDB.transaction {
          testPostgresDB.run(qr1.insert(_.i -> 33))
          throw new IllegalStateException
        }
      }
      testPostgresDB.run(qr1).isEmpty mustEqual true
    }
  }
}
