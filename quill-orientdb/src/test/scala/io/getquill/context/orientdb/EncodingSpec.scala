package io.getquill.context.orientdb

import java.util.Date

import io.getquill.Spec
import io.getquill.Query

class EncodingSpec extends Spec {

  "encodes and decodes types" - {

    "sync" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      ctx.run(query[EncodingTestEntity].delete)
      ctx.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
      verify(ctx.run(query[EncodingTestEntity]))
      ctx.close()
    }
  }

  "mapped" in {
    val ctx = orientdb.testSyncDB
    import ctx._
    // 100% coverage
    case class A()
    case class B()
    val a1: Encoder[A] = encoder((b, c, d) => d)
    val a2: Decoder[A] = decoder(b => c => A())
    mappedDecoder(MappedEncoding[A, B](_ => B()), a2).isInstanceOf[OrientDBDecoder[B]] mustBe true
    mappedEncoder(MappedEncoding[B, A](_ => A()), a1).isInstanceOf[OrientDBEncoder[B]] mustBe true
  }

  "encodes collections" - {

    "sync" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      val q = quote {
        (list: Query[Int]) =>
          query[EncodingTestEntity].filter(t => list.contains(t.id))
      }
      ctx.run(query[EncodingTestEntity].delete)
      ctx.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insert(e)))
      verify(ctx.run(q(liftQuery(insertValues.map(_.id)))))
      ctx.close()
    }
  }

  private def verify(result: List[EncodingTestEntity]): Unit = {
    result.zip(insertValues) match {
      case List((e1, a1), (e2, a2)) =>
        verify(e1, a1)
        verify(e2, a2)
    }
  }

  private def verify(e: EncodingTestEntity, a: EncodingTestEntity): Unit = {
    e.id mustEqual a.id

    e.v1 mustEqual a.v1
    e.v2 mustEqual a.v2
    e.v3 mustEqual a.v3
    e.v4 mustEqual a.v4
    e.v5 mustEqual a.v5
    e.v6 mustEqual a.v6
    e.v7 mustEqual a.v7
    e.v8.toList mustEqual a.v8.toList
    e.v11.isInstanceOf[Date]
    e.v13 mustEqual a.v13
    e.o1 mustEqual a.o1
    e.o2 mustEqual a.o2
    e.o3 mustEqual a.o3
    e.o4 mustEqual a.o4
    e.o5 mustEqual a.o5
    e.o6 mustEqual a.o6
    e.o7 mustEqual a.o7
    e.o8.map(_.toList) mustEqual a.o8.map(_.toList)
    e.o10 mustEqual a.o10

    ()
  }

  case class EncodingTestEntity(
    id:  Int,
    v1:  String,
    v2:  BigDecimal,
    v3:  Boolean,
    v4:  Int,
    v5:  Long,
    v6:  Float,
    v7:  Double,
    v8:  Array[Byte],
    v11: Date,
    v12: Short,
    v13: Byte,
    o1:  Option[String],
    o2:  Option[BigDecimal],
    o3:  Option[Boolean],
    o4:  Option[Int],
    o5:  Option[Long],
    o6:  Option[Float],
    o7:  Option[Double],
    o8:  Option[Array[Byte]],
    o9:  Option[Date],
    o10: Option[Byte]
  )

  val insertValues =
    List(
      EncodingTestEntity(
        id = 1,
        v1 = "s",
        v2 = BigDecimal(1.1),
        v3 = true,
        v4 = 33,
        v5 = 431L,
        v6 = 34.4f,
        v7 = 42.4d,
        v8 = Array(1.toByte, 2.toByte),
        v11 = new Date(31202000),
        v12 = 1,
        v13 = 8.toByte,
        o1 = Some("s"),
        o2 = Some(BigDecimal(1.1)),
        o3 = Some(true),
        o4 = Some(33),
        o5 = Some(431L),
        o6 = Some(34.4f),
        o7 = Some(42.4d),
        o8 = Some(Array(1.toByte, 2.toByte)),
        o9 = Some(new Date(31200000)),
        o10 = Some(8.toByte)
      ),
      EncodingTestEntity(
        id = 2,
        v1 = "",
        v2 = BigDecimal(0.0),
        v3 = false,
        v4 = 0,
        v5 = 0L,
        v6 = 0.0F,
        v7 = 0.0D,
        v8 = Array(),
        v11 = new Date(0),
        v12 = 2,
        v13 = 7.toByte,
        o1 = None,
        o2 = None,
        o3 = None,
        o4 = None,
        o5 = None,
        o6 = None,
        o7 = None,
        o8 = None,
        o9 = None,
        o10 = None
      )
    )
}