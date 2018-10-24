package io.getquill.context.sql.mirror

import io.getquill.Spec
import io.getquill.context.mirror.Row

class ObservationMirrorSpec extends Spec {

  val ctx = io.getquill.context.sql.testContext
  import ctx._

  case class LatLon(lat: Int, lon: Int) extends Embedded
  case class ScalarData(value: Long, position: Option[LatLon]) extends Embedded
  case class Observation(data: Option[ScalarData], foo: Option[String], bar: Option[String])

  val obs = quote {
    querySchema[Observation](
      "observation",
      _.data.map(_.value) -> "obs_value",
      _.data.map(_.position.map(_.lat)) -> "obs_lat",
      _.data.map(_.position.map(_.lon)) -> "obs_lon",
      _.bar -> "baz"
    )
  }

  val obsEntry = Observation(Some(ScalarData(123, Some(LatLon(2, 3)))), None, Some("abc"))

  "select query" in {
    ctx.run(obs).string mustEqual
      "SELECT x.obs_value, x.obs_lat, x.obs_lon, x.foo, x.baz FROM observation x"
  }

  "insert query" in {
    val r = ctx.run(obs.insert(lift(obsEntry)))
    r.string mustEqual "INSERT INTO observation (obs_value,obs_lat,obs_lon,foo,baz) VALUES (?, ?, ?, ?, ?)"
    r.prepareRow mustEqual Row(Some(123), Some(Some(2)), Some(Some(3)), None, Some("abc"))
  }

  "update query" in {
    val r = ctx.run(obs.update(lift(obsEntry)))
    r.string mustEqual "UPDATE observation SET obs_value = ?, obs_lat = ?, obs_lon = ?, foo = ?, baz = ?"
    r.prepareRow mustEqual Row(Some(123), Some(Some(2)), Some(Some(3)), None, Some("abc"))
  }
}
