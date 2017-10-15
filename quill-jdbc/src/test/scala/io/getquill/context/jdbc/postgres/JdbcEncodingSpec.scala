package io.getquill.context.jdbc.postgres

import java.time.{ LocalDate, LocalDateTime }

import io.getquill.context.sql.EncodingSpec

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
    case class EncodingTestEntity(v11: LocalDateTime)
    case class E(v11: Option[LocalDateTime])
    val e = EncodingTestEntity(LocalDateTime.now())
    val res: List[EncodingTestEntity] = performIO {
      for {
        _ <- testContext.runIO(query[EncodingTestEntity].delete)
        _ <- testContext.runIO(query[EncodingTestEntity].insert(lift(e)))
        _ <- testContext.runIO(querySchema[E]("EncodingTestEntity").insert(lift(E(None))))
        r <- testContext.runIO(query[EncodingTestEntity])
      } yield r
    }
    res must contain theSameElementsAs List(e, EncodingTestEntity(LocalDate.ofEpochDay(0).atStartOfDay()))
  }
}
