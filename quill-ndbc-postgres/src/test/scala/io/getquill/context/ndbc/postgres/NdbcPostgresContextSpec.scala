package io.getquill.context.ndbc.postgres

import io.getquill.Spec
import io.trane.future.scala.Future

class NdbcPostgresContextSpec extends Spec {

  val ctx = testContext
  import ctx._

  "run non-batched action" in {
    get(ctx.run(qr1.delete))
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    get(ctx.run(insert(lift(1)))) mustEqual 1
  }

  "insert with returning" - {
    "single column table" in {
      val inserted: Long = get(ctx.run {
        qr4.insert(lift(TestEntity4(0))).returningGenerated(_.i)
      })
      get(ctx.run(qr4.filter(_.i == lift(inserted)))).head.i mustBe inserted
    }

    "multiple columns" in {
      get(ctx.run(qr1.delete))
      val inserted = get(ctx.run {
        qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123)))).returning(r => (r.i, r.s, r.o))
      })
      (1, "foo", Some(123)) mustBe inserted
    }
  }

  "transaction support" - {
    "success" in {
      get(for {
        _ <- ctx.run(qr1.delete)
        _ <- ctx.transaction {
          ctx.run(qr1.insert(_.i -> 33))
        }
        r <- ctx.run(qr1)
      } yield r).map(_.i) mustEqual List(33)
    }

    "failure" in {
      get(for {
        _ <- ctx.run(qr1.delete)
        e <- ctx.transaction {
          Future.sequence(Seq(
            ctx.run(qr1.insert(_.i -> 19)),
            Future(throw new IllegalStateException)
          ))
        }.recoverWith {
          case e: Exception => Future(e.getClass.getSimpleName)
        }
        r <- ctx.run(qr1)
      } yield (e, r.isEmpty)) mustEqual (("IllegalStateException", true))
    }

    "nested" in {
      get(for {
        _ <- ctx.run(qr1.delete)
        _ <- ctx.transaction {
          ctx.transaction {
            ctx.run(qr1.insert(_.i -> 33))
          }
        }
        r <- ctx.run(qr1)
      } yield r).map(_.i) mustEqual List(33)
    }
  }
}