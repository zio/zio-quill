package io.getquill.context.async.mysql

import io.getquill.context.sql.EncodingSpec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.Date

class MysqlAsyncEncodingSpec extends EncodingSpec {

  val context = testContext
  import testContext._

  "encodes and decodes types" in {
    val r =
      for {
        _ <- testContext.run(delete)
        _ <- testContext.run(insert)(insertValues)
        result <- testContext.run(query[EncodingTestEntity])
      } yield result

    verify(Await.result(r, Duration.Inf).toList)
  }

  "decode numeric types correctly" - {
    "decode byte to" - {
      "short" in {
        prepareEncodingTestEntity()
        case class EncodingTestEntity(v3: Short)
        val v3List = Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
        v3List.map(_.v3) must contain theSameElementsAs (List(1: Byte, 0: Byte))
      }
      "int" in {
        prepareEncodingTestEntity()
        case class EncodingTestEntity(v3: Int)
        val v3List = Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
        v3List.map(_.v3) must contain theSameElementsAs (List(1, 0))
      }
      "long" in {
        prepareEncodingTestEntity()
        case class EncodingTestEntity(v3: Long)
        val v3List = Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
        v3List.map(_.v3) must contain theSameElementsAs (List(1L, 0L))
      }
    }
    "decode short to" - {
      "int" in {
        prepareEncodingTestEntity()
        case class EncodingTestEntity(v5: Int)
        val v5List = Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
        v5List.map(_.v5) must contain theSameElementsAs (List(23, 0))
      }
      "long" in {
        prepareEncodingTestEntity()
        case class EncodingTestEntity(v5: Long)
        val v5List = Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
        v5List.map(_.v5) must contain theSameElementsAs (List(23L, 0L))
      }
    }
    "decode int to long" in {
      case class EncodingTestEntity(v6: Long)
      val v6List = Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
      v6List.map(_.v6) must contain theSameElementsAs (List(33L, 0L))
    }

    "decode and encode any numeric as boolean" in {
      case class EncodingTestEntity(v3: Boolean, v4: Boolean, v6: Boolean, v7: Boolean)
      Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
      ()
    }
  }

  "decode date types" in {
    case class DateEncodingTestEntity(v1: Date, v2: Date, v3: Date)
    val entity = new DateEncodingTestEntity(new Date, new Date, new Date)
    val delete = quote(query[DateEncodingTestEntity].delete)
    val insert = quote(query[DateEncodingTestEntity].insert)
    val r = for {
      _ <- testContext.run(delete)
      _ <- testContext.run(insert)(List(entity))
      result <- testContext.run(query[DateEncodingTestEntity])
    } yield result
    Await.result(r, Duration.Inf)
    ()
  }

  "fails if the column has the wrong type" - {
    "numeric" in {
      Await.result(testContext.run(insert)(insertValues), Duration.Inf)
      case class EncodingTestEntity(v1: Int)
      val e = intercept[IllegalStateException] {
        Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
      }
    }
    "non-numeric" in {
      Await.result(testContext.run(insert)(insertValues), Duration.Inf)
      case class EncodingTestEntity(v1: Date)
      val e = intercept[IllegalStateException] {
        Await.result(testContext.run(query[EncodingTestEntity]), Duration.Inf)
      }
    }
  }

  "encodes sets" in {
    val q = quote {
      (set: Set[Int]) =>
        query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    val fut =
      for {
        _ <- testContext.run(query[EncodingTestEntity].delete)
        _ <- testContext.run(query[EncodingTestEntity].insert)(insertValues)
        r <- testContext.run(q)(insertValues.map(_.v6).toSet)
      } yield {
        r
      }
    verify(Await.result(fut, Duration.Inf))
  }

  private def prepareEncodingTestEntity() = {
    val prepare = for {
      _ <- testContext.run(delete)
      _ <- testContext.run(insert)(insertValues)
    } yield {}
    Await.result(prepare, Duration.Inf)
  }
}
