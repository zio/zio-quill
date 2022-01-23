package io.getquill.postgres

import io.getquill.PeopleZioSpec

import org.scalatest.matchers.should.Matchers._
import zio.{ ZIO, ZLayer }
import io.getquill.context.ZioJdbc._

import javax.sql.DataSource

class OnDataSourceSpec extends PeopleZioSpec {

  val context = testContext

  import testContext._

  override def beforeAll = {
    super.beforeAll()
    testContext.transaction {
      for {
        _ <- testContext.run(query[Couple].delete)
        _ <- testContext.run(query[Person].delete)
        _ <- testContext.run(liftQuery(peopleEntries).foreach(p => peopleInsert(p)))
        _ <- testContext.run(liftQuery(couplesEntries).foreach(p => couplesInsert(p)))
      } yield ()
    }.runSyncUnsafe()
  }

  "onDataSource on underlying context" - {
    "should work with additional dependency" in {
      // This is how you import the decoders of `underlying` context without importing things that will conflict
      // i.e. the quote and run methods
      import testContext.underlying.{ quote => _, run => _, _ }
      val people =
        (for {
          n <- ZIO.service[String]
          out <- testContext.underlying.run(query[Person].filter(p => p.name == lift(n)))
        } yield out)
          .onSomeDataSource
          .provideSomeLayer[DataSource](ZLayer.succeed("Alex"))
          .runSyncUnsafe()

      people mustEqual peopleEntries.filter(p => p.name == "Alex")
    }
    "should work" in {
      // This is how you import the encoders/decoders of `underlying` context without importing things that will conflict
      // i.e. the quote and run methods
      import testContext.underlying.{ quote => _, run => _, prepare => _, _ }
      val people =
        (for {
          out <- testContext.underlying.run(query[Person].filter(p => p.name == "Alex"))
        } yield out)
          .onDataSource
          .runSyncUnsafe()

      people mustEqual peopleEntries.filter(p => p.name == "Alex")
    }
  }

  "implicitDS on underlying context" - {
    import io.getquill.context.qzio.ImplicitSyntax._

    "should work with additional dependency" in {
      // This is how you import the decoders of `underlying` context without importing things that will conflict
      // i.e. the quote and run methods
      case class Service(ds: DataSource) {
        implicit val dsi = Implicit(ds)
        val people =
          (for {
            n <- ZIO.service[String]
            out <- testContext.run(query[Person].filter(p => p.name == lift(n)))
          } yield out)
            .implicitSomeDS
            .provide(ZLayer.succeed("Alex"))
            .runSyncUnsafe()
      }

      (for {
        ds <- ZIO.service[DataSource]
        svc <- ZIO.attempt(Service(ds))
      } yield (svc.people)).runSyncUnsafe() mustEqual peopleEntries.filter(p => p.name == "Alex")
    }
    "should work" in {
      // This is how you import the decoders of `underlying` context without importing things that will conflict
      // i.e. the quote and run methods
      case class Service(ds: DataSource) {
        implicit val dsi = Implicit(ds)
        val people =
          (for {
            out <- testContext.run(query[Person].filter(p => p.name == "Alex"))
          } yield out)
            .implicitDS
            .runSyncUnsafe()
      }

      (for {
        ds <- ZIO.service[DataSource]
        svc <- ZIO.attempt(Service(ds))
      } yield (svc.people)).runSyncUnsafe() mustEqual peopleEntries.filter(p => p.name == "Alex")
    }
  }
}