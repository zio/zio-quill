package io.getquill

import io.getquill.context.qzio.ResultSetIterator
import zio.ZIO
import io.getquill.postgres._

import javax.sql.DataSource
import scala.collection.mutable.ArrayBuffer

class ResultSetIteratorSpec extends ZioSpec {

  val ctx = new PostgresZioJdbcContext(Literal)
  import ctx._

  case class Person(name: String, age: Int)

  val peopleInsert =
    quote((p: Person) => query[Person].insertValue(p))

  val peopleEntries = List(
    Person("Alex", 60),
    Person("Bert", 55),
    Person("Cora", 33)
  )

  override def beforeAll = {
    super.beforeAll()
    ctx.transaction {
      for {
        _ <- ctx.run(query[Person].delete)
        _ <- ctx.run(liftQuery(peopleEntries).foreach(p => peopleInsert(p)))
      } yield ()
    }.runSyncUnsafe()
  }

  "traverses correctly" in {
    val results =
      ZIO.service[DataSource].mapAttempt(ds => ds.getConnection).acquireReleaseWithAuto { conn =>
        ZIO.attempt {
          val stmt = conn.prepareStatement("select * from person")
          val rs = new ResultSetIterator[String](stmt.executeQuery(), conn, extractor = (rs, conn) => { rs.getString(1) })
          val accum = ArrayBuffer[String]()
          while (rs.hasNext) accum += rs.next()
          accum
        }
      }.runSyncUnsafe()

    results must contain theSameElementsAs (peopleEntries.map(_.name))
  }

  "can take head element" in {
    val result =
      ZIO.service[DataSource].mapAttempt(ds => ds.getConnection).acquireReleaseWithAuto { conn =>
        ZIO.attempt {
          val stmt = conn.prepareStatement("select * from person where name = 'Alex'")
          val rs = new ResultSetIterator(stmt.executeQuery(), conn, extractor = (rs, conn) => { rs.getString(1) })
          rs.head
        }
      }.runSyncUnsafe()

    result must equal("Alex")
  }
}
