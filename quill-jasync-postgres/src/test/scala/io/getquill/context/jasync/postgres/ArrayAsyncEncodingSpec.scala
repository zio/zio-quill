package io.getquill.context.jasync.postgres

import java.time.{LocalDate, LocalDateTime}
import java.util.{Date, UUID}

import io.getquill.context.sql.EncodingTestType
import io.getquill.context.sql.encoding.ArrayEncodingBaseSpec

import scala.concurrent.ExecutionContext.Implicits.global

class ArrayAsyncEncodingSpec extends ArrayEncodingBaseSpec {
  val ctx = testContext
  import ctx._

  val q = quote(query[ArraysTestEntity])

  "Support all sql base types and `Iterable` implementers" in {
    await(ctx.run(q.insertValue(lift(e))))
    val actual = await(ctx.run(q)).head
    actual mustEqual e
    baseEntityDeepCheck(actual, e)
  }

  "Java8 times" in {
    case class Java8Times(timestamps: Seq[LocalDateTime], dates: Seq[LocalDate])
    val jE = Java8Times(Seq(LocalDateTime.now()), Seq(LocalDate.now()))
    val jQ = quote(querySchema[Java8Times]("ArraysTestEntity"))
    await(ctx.run(jQ.insertValue(lift(jE))))
    val actual = await(ctx.run(jQ)).head
    actual.timestamps mustBe jE.timestamps
    actual.dates mustBe jE.dates
  }

  "Support Iterable encoding basing on MappedEncoding" in {
    val wrapQ = quote(querySchema[WrapEntity]("ArraysTestEntity"))
    await(ctx.run(wrapQ.insertValue(lift(wrapE))))
    await(ctx.run(wrapQ)).head mustBe wrapE
  }

  "Arrays in where clause" in {
    await(ctx.run(q.insertValue(lift(e))))
    val actual1 = await(ctx.run(q.filter(_.texts == lift(List("test")))))
    val actual2 = await(ctx.run(q.filter(_.texts == lift(List("test2")))))
    baseEntityDeepCheck(actual1.head, e)
    actual1 mustEqual List(e)
    actual2 mustEqual List()
  }

  // Need to have an actual value in the table in order for the decoder to go off. Previously,
  // there was guaranteed to be information there due to ordering of build artifacts but not anymore.
  "fail if found not an array" in {
    case class RealEncodingTestEntity(
      v1: String,
      v2: BigDecimal,
      v3: Boolean,
      v4: Byte,
      v5: Short,
      v6: Int,
      v7: Long,
      v8: Float,
      v9: Double,
      v10: Array[Byte],
      v11: Date,
      v12: EncodingTestType,
      v13: LocalDate,
      v14: UUID,
      o1: Option[String],
      o2: Option[BigDecimal],
      o3: Option[Boolean],
      o4: Option[Byte],
      o5: Option[Short],
      o6: Option[Int],
      o7: Option[Long],
      o8: Option[Float],
      o9: Option[Double],
      o10: Option[Array[Byte]],
      o11: Option[Date],
      o12: Option[EncodingTestType],
      o13: Option[LocalDate],
      o14: Option[UUID],
      o15: Option[io.getquill.context.sql.Number]
    )

    val insertValue =
      RealEncodingTestEntity(
        "s",
        BigDecimal(1.1),
        true,
        11.toByte,
        23.toShort,
        33,
        431L,
        34.4f,
        42d,
        Array(1.toByte, 2.toByte),
        new Date(31200000),
        EncodingTestType("s"),
        LocalDate.of(2013, 11, 23),
        UUID.randomUUID(),
        Some("s"),
        Some(BigDecimal(1.1)),
        Some(true),
        Some(11.toByte),
        Some(23.toShort),
        Some(33),
        Some(431L),
        Some(34.4f),
        Some(42d),
        Some(Array(1.toByte, 2.toByte)),
        Some(new Date(31200000)),
        Some(EncodingTestType("s")),
        Some(LocalDate.of(2013, 11, 23)),
        Some(UUID.randomUUID()),
        Some(io.getquill.context.sql.Number("0"))
      )

    val realEntity = quote {
      querySchema[RealEncodingTestEntity]("EncodingTestEntity")
    }
    await(ctx.run(realEntity.insertValue(lift(insertValue))))

    case class EncodingTestEntity(v1: List[String])
    intercept[IllegalStateException](await(ctx.run(query[EncodingTestEntity])))
  }

  override protected def beforeEach(): Unit = {
    await(ctx.run(q.delete))
    ()
  }
}
