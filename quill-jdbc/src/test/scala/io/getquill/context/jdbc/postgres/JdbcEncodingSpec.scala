package io.getquill.context.jdbc.postgres

import java.time.{ LocalDateTime, OffsetDateTime, ZoneId }
import io.getquill.context.sql.EncodingSpec
import io.getquill.Query

class JdbcEncodingSpec extends EncodingSpec {

  val context = testContext
  import testContext._

  "encodes and decodes types" in {
    testContext.run(delete)
    testContext.run(liftQuery(insertValues).foreach(e => insert(e)))
    verify(testContext.run(query[EncodingTestEntity]))
  }

  "encodes sets" in {
    testContext.run(query[EncodingTestEntity].delete)
    testContext.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
    val q = quote {
      (set: Query[Int]) =>
        query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    verify(testContext.run(q(liftQuery(insertValues.map(_.v6)))))
  }

  "returning custom type" in {
    val uuid = testContext.run(insertBarCode(lift(barCodeEntry))).get
    val (barCode :: Nil) = testContext.run(findBarCodeByUuid(uuid))

    verifyBarcode(barCode)
  }

  "LocalDateTime" in {
    case class EncodingTestEntity(v11: Option[LocalDateTime])
    val now = LocalDateTime.now()
    val e1 = EncodingTestEntity(Some(now))
    val e2 = EncodingTestEntity(None)
    val res: (List[EncodingTestEntity], List[EncodingTestEntity]) = performIO {
      val steps = for {
        _ <- testContext.runIO(query[EncodingTestEntity].delete)
        _ <- testContext.runIO(query[EncodingTestEntity].insert(lift(e1)))
        withoutNull <- testContext.runIO(query[EncodingTestEntity])
        _ <- testContext.runIO(query[EncodingTestEntity].delete)
        _ <- testContext.runIO(query[EncodingTestEntity].insert(lift(e2)))
        withNull <- testContext.runIO(query[EncodingTestEntity])
      } yield (withoutNull, withNull)
      steps
    }
    res._1 must contain theSameElementsAs List(EncodingTestEntity(Some(now)))
    res._2 must contain theSameElementsAs List(EncodingTestEntity(None))
  }

  "OffsetDateTime in UTC" in {
    case class EncodingTestEntity(v15: Option[OffsetDateTime])
    val dateTime = OffsetDateTime.parse("2021-08-03T18:20:00.951956Z")
    val e1 = EncodingTestEntity(Some(dateTime))
    val e2 = EncodingTestEntity(None)
    val res: (List[EncodingTestEntity], List[EncodingTestEntity]) = performIO {
      val steps = for {
        _ <- testContext.runIO(query[EncodingTestEntity].delete)
        _ <- testContext.runIO(query[EncodingTestEntity].insert(lift(e1)))
        withoutNull <- testContext.runIO(query[EncodingTestEntity])
        _ <- testContext.runIO(query[EncodingTestEntity].delete)
        _ <- testContext.runIO(query[EncodingTestEntity].insert(lift(e2)))
        withNull <- testContext.runIO(query[EncodingTestEntity])
      } yield (withoutNull, withNull)
      steps
    }
    res._1 must contain theSameElementsAs List(EncodingTestEntity(Some(dateTime)))
    res._2 must contain theSameElementsAs List(EncodingTestEntity(None))
  }

  "OffsetDateTime in IST" in {
    case class EncodingTestEntity(v15: Option[OffsetDateTime])
    val dateTime = OffsetDateTime.parse("2021-08-03T23:50:00.951956+05:30")
    val e1 = EncodingTestEntity(Some(dateTime))
    val e2 = EncodingTestEntity(None)
    val res: (List[EncodingTestEntity], List[EncodingTestEntity]) = performIO {
      val steps = for {
        _ <- testContext.runIO(query[EncodingTestEntity].delete)
        _ <- testContext.runIO(query[EncodingTestEntity].insert(lift(e1)))
        withoutNull <- testContext.runIO(query[EncodingTestEntity])
        _ <- testContext.runIO(query[EncodingTestEntity].delete)
        _ <- testContext.runIO(query[EncodingTestEntity].insert(lift(e2)))
        withNull <- testContext.runIO(query[EncodingTestEntity])
      } yield (withoutNull, withNull)
      steps
    }
    // as specified in PostgreSQL JDBC docs: https://jdbc.postgresql.org/documentation/head/java8-date-time.html
    val expectedTimestamp = dateTime.atZoneSameInstant(ZoneId.of("Z")).toOffsetDateTime
    res._1 must contain theSameElementsAs List(EncodingTestEntity(Some(expectedTimestamp)))
    res._2 must contain theSameElementsAs List(EncodingTestEntity(None))
  }
}
