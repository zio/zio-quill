package io.getquill.examples

import io.getquill._
import io.getquill.ziojdbc.Quill
import zio._

import javax.sql.DataSource

case class Person(name: String, age: Int)

object QuillContext extends PostgresZioJdbcContext(SnakeCase) {
  val dataSourceLayer = Quill.DataSource.fromPrefix("testPostgresDB").orDie
}

object DataService {
  def getPeople =
    ZIO.serviceWith[DataServiceLive](_.getPeople)
  def getPeopleOlderThan(age: Int) =
    ZIO.serviceWith[DataServiceLive](_.getPeopleOlderThan(age))
}

object DataServiceLive {
  val layer = ZLayer.fromFunction(DataServiceLive.apply _)
}

final case class DataServiceLive(dataSource: DataSource) {
  import QuillContext._
  def getPeople = run(query[Person]).provideEnvironment(ZEnvironment(dataSource))
  def getPeopleOlderThan(age: Int) = run(query[Person].filter(p => p.age > lift(age))).provideEnvironment(ZEnvironment(dataSource))
}

object ZioAppExample extends ZIOAppDefault {
  override def run =
    DataService.getPeople
      .provide(QuillContext.dataSourceLayer, DataServiceLive.layer)
      .debug("Results")
      .exitCode
}
