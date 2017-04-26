package io.getquill.context.cassandra

import java.sql.Timestamp
import java.util.UUID

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
    blobs:      Set[Array[Byte]],
    dates:      Set[LocalDate],
    timestamps: Set[Timestamp],
    uuids:      Set[UUID]
  )
  val e = SetsEntity(1, Set("c"), Set(BigDecimal(1.33)), Set(true), Set(1, 2), Set(2, 3), Set(1f, 3f),
    Set(5d), Set(Array(1.toByte)), Set(new LocalDate(System.currentTimeMillis())),
    Set(new Timestamp(System.currentTimeMillis())), Set(UUID.randomUUID()))
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
