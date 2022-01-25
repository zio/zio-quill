package io.getquill.postgres

import io.getquill.{ JdbcContextConfig, PeopleZioSpec, Prefix }

import java.io.Closeable
import javax.sql.DataSource
import io.getquill.context.qzio.ImplicitSyntax._
import io.getquill.util.LoadConfig
import zio.{ Has, Task, ZManaged }

class ImplicitEnvPatternSpec extends PeopleZioSpec {

  // Need to specify prefix to use for the setup
  override def prefix: Prefix = Prefix("testPostgresDB")
  val context = testContext
  import testContext._

  override def beforeAll = {
    super.beforeAll()
    testContext.transaction {
      for {
        _ <- testContext.run(query[Couple].delete)
        _ <- testContext.run(query[Person].filter(_.age > 0).delete)
        _ <- testContext.run(liftQuery(peopleEntries).foreach(p => peopleInsert(p)))
        _ <- testContext.run(liftQuery(couplesEntries).foreach(p => couplesInsert(p)))
      } yield ()
    }.runSyncUnsafe()
  }

  case class MyService(ds: DataSource with Closeable) {
    implicit val env = Implicit(Has(ds))

    def alexes = testContext.run(query[Person].filter(p => p.name == "Alex"))
    def berts = testContext.run(query[Person].filter(p => p.name == "Bert"))
    def coras = testContext.run(query[Person].filter(p => p.name == "Cora"))
  }

  def makeDataSource() = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  "dataSource based context should fetch results" in {
    val (alexes, berts, coras) =
      ZManaged.fromAutoCloseable(Task(makeDataSource())).use { ds =>
        for {
          svc <- Task(MyService(ds))
          alexes <- svc.alexes
          berts <- svc.berts
          coras <- svc.coras
        } yield (alexes, berts, coras)
      }.runSyncUnsafe()

    alexes must contain theSameElementsAs (peopleEntries.filter(_.name == "Alex"))
    berts must contain theSameElementsAs (peopleEntries.filter(_.name == "Bert"))
    coras must contain theSameElementsAs (peopleEntries.filter(_.name == "Cora"))
  }

}
