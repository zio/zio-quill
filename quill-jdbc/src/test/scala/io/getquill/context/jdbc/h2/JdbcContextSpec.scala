package io.getquill.context.jdbc.h2

import io.getquill.Spec

class JdbcContextSpec extends Spec {

  import testContext._

  val badEntity = quote {
    querySchema[TestEntity]("TestEntity", _.s -> "a", _.i -> "i", _.l -> "l", _.o -> "o")
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
    "nested" in {
      testContext.run(qr1.delete)
      testContext.transaction {
        testContext.transaction {
          testContext.run(qr1.insert(_.i -> 33))
        }
      }
      testContext.run(qr1).map(_.i) mustEqual List(33)
    }
    "prepare" in {
      testContext.prepareParams(
        "select * from Person where name=? and age > ?", ps => (List("Sarah", 127), ps)
      ) mustEqual List("127", "'Sarah'")
    }
  }

  "Insert with returning with single column table" in {
    val inserted = testContext.run {
      qr4.insert(lift(TestEntity4(0))).returningGenerated(_.i)
    }
    testContext.run(qr4.filter(_.i == lift(inserted))).head.i mustBe inserted
  }
}
