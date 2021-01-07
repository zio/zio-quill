package io.getquill.context

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.testContext
import io.getquill.testContext._

class QueryMacroSpec extends Spec {

  "runs query with nested optional case class" in {
    val q = quote {
      qr1.leftJoin(qr2).on((a, b) => a.i == b.i)
    }
    testContext.run(q)
  }

  "translate query with nested optional case class" in {
    val q = quote {
      qr1.leftJoin(qr2).on((a, b) => a.i == b.i)
    }
    testContext.translate(q)
  }

  "runs query without liftings" in {
    val q = quote {
      qr1.map(t => t.i)
    }
    testContext.run(q).string mustEqual
      """querySchema("TestEntity").map(t => t.i)"""
  }

  "translate query without liftings" in {
    val q = quote {
      qr1.map(t => t.i)
    }
    testContext.translate(q) mustEqual
      """querySchema("TestEntity").map(t => t.i)"""
  }

  "runs query with liftings" - {
    "static" - {
      "one" in {
        val q = quote {
          qr1.filter(t => t.i == lift(1)).map(t => t.i)
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").filter(t => t.i == ?).map(t => t.i)"""
        r.prepareRow mustEqual Row(1)
      }
      "two" in {
        val q = quote {
          qr1.filter(t => t.i == lift(1) && t.s == lift("a")).map(t => t.i)
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").filter(t => (t.i == ?) && (t.s == ?)).map(t => t.i)"""
        r.prepareRow mustEqual Row(1, "a")
      }
      "nested" in {
        val c = quote {
          (t: TestEntity) => t.i == lift(1)
        }
        val q = quote {
          qr1.filter(t => c(t) && t.s == lift("a")).map(t => t.i)
        }
        val r = testContext.run(q)
        r.string mustEqual """querySchema("TestEntity").filter(t => (t.i == ?) && (t.s == ?)).map(t => t.i)"""
        r.prepareRow mustEqual Row(1, "a")
      }
    }
    "dynamic" - {
      "one" in {
        val q = quote {
          qr1.filter(t => t.i == lift(1)).map(t => t.i)
        }
        val r = testContext.run(q.dynamic)
        r.string mustEqual """querySchema("TestEntity").filter(t => t.i == ?).map(t => t.i)"""
        r.prepareRow mustEqual Row(1)
      }
      "two" in {
        val q = quote {
          qr1.filter(t => t.i == lift(1) && t.s == lift("a")).map(t => t.i)
        }
        val r = testContext.run(q.dynamic)
        r.string mustEqual """querySchema("TestEntity").filter(t => (t.i == ?) && (t.s == ?)).map(t => t.i)"""
        r.prepareRow mustEqual Row(1, "a")
      }
      "nested" in {
        val c = quote {
          (t: TestEntity) => t.i == lift(1)
        }
        val q = quote {
          qr1.filter(t => c(t) && t.s == lift("a")).map(t => t.i)
        }
        val r = testContext.run(q.dynamic)
        r.string mustEqual """querySchema("TestEntity").filter(t => (t.i == ?) && (t.s == ?)).map(t => t.i)"""
        r.prepareRow mustEqual Row(1, "a")
      }
    }
  }

  "translate query with liftings" - {
    "static" - {
      "one" in {
        val q = quote {
          qr1.filter(t => t.i == lift(1)).map(t => t.i)
        }
        testContext.translate(q) mustEqual
          """querySchema("TestEntity").filter(t => t.i == 1).map(t => t.i)"""
      }
      "two" in {
        val q = quote {
          qr1.filter(t => t.i == lift(1) && t.s == lift("a")).map(t => t.i)
        }
        testContext.translate(q) mustEqual
          """querySchema("TestEntity").filter(t => (t.i == 1) && (t.s == 'a')).map(t => t.i)"""
      }
      "nested" in {
        val c = quote {
          (t: TestEntity) => t.i == lift(1)
        }
        val q = quote {
          qr1.filter(t => c(t) && t.s == lift("a")).map(t => t.i)
        }
        testContext.translate(q) mustEqual
          """querySchema("TestEntity").filter(t => (t.i == 1) && (t.s == 'a')).map(t => t.i)"""
      }
    }
    "dynamic" - {
      "one" in {
        val q = quote {
          qr1.filter(t => t.i == lift(1)).map(t => t.i)
        }
        testContext.translate(q.dynamic) mustEqual
          """querySchema("TestEntity").filter(t => t.i == 1).map(t => t.i)"""
      }
      "two" in {
        val q = quote {
          qr1.filter(t => t.i == lift(1) && t.s == lift("a")).map(t => t.i)
        }
        val r = testContext.translate(q.dynamic) mustEqual
          """querySchema("TestEntity").filter(t => (t.i == 1) && (t.s == 'a')).map(t => t.i)"""
      }
      "nested" in {
        val c = quote {
          (t: TestEntity) => t.i == lift(1)
        }
        val q = quote {
          qr1.filter(t => c(t) && t.s == lift("a")).map(t => t.i)
        }
        testContext.translate(q.dynamic) mustEqual
          """querySchema("TestEntity").filter(t => (t.i == 1) && (t.s == 'a')).map(t => t.i)"""
      }
    }
  }
}
