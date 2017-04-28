package io.getquill.context.cassandra

import java.util.{ Date, UUID }

import com.datastax.driver.core.LocalDate
import io.getquill.Spec
import org.scalatest.BeforeAndAfterEach

class ListsEncodingSpec extends Spec with BeforeAndAfterEach {
  val ctx = testSyncDB
  import ctx._

  case class ListsEntity(
    id:    Int,
    texts: List[String],
    //decimals:   List[BigDecimal],
    bools:      List[Boolean],
    ints:       List[Int],
    longs:      List[Long],
    floats:     List[Float],
    doubles:    List[Double],
    dates:      List[LocalDate],
    timestamps: List[Date],
    uuids:      List[UUID]
  )
  val e = ListsEntity(1, List("c"), /*List(BigDecimal(1.33)),*/ List(true), List(1, 2), List(2, 3), List(1f, 3f),
    List(5d), List(LocalDate.fromMillisSinceEpoch(System.currentTimeMillis())),
    List(new Date), List(UUID.randomUUID()))
  val q = quote(query[ListsEntity])

  "List encoders/decoders" in {
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Empty lists" in {
    val expected = e.copy(ints = Nil, bools = Nil)
    ctx.run(q.insert(lift(expected)))
    ctx.run(q.filter(_.id == 1)).head mustBe expected
  }

  "Mapped encoding for supported type" in {
    case class StrWrap(x: String)
    implicit val encodeIntWrap = MappedEncoding[StrWrap, String](_.x)
    implicit val decodeIntWrap = MappedEncoding[String, StrWrap](StrWrap.apply)
    case class StrEntity(id: Int, texts: List[StrWrap])
    val e = StrEntity(1, List("1", "2").map(StrWrap.apply))
    val q = quote(querySchema[StrEntity]("ListsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Mapped encoding for mapped type" in {
    case class IntWrap(x: Int)
    implicit val encodeIntWrap = MappedEncoding[IntWrap, Int](_.x)
    implicit val decodeIntWrap = MappedEncoding[Int, IntWrap](IntWrap.apply)

    case class IntEntity(id: Int, ints: List[IntWrap])
    val e = IntEntity(1, List(1, 2).map(IntWrap.apply))
    val q = quote(querySchema[IntEntity]("ListsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Blob (Array[Byte]) support" in {
    case class BlobsEntity(id: Int, blobs: List[Array[Byte]])
    val e = BlobsEntity(1, List(Array(1.toByte, 2.toByte), Array(2.toByte)))
    val q = quote(querySchema[BlobsEntity]("ListsEntity"))

    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1))
      .head.blobs.map(_.toList) mustBe e.blobs.map(_.toList)
  }

  override protected def beforeEach(): Unit = {
    ctx.run(q.delete)
  }
}
