package io.getquill.context.jdbc.postgres

import io.getquill.Spec

class JdbcContextSpec extends Spec {

  val ctx = testContext
  import ctx._

  "probes sqls" in {
    val p = ctx.probe("DELETE FROM TestEntity")
  }

  "run non-batched action" in {
    val insert = quote {
      qr1.insert(_.i -> lift(1))
    }
    ctx.run(insert) mustEqual 1
  }

  "provides transaction support" - {
    "success" in {
      ctx.run(qr1.delete)
      ctx.transaction {
        ctx.run(qr1.insert(_.i -> 33))
      }
      ctx.run(qr1).map(_.i) mustEqual List(33)
    }
    "failure" in {
      ctx.run(qr1.delete)
      intercept[IllegalStateException] {
        ctx.transaction {
          ctx.run(qr1.insert(_.i -> 33))
          throw new IllegalStateException
        }
      }
      ctx.run(qr1).isEmpty mustEqual true
    }
    "nested" in {
      ctx.run(qr1.delete)
      ctx.transaction {
        ctx.transaction {
          ctx.run(qr1.insert(_.i -> 33))
        }
      }
      ctx.run(qr1).map(_.i) mustEqual List(33)
    }
    "prepare" in {
      ctx.prepareParams(
        "select * from Person where name=? and age > ?", ps => (List("Sarah", 127), ps)
      ) mustEqual List("127", "'Sarah'")
    }
  }

  "Insert with returning generated with single column table" in {
    ctx.run(qr4.delete)
    val insert = quote {
      qr4.insert(lift(TestEntity4(0))).returningGenerated(_.i)
    }

    val inserted1 = ctx.run(insert)
    ctx.run(qr4.filter(_.i == lift(inserted1))).head.i mustBe inserted1

    val inserted2 = ctx.run(insert)
    ctx.run(qr4.filter(_.i == lift(inserted2))).head.i mustBe inserted2

    val inserted3 = ctx.run(insert)
    ctx.run(qr4.filter(_.i == lift(inserted3))).head.i mustBe inserted3
  }

  "Insert with returning generated with single column table using query" in {
    ctx.run(qr5.delete)
    val id = ctx.run(qr5.insert(lift(TestEntity5(0, "foo"))).returningGenerated(_.i))
    val id2 = ctx.run {
      qr5.insert(_.s -> "bar").returningGenerated(r => query[TestEntity5].filter(_.s == "foo").map(_.i).max)
    }.get
    id mustBe id2
  }

  "insert returning" - {
    "with multiple columns" in {
      ctx.run(qr1.delete)
      val inserted = ctx.run {
        qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123), true))).returning(r => (r.i, r.s, r.o))
      }
      (1, "foo", Some(123)) mustBe inserted
    }

    "with multiple columns and operations" in {
      ctx.run(qr1.delete)
      val inserted = ctx.run {
        qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123), true))).returning(r => (r.i + 100, r.s, r.o.map(_ + 100)))
      }
      (1 + 100, "foo", Some(123 + 100)) mustBe inserted
    }

    "with multiple columns and query" in {
      ctx.run(qr2.delete)
      ctx.run(qr2.insert(_.i -> 36, _.l -> 0L, _.s -> "foobar"))

      ctx.run(qr1.delete)
      val inserted = ctx.run {
        qr1.insert(lift(TestEntity("two", 36, 18L, Some(123), true))).returning(r =>
          (r.i, r.s + "_s", qr2.filter(rr => rr.i == r.i).map(_.s).max))
      }
      (36, "two_s", Some("foobar")) mustBe inserted
    }

    "with multiple columns and query - with lifting" in {
      ctx.run(qr2.delete)
      ctx.run(qr2.insert(_.i -> 36, _.l -> 0L, _.s -> "foobar"))

      val value = "foobar"
      ctx.run(qr1.delete)
      val inserted = ctx.run {
        qr1.insert(lift(TestEntity("two", 36, 18L, Some(123), true))).returning(r =>
          (r.i, r.s + "_s", qr2.filter(rr => rr.i == r.i && rr.s == lift(value)).map(_.s).max))
      }
      (36, "two_s", Some("foobar")) mustBe inserted
    }

    "with multiple columns and query - same table" in {
      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("one", 1, 18L, Some(1), true))))
      val inserted = ctx.run {
        qr1.insert(lift(TestEntity("two", 2, 18L, Some(123), true))).returning(r =>
          (r.i, r.s + "_s", qr1.filter(rr => rr.o.exists(_ == r.i - 1)).map(_.s).max))
      }
      (2, "two_s", Some("one")) mustBe inserted
    }

    "with multiple columns and query embedded" in {
      ctx.run(qr1Emb.delete)
      ctx.run(qr1Emb.insert(lift(TestEntityEmb(Emb("one", 1), 18L, Some(123)))))
      val inserted = ctx.run {
        qr1Emb.insert(lift(TestEntityEmb(Emb("two", 2), 18L, Some(123)))).returning(r =>
          (r.emb.i, r.o))
      }
      (2, Some(123)) mustBe inserted
    }

    "with multiple columns - case class" in {
      case class Return(id: Int, str: String, opt: Option[Int])
      ctx.run(qr1.delete)
      val inserted = ctx.run {
        qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123), true))).returning(r => Return(r.i, r.s, r.o))
      }
      Return(1, "foo", Some(123)) mustBe inserted
    }
  }

  "update returning" - {
    "with multiple columns" in {
      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123), true))))

      val updated = ctx.run {
        qr1.update(lift(TestEntity("bar", 2, 42L, Some(321), true))).returning(r => (r.i, r.s, r.o))
      }
      (2, "bar", Some(321)) mustBe updated
    }

    "with multiple columns and operations" in {
      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123), true))))

      val updated = ctx.run {
        qr1.update(lift(TestEntity("bar", 2, 42L, Some(321), true))).returning(r => (r.i + 100, r.s, r.o.map(_ + 100)))
      }
      (2 + 100, "bar", Some(321 + 100)) mustBe updated
    }

    "with multiple columns and query" in {
      ctx.run(qr2.delete)
      ctx.run(qr2.insert(_.i -> 36, _.l -> 0L, _.s -> "foobar"))

      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123), true))))

      val updated = ctx.run {
        qr1.update(lift(TestEntity("bar", 36, 42L, Some(321), true))).returning(r =>
          (r.i, r.s + "_s", qr2.filter(rr => rr.i == r.i).map(_.s).max))
      }
      (36, "bar_s", Some("foobar")) mustBe updated
    }

    "with multiple columns and query - with lifting" in {
      ctx.run(qr2.delete)
      ctx.run(qr2.insert(_.i -> 36, _.l -> 0L, _.s -> "foobar"))

      val value = "foobar"
      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123), true))))

      val updated = ctx.run {
        qr1.update(lift(TestEntity("bar", 36, 42L, Some(321), true))).returning(r =>
          (r.i, r.s + "_s", qr2.filter(rr => rr.i == r.i && rr.s == lift(value)).map(_.s).max))
      }
      (36, "bar_s", Some("foobar")) mustBe updated
    }

    "with multiple columns and query - same table" in {
      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("one", 1, 18L, Some(1), true))))

      val updated = ctx.run {
        qr1.update(lift(TestEntity("two", 2, 18L, Some(123), true))).returning(r =>
          (r.i, r.s + "_s", qr1.filter(rr => rr.o.exists(_ == r.i - 1)).map(_.s).max))
      }
      (2, "two_s", Some("one")) mustBe updated
    }

    "with multiple columns and query embedded" in {
      ctx.run(qr1Emb.delete)
      ctx.run(qr1Emb.insert(lift(TestEntityEmb(Emb("one", 1), 18L, Some(123)))))

      val updated = ctx.run {
        qr1Emb.update(lift(TestEntityEmb(Emb("two", 2), 18L, Some(123)))).returning(r => (r.emb.i, r.o))
      }
      (2, Some(123)) mustBe updated
    }

    "with multiple columns - case class" in {
      case class Return(id: Int, str: String, opt: Option[Int])
      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("one", 1, 18L, Some(1), true))))

      val updated = ctx.run {
        qr1.update(lift(TestEntity("foo", 1, 18L, Some(123), true))).returning(r => Return(r.i, r.s, r.o))
      }
      Return(1, "foo", Some(123)) mustBe updated
    }
  }

  "delete returning" - {
    "with multiple columns" in {
      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123), true))))

      val deleted = ctx.run {
        qr1.delete.returning(r => (r.i, r.s, r.o))
      }
      (1, "foo", Some(123)) mustBe deleted
    }

    "with multiple columns and operations" in {
      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123), true))))

      val deleted = ctx.run {
        qr1.delete.returning(r => (r.i + 100, r.s, r.o.map(_ + 100)))
      }
      (1 + 100, "foo", Some(123 + 100)) mustBe deleted
    }

    "with multiple columns and query" in {
      ctx.run(qr2.delete)
      ctx.run(qr2.insert(_.i -> 1, _.l -> 0L, _.s -> "foobar"))

      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123), true))))

      val deleted = ctx.run {
        qr1.delete.returning(r =>
          (r.i, r.s + "_s", qr2.filter(rr => rr.i == r.i).map(_.s).max))
      }
      (1, "foo_s", Some("foobar")) mustBe deleted
    }

    "with multiple columns and query - with lifting" in {
      ctx.run(qr2.delete)
      ctx.run(qr2.insert(_.i -> 1, _.l -> 0L, _.s -> "foobar"))

      val value = "foobar"
      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123), true))))

      val deleted = ctx.run {
        qr1.delete.returning(r =>
          (r.i, r.s + "_s", qr2.filter(rr => rr.i == r.i && rr.s == lift(value)).map(_.s).max))
      }
      (1, "foo_s", Some("foobar")) mustBe deleted
    }

    "with multiple columns and query - same table" in {
      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("one", 2, 18L, Some(1), true))))

      val deleted = ctx.run {
        qr1.delete.returning(r =>
          (r.i, r.s + "_s", qr1.filter(rr => rr.o.exists(_ == r.i - 1)).map(_.s).max))
      }
      (2, "one_s", Some("one")) mustBe deleted
    }

    "with multiple columns and query embedded" in {
      ctx.run(qr1Emb.delete)
      ctx.run(qr1Emb.insert(lift(TestEntityEmb(Emb("one", 1), 18L, Some(123)))))

      val deleted = ctx.run {
        qr1Emb.delete.returning(r => (r.emb.i, r.o))
      }
      (1, Some(123)) mustBe deleted
    }

    "with multiple columns - case class" in {
      case class Return(id: Int, str: String, opt: Option[Int])
      ctx.run(qr1.delete)
      ctx.run(qr1.insert(lift(TestEntity("one", 1, 18L, Some(123), true))))

      val deleted = ctx.run {
        qr1.delete.returning(r => Return(r.i, r.s, r.o))
      }
      Return(1, "one", Some(123)) mustBe deleted
    }
  }
}
