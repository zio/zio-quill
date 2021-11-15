package io.getquill.mysql

import io.getquill.{ Prefix, ZioSpec }
import zio.{ Task, ZIO, ZLayer }
import io.getquill.context.ZioJdbc._

import javax.sql.DataSource

class ZioJdbcContextSpec extends ZioSpec {

  def prefix = Prefix("testMysqlDB")
  val context = testContext
  import testContext._

  "provides transaction support" - {
    "success" in {
      (for {
        _ <- testContext.run(qr1.delete)
        _ <- testContext.transaction { testContext.run(qr1.insert(_.i -> 33)) }
        r <- testContext.run(qr1)
      } yield r).runSyncUnsafe().map(_.i) mustEqual List(33)
    }
    "success - with dependency" in {
      (for {
        _ <- testContext.run(qr1.delete)
        _ <- testContext.transaction {
          for {
            env <- ZIO.service[Int]
            qry <- testContext.run(qr1.insert(_.i -> lift(env)))
          } yield qry
        }
        r <- testContext.run(qr1)
      } yield r).provideSomeLayer(ZLayer.service[DataSource] >>> ZLayer.succeed(33)).runSyncUnsafe().map(_.i) mustEqual List(33)
    }
    "success - stream" in {
      (for {
        _ <- testContext.run(qr1.delete)
        seq <- testContext.transaction {
          for {
            _ <- testContext.run(qr1.insert(_.i -> 33))
            s <- accumulateDS(testContext.stream(qr1))
          } yield s
        }
        r <- testContext.run(qr1)
      } yield (seq.map(_.i), r.map(_.i))).runSyncUnsafe() mustEqual ((List(33), List(33)))
    }
    "failure - nested" in {
      (for {
        _ <- testContext.run(qr1.delete)
        e <- testContext.transaction {
          testContext.run(qr1.insert(_.i -> 36)) *>
            testContext.transaction {
              ZIO.collectAll(Seq(
                testContext.run(qr1.insert(_.i -> 18)),
                Task {
                  throw new IllegalStateException
                }
              ))
            }
        }.catchSome {
          case e: Exception => Task(e.getClass.getSimpleName)
        }
        r <- testContext.run(qr1)
      } yield (e, r.isEmpty)).runSyncUnsafe() mustEqual (("IllegalStateException", true))
    }
    "nested" in {
      (for {
        _ <- testContext.run(qr1.delete)
        _ <- testContext.transaction { testContext.transaction { testContext.run(qr1.insert(_.i -> 33)) } }
        r <- testContext.run(qr1)
      } yield r).runSyncUnsafe().map(_.i) mustEqual List(33)
    }
    "prepare" in {
      testContext.prepareParams(
        "select * from Person where name=? and age > ?", (ps, session) => (List("Sarah", 127), ps)
      ).onDataSource.runSyncUnsafe() mustEqual List("127", "'Sarah'")
    }
  }
}
