package io.getquill.postgres

import io.getquill.MonixSpec
import monix.eval.Task

import scala.concurrent.duration._
import scala.language.postfixOps

class MonixNdbcContextSpec extends MonixSpec {

  val context = testContext
  import testContext._

  "without transaction" - {
    "does simple insert" in {
      (for {
        _ <- testContext.run(qr1.delete)
        _ <- testContext.run(qr1.insert(_.i -> 33))
        r <- testContext.run(qr1)
      } yield r).runSyncUnsafe(10 seconds).map(_.i) mustEqual List(33)
    }

    "streams empty result set" in {
      (for {
        _ <- testContext.run(qr1.delete)
        s <- accumulate(testContext.stream(qr1))
      } yield s).runSyncUnsafe(10 seconds).map(_.i) mustEqual List()
    }

    "streams single result" in {
      (for {
        _ <- testContext.run(qr1.delete)
        _ <- testContext.run(qr1.insert(_.i -> 666))
        s <- accumulate(testContext.stream(qr1))
      } yield s).runSyncUnsafe(10 seconds).map(_.i) mustEqual List(666)
    }

    "streams multiple results" in {
      (for {
        _ <- testContext.run(qr1.delete)
        _ <- testContext.run(liftQuery(List(1, 2, 3, 4)).foreach(n => qr1.insert(_.i -> n)))
        s <- accumulate(testContext.stream(qr1))
      } yield s).runSyncUnsafe(10 seconds).map(_.i) mustEqual List(4, 3, 2, 1)
      // Caution: NDBC streams result in reverse order compared to JDBC. But users can not expect a specific order anyway.
    }
  }

  "with transaction" - {
    "does simple insert" in {
      (for {
        _ <- testContext.run(qr1.delete)
        _ <- testContext.transaction {
          testContext.run(qr1.insert(_.i -> 33))
        }
        r <- testContext.run(qr1)
      } yield r).runSyncUnsafe(10 seconds).map(_.i) mustEqual List(33)
    }

    /* Ignore because apparently NDBC streaming doesn't work with transactions, as these tests
    show. https://github.com/traneio/ndbc/blob/1f37baf4815a90299842afeb3c710a80b86ef9d6/ndbc-core/src/main/java/io/trane/ndbc/datasource/PooledDataSource.java#L49
     */
    "streams empty result set" ignore {
      (for {
        _ <- testContext.run(qr1.delete)
        s <- testContext.transaction(accumulate(testContext.stream(qr1)))
      } yield s).runSyncUnsafe(10 seconds).map(_.i) mustEqual List()
    }

    /* Ignore because apparently NDBC streaming doesn't work with transactions, as these tests
    show. https://github.com/traneio/ndbc/blob/1f37baf4815a90299842afeb3c710a80b86ef9d6/ndbc-core/src/main/java/io/trane/ndbc/datasource/PooledDataSource.java#L49
     */
    "streams single result" ignore {
      (for {
        _ <- testContext.run(qr1.delete)
        s <- testContext.transaction(for {
          _ <- testContext.run(qr1.insert(_.i -> 33))
          s <- accumulate(testContext.stream(qr1))
        } yield s)
      } yield s).runSyncUnsafe(10 seconds).map(_.i) mustEqual List(33)
    }

    /* Ignore because apparently NDBC streaming doesn't work with transactions, as these tests
    show. https://github.com/traneio/ndbc/blob/1f37baf4815a90299842afeb3c710a80b86ef9d6/ndbc-core/src/main/java/io/trane/ndbc/datasource/PooledDataSource.java#L49
     */
    "streams multiple results" ignore {
      (for {
        _ <- testContext.run(qr1.delete)
        s <- testContext.transaction(for {
          _ <- testContext.run(liftQuery(List(1, 2, 3, 4)).foreach(n => qr1.insert(_.i -> n)))
          s <- accumulate(testContext.stream(qr1))
        } yield s)
      } yield s).runSyncUnsafe(10 seconds).map(_.i) mustEqual List(4, 3, 2, 1)
      // Caution: NDBC streams result in reverse order compared to JDBC. But users can not expect a specific order anyway.
    }

    "reverts when failed" in {
      (for {
        _ <- testContext.run(qr1.delete)
        e <- testContext.transaction {
          Task.sequence(Seq(
            testContext.run(qr1.insert(_.i -> 18)),
            Task.eval {
              throw new IllegalStateException
            }
          ))
        }.onErrorHandleWith {
          case e: Exception => Task(e.getClass.getSimpleName)
        }
        r <- testContext.run(qr1)
      } yield (e, r.isEmpty)).runSyncUnsafe(10 seconds) mustEqual (("IllegalStateException", true))
    }

    "supports nested transactions" in {
      (for {
        _ <- testContext.run(qr1.delete)
        _ <- testContext.transaction {
          testContext.transaction {
            testContext.run(qr1.insert(_.i -> 33))
          }
        }
        r <- testContext.run(qr1)
      } yield r).runSyncUnsafe(10 seconds).map(_.i) mustEqual List(33)
    }

    "prepare" in {
      testContext.prepareParams(
        "select * from Person where name=? and age > ?", ps => (List("Sarah", 127), ps)
      ).runSyncUnsafe() mustEqual List("127", "'Sarah'")
    }
  }
}
