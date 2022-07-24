package io.getquill.postgres

import io.getquill.ziojdbc.Quill
import io.getquill.{ testContext => _, _ }
import zio.{ Unsafe, ZIO, ZLayer }

import java.sql.SQLException
import javax.sql.DataSource

class MultiLevelServiceSpec extends PeopleZioSpec with ZioSpec {

  val context = testContext
  import testContext._
  val entries = List(Person("Joe", 1), Person("Jack", 2))

  override def beforeAll = {
    super.beforeAll()
    testContext.transaction {
      for {
        _ <- testContext.run(query[Person].delete)
        _ <- testContext.run(liftQuery(entries).foreach(p => peopleInsert(p)))
      } yield ()
    }.runSyncUnsafe()
  }

  case class DataService(quill: Quill[PostgresDialect, Literal]) {
    import quill.{ run => qrun, _ }
    val people = quote { query[Person] }
    def somePeopleByName = quote { (ps: Query[Person], name: String) => ps.filter(p => p.name == name) }
    def peopleByName = quote { (name: String) => people.filter(p => p.name == name) }
    def getAllPeople(): ZIO[Any, SQLException, List[Person]] = qrun(people)
    def getPeopleByName(name: String): ZIO[Any, SQLException, List[Person]] = qrun(peopleByName(lift(name)))
  }
  case class ApplicationLive(dataService: DataService) {
    import dataService._
    import dataService.quill.{ run => qrun, _ }

    val joes = quote { peopleByName("Joe") }
    def getJoes: ZIO[Any, SQLException, List[Person]] = qrun(joes)
    def getPeopleByName3(name: String): ZIO[Any, SQLException, List[Person]] = qrun(somePeopleByName(query[Person], lift(name)))
    def getPeopleByName2(name: String): ZIO[Any, SQLException, List[Person]] = qrun(peopleByName(lift(name)))
    def getPeopleByName(name: String): ZIO[Any, SQLException, List[Person]] = dataService.getPeopleByName(name)
    def getAllPeople(): ZIO[Any, SQLException, List[Person]] = dataService.getAllPeople()
  }
  val dataServiceLive = ZLayer.fromFunction(DataService.apply _)
  val applicationLive = ZLayer.fromFunction(ApplicationLive.apply _)

  object Application {
    def getJoes() = ZIO.serviceWithZIO[ApplicationLive](_.getJoes)
    def getPeopleByName3(name: String) = ZIO.serviceWithZIO[ApplicationLive](_.getPeopleByName3(name))
    def getPeopleByName2(name: String) = ZIO.serviceWithZIO[ApplicationLive](_.getPeopleByName2(name))
    def getPeopleByName(name: String) = ZIO.serviceWithZIO[ApplicationLive](_.getPeopleByName(name))
    def getAllPeople() = ZIO.serviceWithZIO[ApplicationLive](_.getAllPeople())
  }

  "All Composition variations must work" in {

    val dataSourceLive = ZLayer.succeed(io.getquill.postgres.pool)
    val postgresServiceLive = ZLayer.fromFunction(Quill.PostgresService(Literal, _: DataSource))

    val (a, b, c, d, e) =
      Unsafe.unsafe { implicit u =>
        zio.Runtime.default.unsafe.run(
          (for {
            a <- Application.getJoes()
            b <- Application.getPeopleByName("Joe")
            c <- Application.getPeopleByName2("Joe")
            d <- Application.getPeopleByName3("Joe")
            e <- Application.getAllPeople()
          } yield (a, b, c, d, e)).provide(applicationLive, dataServiceLive, dataSourceLive, postgresServiceLive)
        ).getOrThrow()
      }

    val joes = entries.filter(_.name == "Joe")
    a mustEqual joes
    b mustEqual joes
    c mustEqual joes
    d mustEqual joes
    e.toSet mustEqual entries.toSet
  }
}
