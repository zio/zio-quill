package io.getquill.context.jdbc.h2

import io.getquill._

class JdbcContextSpec extends Spec {

  import testContext._

  val badEntity = quote {
    query[TestEntity].schema(_.entity("TestEntity").columns(_.s -> "a", _.i -> "i", _.l -> "l", _.o -> "o"))
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
