package io.getquill.context.ndbc.postgres

import java.time.{ LocalDate, LocalDateTime }
import java.util.{ Date, UUID }

import io.getquill.context.sql.EncodingSpec

class NdbcPostgresEncodingSpec extends EncodingSpec {

  val context = testContext
  import context._

  "encodes and decodes types" in {
    val r =
      for {
        _ <- context.run(delete)
        _ <- context.run(liftQuery(insertValues).foreach(e => insert(e)))
        result <- context.run(query[EncodingTestEntity])
      } yield result

    verify(get(r))
  }

  "encodes and decodes uuids" in {
    case class EncodingUUIDTestEntity(v1: UUID)
    val testUUID = UUID.fromString("e5240c08-6ee7-474a-b5e4-91f79c48338f")

    // delete old values
    val q0 = quote(query[EncodingUUIDTestEntity].delete)
    val rez0 = get(context.run(q0))

    // insert new uuid
    val rez1 = get(context.run(query[EncodingUUIDTestEntity].insert(lift(EncodingUUIDTestEntity(testUUID)))))

    // verify you can get the uuid back from the db
    val q2 = quote(query[EncodingUUIDTestEntity].map(p => p.v1))

    val rez2 = get(testContext.run(q2))

    rez2 mustEqual List(testUUID)
  }

  "fails if the column has the wrong type" - {
    "numeric" in {
      get(context.run(liftQuery(insertValues).foreach(e => insert(e))))
      case class EncodingTestEntity(v1: Int)
      val e = intercept[UnsupportedOperationException] {
        get(context.run(query[EncodingTestEntity]))
      }
    }
    "non-numeric" in {
      get(context.run(liftQuery(insertValues).foreach(e => insert(e))))
      case class EncodingTestEntity(v1: Date)
      val e = intercept[UnsupportedOperationException] {
        get(context.run(query[EncodingTestEntity]))
      }
    }
  }

  "encodes sets" in {
    val q = quote {
      (set: Query[Int]) =>
        query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    val fut =
      for {
        _ <- context.run(query[EncodingTestEntity].delete)
        _ <- context.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
        r <- context.run(q(liftQuery(insertValues.map(_.v6))))
      } yield {
        r
      }
    verify(get(fut))
  }

  "returning UUID" in {
    val success = for {
      uuid <- get(context.run(insertBarCode(lift(barCodeEntry))))
      barCode <- get(context.run(findBarCodeByUuid(uuid))).headOption
    } yield {
      verifyBarcode(barCode)
    }
    success must not be empty
  }

  "encodes localdate type" in {
    case class DateEncodingTestEntity(v1: LocalDate, v2: LocalDate)
    val entity = DateEncodingTestEntity(LocalDate.now, LocalDate.now)
    val r = for {
      _ <- context.run(query[DateEncodingTestEntity].delete)
      _ <- context.run(query[DateEncodingTestEntity].insert(lift(entity)))
      result <- context.run(query[DateEncodingTestEntity])
    } yield result
    get(r) must contain(entity)
  }

  "encodes localdatetime type" in {
    case class DateEncodingTestEntity(v1: LocalDateTime, v2: LocalDateTime)
    val entity = DateEncodingTestEntity(LocalDateTime.now, LocalDateTime.now)
    val r = for {
      _ <- context.run(query[DateEncodingTestEntity].delete)
      _ <- context.run(query[DateEncodingTestEntity].insert(lift(entity)))
      result <- context.run(query[DateEncodingTestEntity])
    } yield result
    get(r)
  }
}