package io.getquill.context.cassandra

import io.getquill.Spec
import org.scalatest.BeforeAndAfterEach

class MapsEncodingSpec extends Spec with BeforeAndAfterEach {
  val ctx = testSyncDB
  import ctx._

  case class MapsEntity(
    id:           Int,
    textsDoubles: Map[String, Double],
    intsBools:    Map[Int, Boolean],
    longsFloats:  Map[Long, Float]
  )
  val e = MapsEntity(1, Map("1" -> 1d), Map(1 -> true, 2 -> false), Map(1l -> 3f))
  val q = quote(query[MapsEntity])

  "Map encoders/decoders" in {
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  override protected def beforeEach(): Unit = {
    ctx.run(q.delete)
  }
}
