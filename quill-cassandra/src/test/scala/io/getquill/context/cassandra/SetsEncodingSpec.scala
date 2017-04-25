package io.getquill.context.cassandra

import io.getquill.Spec
import org.scalatest.BeforeAndAfterEach

class SetsEncodingSpec extends Spec with BeforeAndAfterEach {
  val ctx = testSyncDB
  import ctx._

  case class SetsEntity(
    id:    Int,
    texts: Set[String],
    //decimals: List[BigDecimal],
    bools:   Set[Boolean],
    ints:    Set[Int],
    longs:   Set[Long],
    floats:  Set[Float],
    doubles: Set[Double]
  )
  val e = SetsEntity(1, Set("c"), Set(true), Set(1, 2), Set(2, 3), Set(1f, 3f), Set(5d))
  val q = quote(query[SetsEntity])

  "Set encoders/decoders" in {
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  override protected def beforeEach(): Unit = {
    ctx.run(q.delete)
  }
}
