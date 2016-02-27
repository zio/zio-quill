package io.getquill.sources.jdbc.h2

import io.getquill._

class JdbcSourceSpec extends Spec {

  val badEntity = quote {
    query[TestEntity]("TestEntity", _.s -> "a", _.i -> "i", _.l -> "l", _.o -> "o")
  }

  "probes valid sqls" - {
    """testH2DBWithQueryProbing.run(qr1)""" must compile
  }

  "probes invalid sqls" - {
    """testH2DBWithQueryProbing.run(badEntity.map(_.s))""" mustNot compile
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
