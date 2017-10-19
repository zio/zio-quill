package io.getquill.context.finagle.mysql

import java.util.TimeZone

import com.twitter.finagle.mysql
import com.twitter.finagle.mysql.{ EmptyValue, Error }
import com.twitter.util._
import io.getquill.{ FinagleMysqlContext, _ }

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
      qr4.insert(lift(TestEntity4(0))).returning(_.i)
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

  "different constructors" in {
    new FinagleMysqlContext(Literal, "testDB", TimeZone.getDefault).close
    new FinagleMysqlContext(Literal, "testDB", TimeZone.getDefault, TimeZone.getDefault).close
  }

  "fail in toOk" in {
    object ctx extends FinagleMysqlContext(Literal, "testDB") {
      override def toOk(result: mysql.Result) = super.toOk(result)
    }
    intercept[IllegalStateException](ctx.toOk(Error(-1, "no ok", "test")))
    ctx.close
  }
}
