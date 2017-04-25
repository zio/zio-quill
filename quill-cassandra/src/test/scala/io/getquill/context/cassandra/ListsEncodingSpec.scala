package io.getquill.context.cassandra

import io.getquill.Spec
import org.scalatest.BeforeAndAfterEach

class ListsEncodingSpec extends Spec with BeforeAndAfterEach {
  val ctx = testSyncDB
  import ctx._

  case class ListsEntity(
    id:    Int,
    texts: List[String],
    //decimals: List[BigDecimal],
    bools:   List[Boolean],
    ints:    List[Int],
    longs:   List[Long],
    floats:  List[Float],
    doubles: List[Double]
  )
  val e = ListsEntity(1, List("c"), List(true), List(1, 2), List(2, 3), List(1f, 3f), List(5d))
  val q = quote(query[ListsEntity])

  "List encoders/decoders" in {
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  override protected def beforeEach(): Unit = {
    ctx.run(q.delete)
  }
}
