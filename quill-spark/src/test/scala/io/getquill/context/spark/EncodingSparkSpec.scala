package io.getquill.context.spark

import io.getquill.Spec

case class EncodingTestEntity(
  v1: String,
  v2: BigDecimal,
  v3: Boolean,
  v4: Byte,
  v5: Short,
  v6: Int,
  v7: Long,
  v8: Double,
  v9: Array[Byte],
  o1: Option[String],
  o2: Option[BigDecimal],
  o3: Option[Boolean],
  o4: Option[Byte],
  o5: Option[Short],
  o6: Option[Int],
  o7: Option[Long],
  o8: Option[Double],
  o9: Option[Array[Byte]]
)

class EncodingSparkSpec extends Spec {

  import testContext._
  import sqlContext.implicits._

  implicit val e = org.apache.spark.sql.Encoders.DATE

  "encodes and decodes types" in {
    verify(testContext.run(entities).collect.toList)
  }

  "string" in {
    val v = "s"
    val q = quote {
      entities.filter(_.v1 == lift(v)).map(_.v1)
    }
    testContext.run(q).collect.toList mustEqual List(v)
  }

  "string with ' " in {
    val v = "will'break"

    val entities = liftQuery(Seq(
      EncodingTestEntity(
        v,
        BigDecimal(1.1),
        true,
        11.toByte,
        23.toShort,
        33,
        431L,
        42d,
        Array(1.toByte, 2.toByte),
        Some("s"),
        Some(BigDecimal(1.1)),
        Some(true),
        Some(11.toByte),
        Some(23.toShort),
        Some(33),
        Some(431L),
        Some(42d),
        Some(Array(1.toByte, 2.toByte))
      )
    ).toDS)

    val q = quote {
      entities.filter(_.v1 == lift(v)).map(_.v1)
    }
    testContext.run(q).collect.toList mustEqual List(v)
  }

  "bigDecimal" in {
    val v = BigDecimal(1.1)
    val q = quote {
      entities.filter(_.v2 == lift(v)).map(_.v2)
    }
    testContext.run(q).collect.toList mustEqual List(v)
  }

  "boolean" in {
    val v = true
    val q = quote {
      entities.filter(_.v3 == lift(v)).map(_.v3)
    }
    testContext.run(q).collect.toList mustEqual List(v)
  }

  "byte" in {
    val v = 11.toByte
    val q = quote {
      entities.filter(_.v4 == lift(v)).map(_.v4)
    }
    testContext.run(q).collect.toList mustEqual List(v)
  }

  "short" in {
    val v = 23.toShort
    val q = quote {
      entities.filter(_.v5 == lift(v)).map(_.v5)
    }
    testContext.run(q).collect.toList mustEqual List(v)
  }

  "int" in {
    val v = 33
    val q = quote {
      entities.filter(_.v6 == lift(v)).map(_.v6)
    }
    testContext.run(q).collect.toList mustEqual List(v)
  }

  "long" in {
    val v = 431L
    val q = quote {
      entities.filter(_.v7 == lift(v)).map(_.v7)
    }
    testContext.run(q).collect.toList mustEqual List(v)
  }

  "double" in {
    val v = 42d
    val q = quote {
      entities.filter(_.v8 == lift(v)).map(_.v8)
    }
    testContext.run(q).collect.toList mustEqual List(v)
  }

  "option" - {
    "defined" in {
      val v = Option(42d)
      val q = quote {
        entities.filter(_.o8.exists(lift(v).contains(_))).map(_.o8)
      }
      testContext.run(q).collect.toList mustEqual List(v)
    }
    "undefined" in {
      val v: Option[Double] = None
      val q = quote {
        entities.filter(_.o8.exists(lift(v).contains(_))).map(_.o8)
      }
      testContext.run(q).collect.toList mustEqual List()
    }
  }

  "mapped encoding" in {
    case class Temp(i: Int)
    implicit val enc = MappedEncoding[Temp, Int](_.i)
    implicitly[Encoder[Temp]]
  }

  val entities = liftQuery {
    Seq(
      EncodingTestEntity(
        "s",
        BigDecimal(1.1),
        true,
        11.toByte,
        23.toShort,
        33,
        431L,
        42d,
        Array(1.toByte, 2.toByte),
        Some("s"),
        Some(BigDecimal(1.1)),
        Some(true),
        Some(11.toByte),
        Some(23.toShort),
        Some(33),
        Some(431L),
        Some(42d),
        Some(Array(1.toByte, 2.toByte))
      ),
      EncodingTestEntity(
        "",
        BigDecimal(0),
        false,
        0.toByte,
        0.toShort,
        0,
        0L,
        0D,
        Array(),
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None
      )
    ).toDS
  }

  def verify(result: List[EncodingTestEntity]) =
    result match {
      case List(e1, e2) =>

        e1.v1 mustEqual "s"
        e1.v2 mustEqual BigDecimal(1.1)
        e1.v3 mustEqual true
        e1.v4 mustEqual 11.toByte
        e1.v5 mustEqual 23.toShort
        e1.v6 mustEqual 33
        e1.v7 mustEqual 431L
        e1.v8 mustEqual 42d
        e1.v9.toList mustEqual List(1.toByte, 2.toByte)

        e1.o1 mustEqual Some("s")
        e1.o2 mustEqual Some(BigDecimal(1.1))
        e1.o3 mustEqual Some(true)
        e1.o4 mustEqual Some(11.toByte)
        e1.o5 mustEqual Some(23.toShort)
        e1.o6 mustEqual Some(33)
        e1.o7 mustEqual Some(431L)
        e1.o8 mustEqual Some(42d)
        e1.o9.map(_.toList) mustEqual Some(List(1.toByte, 2.toByte))

        e2.v1 mustEqual ""
        e2.v2 mustEqual BigDecimal(0)
        e2.v3 mustEqual false
        e2.v4 mustEqual 0.toByte
        e2.v5 mustEqual 0.toShort
        e2.v6 mustEqual 0
        e2.v7 mustEqual 0L
        e2.v8 mustEqual 0d
        e2.v9.toList mustEqual Nil

        e2.o1 mustEqual None
        e2.o2 mustEqual None
        e2.o3 mustEqual None
        e2.o4 mustEqual None
        e2.o5 mustEqual None
        e2.o6 mustEqual None
        e2.o7 mustEqual None
        e2.o8 mustEqual None
        e2.o9.map(_.toList) mustEqual None
    }
}