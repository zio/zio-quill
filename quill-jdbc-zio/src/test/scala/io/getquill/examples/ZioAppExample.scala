package io.getquill.examples

import io.getquill._
import io.getquill.context.ZioJdbc._
import zio._
import zio.magic._
import javax.sql.DataSource

case class Person(name: String, age: Int)

object QuillContext extends PostgresZioJdbcContext(SnakeCase) {
  val dataSourceLayer = DataSourceLayer.fromPrefix("testPostgresDB").orDie
}

object DataService {
  def getPeople =
    ZIO.serviceWith[DataServiceLive](_.getPeople)
  def getPeopleOlderThan(age: Int) =
    ZIO.serviceWith[DataServiceLive](_.getPeopleOlderThan(age))
}

object DataServiceLive {
  val layer = (DataServiceLive.apply _).toLayer
}

final case class DataServiceLive(dataSource: DataSource) {
  import QuillContext._
  val env = Has(dataSource)
  def getPeople = run(query[Person]).provide(env)
  def getPeopleOlderThan(age: Int) =
    run(query[Person].filter(p => p.age > lift(age))).provide(env)
}

object ZioAppExample extends App {
  override def run(args: List[String]) =
    DataService.getPeople
      .inject(QuillContext.dataSourceLayer, DataServiceLive.layer)
      .debug("Results")
      .exitCode
}
