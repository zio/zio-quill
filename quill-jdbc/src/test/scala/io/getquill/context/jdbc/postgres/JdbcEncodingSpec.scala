package io.getquill.context.jdbc.postgres

import java.time._
import io.getquill.context.sql.EncodingSpec
import io.getquill.Query

import java.time.temporal.ChronoUnit

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
    testContext.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
    val q = quote { (set: Query[Int]) =>
      query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    verify(testContext.run(q(liftQuery(insertValues.map(_.v6)))))
  }

  "returning custom type" in {
    val uuid             = testContext.run(insertBarCode(lift(barCodeEntry))).get
    val (barCode :: Nil) = testContext.run(findBarCodeByUuid(uuid))

    verifyBarcode(barCode)
  }

  "LocalDateTime" in {
    case class EncodingTestEntity(v11: Option[LocalDateTime])
    val now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS) // See https://stackoverflow.com/a/74781779/2431728
    val e1  = EncodingTestEntity(Some(now))
    val e2  = EncodingTestEntity(None)
    val res: (List[EncodingTestEntity], List[EncodingTestEntity]) = performIO {
      val steps = for {
        _           <- testContext.runIO(query[EncodingTestEntity].delete)
        _           <- testContext.runIO(query[EncodingTestEntity].insertValue(lift(e1)))
        withoutNull <- testContext.runIO(query[EncodingTestEntity])
        _           <- testContext.runIO(query[EncodingTestEntity].delete)
        _           <- testContext.runIO(query[EncodingTestEntity].insertValue(lift(e2)))
        withNull    <- testContext.runIO(query[EncodingTestEntity])
      } yield (withoutNull, withNull)
      steps
    }
    res._1 must contain theSameElementsAs List(EncodingTestEntity(Some(now)))
    res._2 must contain theSameElementsAs List(EncodingTestEntity(None))
  }

  "Encode/Decode Other Time Types" in {
    context.run(query[TimeEntity].delete)
    val zid        = ZoneId.systemDefault()
    val timeEntity = TimeEntity.make(zid)
    context.run(query[TimeEntity].insertValue(lift(timeEntity)))
    val actual = context.run(query[TimeEntity]).head
    timeEntity mustEqual actual
  }

  "Encode/Decode Other Time Types ordering" in {
    context.run(query[TimeEntity].delete)

    val zid         = ZoneId.systemDefault()
    val timeEntityA = TimeEntity.make(zid, TimeEntity.TimeEntityInput(2022, 1, 1, 1, 1, 1, 0))
    val timeEntityB = TimeEntity.make(zid, TimeEntity.TimeEntityInput(2022, 2, 2, 2, 2, 2, 0))

    // Importing extras messes around with the quto-quote, need to look into why
    import context.extras._
    context.run(quote(query[TimeEntity].insertValue(lift(timeEntityA))))
    context.run(quote(query[TimeEntity].insertValue(lift(timeEntityB))))

    assert(timeEntityB.sqlDate > timeEntityA.sqlDate)
    assert(timeEntityB.sqlTime > timeEntityA.sqlTime)
    assert(timeEntityB.sqlTimestamp > timeEntityA.sqlTimestamp)
    assert(timeEntityB.timeLocalDate > timeEntityA.timeLocalDate)
    assert(timeEntityB.timeLocalTime > timeEntityA.timeLocalTime)
    assert(timeEntityB.timeLocalDateTime > timeEntityA.timeLocalDateTime)
    assert(timeEntityB.timeZonedDateTime > timeEntityA.timeZonedDateTime)
    assert(timeEntityB.timeInstant > timeEntityA.timeInstant)
    assert(timeEntityB.timeOffsetTime > timeEntityA.timeOffsetTime)
    assert(timeEntityB.timeOffsetDateTime > timeEntityA.timeOffsetDateTime)

    val actual =
      context
        .run(
          query[TimeEntity].filter(t =>
            t.sqlDate > lift(timeEntityA.sqlDate) &&
              t.sqlTime > lift(timeEntityA.sqlTime) &&
              t.sqlTimestamp > lift(timeEntityA.sqlTimestamp) &&
              t.timeLocalDate > lift(timeEntityA.timeLocalDate) &&
              t.timeLocalTime > lift(timeEntityA.timeLocalTime) &&
              t.timeLocalDateTime > lift(timeEntityA.timeLocalDateTime) &&
              t.timeZonedDateTime > lift(timeEntityA.timeZonedDateTime) &&
              t.timeInstant > lift(timeEntityA.timeInstant) &&
              t.timeOffsetTime > lift(timeEntityA.timeOffsetTime) &&
              t.timeOffsetDateTime > lift(timeEntityA.timeOffsetDateTime)
          )
        )
        .head

    assert(actual == timeEntityB)
  }
}
