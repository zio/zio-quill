package io.getquill.sources.finagle.mysql

import com.twitter.util.Await

import io.getquill._
import io.getquill.sources.sql.EncodingSpec

class FinagleMysqlEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    val r =
      for {
        _ <- testDB.run(delete)
        _ <- testDB.run(insert)(insertValues)
        result <- testDB.run(query[EncodingTestEntity])
      } yield result

    verify(Await.result(r).toList)
  }

  "fails if the column has the wrong type" in {
    Await.result(testDB.run(insert)(insertValues))
    case class EncodingTestEntity(v1: Int)
    val e = intercept[IllegalStateException] {
      Await.result(testDB.run(query[EncodingTestEntity]))
    }
  }

  "encodes sets" in {
    val q = quote {
      (set: Set[Int]) =>
        query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    Await.result {
      for {
        _ <- testDB.run(query[EncodingTestEntity].delete)
        _ <- testDB.run(query[EncodingTestEntity].insert)(insertValues)
        r <- testDB.run(q)(insertValues.map(_.v6).toSet)
      } yield {
        verify(r)
      }
    }
  }

  "decode boolean types" - {
    case class BooleanEncodingTestEntity(v1: Boolean, v2: Boolean)
    val decodeBoolean = (entity: BooleanEncodingTestEntity) => {
      val delete = quote(query[BooleanEncodingTestEntity].delete)
      val insert = quote(query[BooleanEncodingTestEntity].insert)
      val r = for {
        _ <- testDB.run(delete)
        _ <- testDB.run(insert)(List(entity))
        result <- testDB.run(query[BooleanEncodingTestEntity])
      } yield result
      Await.result(r).head
    }
    "true" in {
      val entity = BooleanEncodingTestEntity(true, true)
      val r = decodeBoolean(entity)
      r.v1 mustEqual true
      r.v2 mustEqual true
    }

    "false" in {
      val entity = BooleanEncodingTestEntity(false, false)
      val r = decodeBoolean(entity)
      r.v1 mustEqual false
      r.v2 mustEqual false
    }
  }
}
