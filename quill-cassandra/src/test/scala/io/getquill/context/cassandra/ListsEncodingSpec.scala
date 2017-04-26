package io.getquill.context.cassandra

import java.util.{ Date, UUID }

import com.datastax.driver.core.LocalDate
import io.getquill.Spec
import org.scalatest.BeforeAndAfterEach

class ListsEncodingSpec extends Spec with BeforeAndAfterEach {
  val ctx = testSyncDB
  import ctx._

  println(implicitly[MappedType[Int, java.lang.Integer]])

  case class ListsEntity(
    id:         Int,
    texts:      List[String],
    decimals:   List[BigDecimal],
    bools:      List[Boolean],
    ints:       List[Int],
    longs:      List[Long],
    floats:     List[Float],
    doubles:    List[Double],
    dates:      List[LocalDate],
    timestamps: List[Date],
    uuids:      List[UUID]
  )
  val e = ListsEntity(1, List("c"), List(BigDecimal(1.33)), List(true), List(1, 2), List(2, 3), List(1f, 3f),
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

  override protected def beforeEach(): Unit = {
    ctx.run(q.delete)
  }

  private def nowMillis: Long = System.currentTimeMillis()
}
