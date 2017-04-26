package io.getquill.context.cassandra

import java.util.{ Date, UUID }

import com.datastax.driver.core.LocalDate
import io.getquill.Spec
import org.scalatest.BeforeAndAfterEach

class MapsEncodingSpec extends Spec with BeforeAndAfterEach {
  val ctx = testSyncDB
  import ctx._

  case class MapsEntity(
    id:            Int,
    textDecimal:   Map[String, BigDecimal],
    intDouble:     Map[Int, Double],
    longFloat:     Map[Long, Float],
    boolDate:      Map[Boolean, LocalDate],
    uuidTimestamp: Map[UUID, Date]
  )
  val e = MapsEntity(1, Map("1" -> BigDecimal(1)), Map(1 -> 1d, 2 -> 2d, 3 -> 3d), Map(1l -> 3f),
    Map(true -> LocalDate.fromMillisSinceEpoch(System.currentTimeMillis())),
    Map(UUID.randomUUID() -> new Date))
  val q = quote(query[MapsEntity])

  "Map encoders/decoders" in {
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Empty maps" in {
    val expected = e.copy(textDecimal = Map.empty)
    ctx.run(q.insert(lift(expected)))
    ctx.run(q.filter(_.id == 1)).head mustBe expected
  }

  override protected def beforeEach(): Unit = {
    ctx.run(q.delete)
  }
}
