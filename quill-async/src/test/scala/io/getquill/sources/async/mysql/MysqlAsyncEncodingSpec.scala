package io.getquill.sources.async.mysql

import io.getquill._
import io.getquill.sources.sql.EncodingSpec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.Date

class MysqlAsyncEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    val r =
      for {
        _ <- testMysqlDB.run(delete)
        _ <- testMysqlDB.run(insert)(insertValues)
        result <- testMysqlDB.run(query[EncodingTestEntity])
      } yield result

    verify(Await.result(r, Duration.Inf).toList)
  }

  "decode numeric types correctly" - {
    "decode byte to" - {
      prepareEncodingTestEntity()
      "short" in {
        case class EncodingTestEntity(v3: Short)
        val v3List = Await.result(testMysqlDB.run(query[EncodingTestEntity]), Duration.Inf)
        v3List.map(_.v3) must contain theSameElementsAs(List(1: Byte, 0: Byte))
      }
      "int" in {
        case class EncodingTestEntity(v3: Int)
        val v3List = Await.result(testMysqlDB.run(query[EncodingTestEntity]), Duration.Inf)
        v3List.map(_.v3) must contain theSameElementsAs(List(1, 0))
      }
      "long" in {
        case class EncodingTestEntity(v3: Long)
        val v3List = Await.result(testMysqlDB.run(query[EncodingTestEntity]), Duration.Inf)
        v3List.map(_.v3) must contain theSameElementsAs(List(1L, 0L))
      }
    }
    "decode short to" - {
      prepareEncodingTestEntity()
      "int" in {
        case class EncodingTestEntity(v5: Int)
        val v5List = Await.result(testMysqlDB.run(query[EncodingTestEntity]), Duration.Inf)
        v5List.map(_.v5) must contain theSameElementsAs(List(23, 0))
      }
      "long" in {
        case class EncodingTestEntity(v5: Long)
        val v5List = Await.result(testMysqlDB.run(query[EncodingTestEntity]), Duration.Inf)
        v5List.map(_.v5) must contain theSameElementsAs(List(23L, 0L))
      }
    }
    "decode int to long" in  {
      case class EncodingTestEntity(v6: Long)
      val v6List = Await.result(testMysqlDB.run(query[EncodingTestEntity]), Duration.Inf)
      v6List.map(_.v6) must contain theSameElementsAs(List(33L, 0L))
    }

    "decode and encode any numeric as boolean" in {
      case class EncodingTestEntity(v3: Boolean, v4: Boolean, v6: Boolean, v7: Boolean)
      Await.result(testMysqlDB.run(query[EncodingTestEntity]), Duration.Inf)
    }
  }

  "decode date types" in {
    case class DateEncodingTestEntity(v1: Date, v2: Date, v3: Date)
    val entity = new DateEncodingTestEntity(new Date, new Date, new Date)
    val delete = quote(query[DateEncodingTestEntity].delete)
    val insert = quote(query[DateEncodingTestEntity].insert)
    val r = for {
      _ <- testMysqlDB.run(delete)
      _ <- testMysqlDB.run(insert)(List(entity))
      result <- testMysqlDB.run(query[DateEncodingTestEntity])
    } yield result
    Await.result(r, Duration.Inf)
  }

  "fails if the column has the wrong type" - {
    "numeric" in {
      Await.result(testMysqlDB.run(insert)(insertValues), Duration.Inf)
      case class EncodingTestEntity(v1: Int)
      val e = intercept[IllegalStateException] {
        Await.result(testMysqlDB.run(query[EncodingTestEntity]), Duration.Inf)
      }
    }
    "non-numeric" in {
      Await.result(testMysqlDB.run(insert)(insertValues), Duration.Inf)
      case class EncodingTestEntity(v1: Date)
      val e = intercept[IllegalStateException] {
        Await.result(testMysqlDB.run(query[EncodingTestEntity]), Duration.Inf)
      }
    }
  }

  private def prepareEncodingTestEntity() = {
    val prepare = for {
      _ <- testMysqlDB.run(delete)
      _ <- testMysqlDB.run(insert)(insertValues)
    } yield {}
    Await.result(prepare, Duration.Inf)
  }
}
