package io.getquill.sources.jdbc.mysql

import io.getquill._

class JdbcSourceSpec extends Spec {

  "probes sqls" - {
    val p = testMysqlDB.probe("DELETE FROM TestEntity")
  }

  "provides transaction support" - {
    "success" in {
      testMysqlDB.run(qr1.delete)
      testMysqlDB.transaction {
        testMysqlDB.run(qr1.insert(_.i -> 33))
      }
      testMysqlDB.run(qr1).map(_.i) mustEqual List(33)
    }
    "failure" in {
      testMysqlDB.run(qr1.delete)
      intercept[IllegalStateException] {
        testMysqlDB.transaction {
          testMysqlDB.run(qr1.insert(_.i -> 33))
          throw new IllegalStateException
        }
      }
      testMysqlDB.run(qr1).isEmpty mustEqual true
    }
  }
}
