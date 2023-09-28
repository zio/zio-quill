package io.getquill

import io.getquill.context.monix.MonixJdbcContext.EffectWrapper
import io.getquill.util.LoadConfig
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers._

import scala.collection.mutable.ArrayBuffer
import com.zaxxer.hikari.HikariDataSource

class ResultSetIteratorSpec extends AnyFreeSpec with Matchers with BeforeAndAfterAll {

  val ds: HikariDataSource = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource
  implicit val scheduler   = Scheduler.global

  val ctx = new PostgresMonixJdbcContext(Literal, ds, EffectWrapper.default)
  import ctx._

  case class Person(name: String, age: Int)

  val peopleInsert =
    quote((p: Person) => query[Person].insertValue(p))

  val peopleEntries: List[Person] = List(
    Person("Alex", 60),
    Person("Bert", 55),
    Person("Cora", 33)
  )

  override def beforeAll: Unit =
    ctx.transaction {
      for {
        _ <- ctx.run(query[Person].delete)
        _ <- ctx.run(liftQuery(peopleEntries).foreach(p => peopleInsert(p)))
      } yield ()
    }.runSyncUnsafe()

  "traverses correctly" in {
    val results =
      Task(ds.getConnection).bracket { conn =>
        Task {
          val stmt = conn.prepareStatement("select * from person")
          val rs =
            new ResultSetIterator[String](stmt.executeQuery(), conn, extractor = (rs, _) => { rs.getString(1) })
          val accum = ArrayBuffer[String]()
          while (rs.hasNext) accum += rs.next()
          accum
        }
      }(conn => Task(conn.close())).runSyncUnsafe()

    results should contain theSameElementsAs (peopleEntries.map(_.name))
  }

  "can take head element" in {
    val result =
      Task(ds.getConnection).bracket { conn =>
        Task {
          val stmt = conn.prepareStatement("select * from person where name = 'Alex'")
          val rs   = new ResultSetIterator(stmt.executeQuery(), conn, extractor = (rs, _) => { rs.getString(1) })
          rs.head
        }
      }(conn => Task(conn.close())).runSyncUnsafe()

    result must equal("Alex")
  }
}
