package io.getquill.context.bigquery

import io.getquill._
import com.google.cloud.bigquery._

class BigQueryContextSpec extends Spec {

  val bigquery: BigQuery = BigQueryOptions.getDefaultInstance().getService()
  val context = new BigQueryContext(NamingStrategy(SnakeCase, MysqlEscape), bigquery)
  import context._

  case class Station(stationId: Long, name: String)
  val stations = quote {
    querySchema[Station]("bigquery-public-data.new_york.citibike_stations")
  }

  "run a simple query" in {
    val q = quote {
      stations.filter(_.stationId == 3416L).map(_.name)
    }
    val res = context.run(q)
    assert(res === List("7 Ave & Park Pl"))
  }
}
