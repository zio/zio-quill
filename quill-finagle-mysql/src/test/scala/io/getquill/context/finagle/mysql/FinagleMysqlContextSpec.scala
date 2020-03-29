package io.getquill.context.finagle.mysql

import java.util.TimeZone

import com.twitter.finagle.mysql
import com.twitter.finagle.mysql.{ EmptyValue, Error, IsolationLevel }
import com.twitter.util._
import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import io.getquill.{ testContext => _, _ }

class FinagleMysqlContextSpec extends Spec {

  val context = testContext
  import testContext._

  def await[T](f: Future[T]) = Await.result(f)

  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(testContext.run(insert(lift(1)))) mustEqual 1
  }

  "Insert with returning with single column table" in {
    val inserted: Long = await(testContext.run {
      qr4.insert(lift(TestEntity4(0))).returningGenerated(_.i)
    })
    await(testContext.run(qr4.filter(_.i == lift(inserted))))
      .head.i mustBe inserted
  }

  "SingleValueRow" in {
    SingleValueRow(EmptyValue).indexOf("w/e") mustBe None
  }

  "probe" in {
    probe("select 1").toOption mustBe defined
  }

  "performIO" in {
    await(performIO(runIO(qr1.filter(_.s == "w/e")).transactional)) mustBe Nil
  }

  "transactionWithIsolation" in {
    await(testContext.transactionWithIsolation(IsolationLevel.ReadCommitted) {
      testContext.run(qr1.filter(_.s == "w/e"))
    }) mustBe Nil
  }

  "different constructors" in {
    new FinagleMysqlContext(Literal, "testDB", TimeZone.getDefault).close
    new FinagleMysqlContext(Literal, "testDB", TimeZone.getDefault, TimeZone.getDefault).close
  }

  def masterSlaveContext(
    master: mysql.Client with mysql.Transactions,
    slave:  mysql.Client with mysql.Transactions
  ): FinagleMysqlContext[Literal] = {
    new FinagleMysqlContext[Literal](Literal, master, slave, TimeZone.getDefault) with TestEntities with TestEncoders with TestDecoders
  }

  "master & slave client writes to master" in {
    val master = new OkTestClient
    val slave = new OkTestClient
    val context = masterSlaveContext(master, slave)

    import context._
    await(context.run(query[TestEntity4].insert(TestEntity4(0))))

    master.methodCount.get() mustBe 1
    slave.methodCount.get() mustBe 0
  }

  "master & slave client reads from slave" in {
    val master = new OkTestClient
    val slave = new OkTestClient
    val context = masterSlaveContext(master, slave)

    import context._
    await(context.run(query[TestEntity4]))

    master.methodCount.get() mustBe 0
    slave.methodCount.get() mustBe 1
  }

  "fail in toOk" in {
    object ctx extends FinagleMysqlContext(Literal, "testDB") {
      override def toOk(result: mysql.Result) = super.toOk(result)
    }
    intercept[IllegalStateException](ctx.toOk(Error(-1, "no ok", "test")))
    ctx.close
  }

  "prepare" in {
    import com.twitter.finagle.mysql.Parameter

    testContext.prepareParams(
      "", ps => (Nil, ps ++: List(Parameter.of("Sarah"), Parameter.of(127)))
    ) mustEqual List("'Sarah'", "127")
  }

  override protected def beforeAll(): Unit = {
    await(testContext.run(qr1.delete))
    ()
  }
}
