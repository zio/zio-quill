package io.getquill.context.jasync.mysql

import com.github.jasync.sql.db.{ QueryResult, ResultSetKt }
import io.getquill.ReturnAction.ReturnColumns
import io.getquill.base.Spec
import io.getquill.{ Literal, MysqlJAsyncContext, ReturnAction }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MysqlJAsyncContextSpec extends Spec {

  import testContext._

  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(testContext.run(insert(lift(1)))) mustEqual 1
  }

  "Insert with returning with single column table" in {
    val inserted: Long = await(testContext.run {
      qr4.insertValue(lift(TestEntity4(0))).returningGenerated(_.i)
    })
    await(testContext.run(qr4.filter(_.i == lift(inserted)))).head.i mustBe inserted
  }

  "performIO" in {
    await(performIO(runIO(qr4).transactional))
  }

  "probe" in {
    probe("select 1").toOption mustBe defined
  }

  "cannot extract" in {
    object ctx extends MysqlJAsyncContext(Literal, "testMysqlDB") {
      override def handleSingleResult[T](sql: String, list: List[T]) = super.handleSingleResult(sql, list)

      override def extractActionResult[O](
        returningAction: ReturnAction,
        returningExtractor: ctx.Extractor[O]
      )(result: QueryResult) =
        super.extractActionResult(returningAction, returningExtractor)(result)
    }
    intercept[IllegalStateException] {
      val v = ctx.extractActionResult(ReturnColumns(List("w/e")), (row, session) => 1)(
        new QueryResult(0, "w/e", ResultSetKt.getEMPTY_RESULT_SET)
      )
      ctx.handleSingleResult("<not used>", v)
    }
    ctx.close
  }

  "prepare" in {
    testContext.prepareParams("", (ps, session) => (Nil, ps ++ List("Sarah", 127))) mustEqual List("'Sarah'", "127")
  }

  "provides transaction support" - {
    "success" in {
      await(for {
        _ <- testContext.run(qr4.delete)
        _ <- testContext.transaction { implicit ec =>
          testContext.run(qr4.insert(_.i -> 33))
        }
        r <- testContext.run(qr4)
      } yield r).map(_.i) mustEqual List(33)
    }
    "failure" in {
      await(for {
        _ <- testContext.run(qr4.delete)
        e <- testContext.transaction { implicit ec =>
          Future.sequence(Seq(
            testContext.run(qr4.insert(_.i -> 18)),
            Future(throw new IllegalStateException)
          ))
        }.recoverWith {
          case e: Exception => Future(e.getClass.getSimpleName)
        }
        r <- testContext.run(qr4)
      } yield (e, r.isEmpty)) mustEqual (("CompletionException", true))
    }
    "nested" in {
      await(for {
        _ <- testContext.run(qr4.delete)
        _ <- testContext.transaction { implicit ec =>
          testContext.transaction { implicit ec =>
            testContext.run(qr4.insert(_.i -> 33))
          }
        }
        r <- testContext.run(qr4)
      } yield r).map(_.i) mustEqual List(33)
    }
    "nested transactions use the same connection" in {
      await(for {
        e <- testContext.transaction { implicit ec =>
          val externalConn = ec.conn
          testContext.transaction { implicit ec =>
            Future(externalConn == ec.conn)
          }
        }
      } yield e) mustEqual true
    }
  }

  override protected def beforeAll(): Unit = {
    await(testContext.run(qr1.delete))
    ()
  }
}
