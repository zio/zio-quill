package io.getquill.examples.other

import io.getquill._
import io.getquill.jdbczio.Quill
import zio._

import javax.sql.DataSource
import java.sql.SQLException

final case class Person(name: String, age: Int)

object QuillContext extends PostgresZioJdbcContext(SnakeCase) {
  val dataSourceLayer: ZLayer[Any,Nothing,DataSource] = Quill.DataSource.fromPrefix("testPostgresDB").orDie
}

object DataService {
  def getPeople: ZIO[DataServiceLive,Nothing,IO[SQLException,List[Person]]] =
    ZIO.serviceWith[DataServiceLive](_.getPeople)
  def getPeopleOlderThan(age: Int): ZIO[DataServiceLive,Nothing,Nothing] =
    ZIO.serviceWith[DataServiceLive](_.getPeopleOlderThan(age))
}

object DataServiceLive {
  val layer: ZLayer[DataSource,Nothing,DataServiceLive] = ZLayer.fromFunction(DataServiceLive.apply _)
}

final case class DataServiceLive(dataSource: DataSource) {
  import QuillContext._
  def getPeople: IO[SQLException,List[Person]] = run(query[Person]).provideEnvironment(ZEnvironment(dataSource))
  def getPeopleOlderThan(age: Int) =
    run(query[Person].filter(p => p.age > lift(age))).provideEnvironment(ZEnvironment(dataSource))
}

object ZioAppExample extends ZIOAppDefault {
  override def run: URIO[Any,ExitCode] =
    DataService.getPeople
      .provide(QuillContext.dataSourceLayer, DataServiceLive.layer)
      .debug("Results")
      .exitCode
}
