package io.getquill.sources.jdbc.h2

import io.getquill._

class JdbcSourceSpec extends SourceSpec(testH2DB) {

  import testH2DB._

  val badEntity = quote {
    query[TestEntity].schema(_.entity("TestEntity").columns(_.s -> "a", _.i -> "i", _.l -> "l", _.o -> "o"))
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
