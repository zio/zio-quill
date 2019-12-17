package io.getquill.context.jdbc.oracle

import io.getquill.Spec

class JdbcContextSpec extends Spec {

  import testContext._

  val badEntity = quote {
    querySchema[TestEntity]("TestEntity", _.s -> "a", _.i -> "i", _.l -> "l", _.o -> "o")
  }

  "probes sqls" in {
    val p = testContext.probe("DELETE FROM TestEntity")
  }

  "run non-batched action" in {
    val insert = quote {
      qr1.insert(_.i -> lift(1))
    }
    testContext.run(insert) mustEqual 1
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

  "insert returning" - {
    "with single column table" in {
      val inserted = testContext.run {
        qr4.insert(lift(TestEntity4(0))).returning(_.i)
      }
      testContext.run(qr4.filter(_.i == lift(inserted))).head.i mustBe inserted
    }

    "with multiple columns" in {
      testContext.run(qr1.delete)
      val inserted = testContext.run {
        qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123)))).returning(r => (r.i, r.s, r.o))
      }
      (1, "foo", Some(123)) mustBe inserted
    }

    "with multiple columns - case class" in {
      case class Return(id: Int, str: String, opt: Option[Int])
      testContext.run(qr1.delete)
      val inserted = testContext.run {
        qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123)))).returning(r => Return(r.i, r.s, r.o))
      }
      Return(1, "foo", Some(123)) mustBe inserted
    }
  }

  "update returning" - {
    "with single column table" in {
      testContext.run(qr4.insert(lift(TestEntity4(8))))

      val updated = testContext.run {
        qr4.update(lift(TestEntity4(0))).returning(_.i)
      }
      testContext.run(qr4.filter(_.i == lift(updated))).head.i mustBe updated
    }

    "with multiple columns" in {
      testContext.run(qr1.delete)
      testContext.run(qr1.insert(lift(TestEntity("baz", 6, 42L, Some(456)))))

      val updated = testContext.run {
        qr1.update(lift(TestEntity("foo", 1, 18L, Some(123)))).returning(r => (r.i, r.s, r.o))
      }
      (1, "foo", Some(123)) mustBe updated
    }

    "with multiple columns - case class" in {
      case class Return(id: Int, str: String, opt: Option[Int])
      testContext.run(qr1.delete)
      testContext.run(qr1.insert(lift(TestEntity("baz", 6, 42L, Some(456)))))

      val updated = testContext.run {
        qr1.update(lift(TestEntity("foo", 1, 18L, Some(123)))).returning(r => Return(r.i, r.s, r.o))
      }
      Return(1, "foo", Some(123)) mustBe updated
    }
  }
}
