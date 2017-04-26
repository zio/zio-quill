package io.getquill.context.cassandra

import java.util.{ Date, UUID }

import com.datastax.driver.core.LocalDate
import io.getquill.Spec
import org.scalatest.BeforeAndAfterEach

class SetsEncodingSpec extends Spec with BeforeAndAfterEach {
  val ctx = testSyncDB
  import ctx._

  case class SetsEntity(
    id:         Int,
    texts:      Set[String],
    decimals:   Set[BigDecimal],
    bools:      Set[Boolean],
    ints:       Set[Int],
    longs:      Set[Long],
    floats:     Set[Float],
    doubles:    Set[Double],
    dates:      Set[LocalDate],
    timestamps: Set[Date],
    uuids:      Set[UUID]
  )
  val e = SetsEntity(1, Set("c"), Set(BigDecimal(1.33)), Set(true), Set(1, 2), Set(2, 3), Set(1f, 3f),
    Set(5d), Set(LocalDate.fromMillisSinceEpoch(System.currentTimeMillis())),
    Set(new Date), Set(UUID.randomUUID()))
  val q = quote(query[SetsEntity])

  "Set encoders/decoders" in {
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Empty sets" in {
    val expected = e.copy(ints = Set.empty, bools = Set.empty)
    ctx.run(q.insert(lift(expected)))
    ctx.run(q.filter(_.id == 1)).head mustBe expected
  }

  override protected def beforeEach(): Unit = {
    ctx.run(q.delete)
  }
}
