package io.getquill.integration

import java.sql.{ Connection, ResultSet }

import io.getquill._
import io.getquill.context.monix.Runner
import monix.execution.Scheduler
import monix.execution.schedulers.CanBlock
import org.scalatest.matchers.should.Matchers._

import scala.concurrent.duration.Duration

/**
 * This is a long-running test that will cause a OutOfMemory exception if
 * a ResultSet is not streamed correctly (e.g. if the ResultSet.TYPE_SCROLL_SENSITIVE option
 * is used which will force most databases to put the entire ResultSet into memory).
 * Run with -Xmx200m and doBlowUp=true to correctly reproduce the error.
 * You can also use -Xmx100m but then it will blow up due to a GC Limit OutOfMemory as opposed
 * to a heap space OutOfMemory.
 *
 * As a default, this test will run as part of the suite without blowing up.
 */
class StreamResultsOrBlowUpSpec extends Spec {

  case class Person(name: String, age: Int)

  private implicit val scheduler = Scheduler.io()

  // set to true in order to create a ResultSet type (i.e. a rewindable one)
  // that will force jdbc to load the entire ResultSet into memory and crash this test.
  val doBlowUp = false

  val ctx = new PostgresMonixJdbcContext(Literal, "testPostgresDB", Runner.default) {
    override protected def prepareStatementForStreaming(sql: String, conn: Connection, fetchSize: Option[Int]) = {
      val stmt =
        conn.prepareStatement(
          sql,
          if (doBlowUp) ResultSet.TYPE_SCROLL_SENSITIVE
          else ResultSet.TYPE_FORWARD_ONLY,
          ResultSet.CONCUR_READ_ONLY
        )
      fetchSize.foreach(stmt.setFetchSize(_))
      stmt
    }
  }
  import ctx.{ run => runQuill, _ }

  val numRows = 1000000L

  "stream a large result set without blowing up" in {
    val deletes = runQuill { query[Person].delete }
    deletes.runSyncUnsafe(Duration.Inf)(scheduler, CanBlock.permit)

    val inserts = quote {
      (numRows: Long) =>
        infix"""insert into person (name, age) select md5(random()::text), random()*10+1 from generate_series(1, ${numRows}) s(i)""".as[Insert[Int]]
    }

    runQuill(inserts(lift(numRows))).runSyncUnsafe(Duration.Inf)(scheduler, CanBlock.permit)

    // not sure why but foreachL causes a OutOfMemory exception anyhow, and firstL causes a ResultSet Closed exception
    val result = stream(query[Person], 100)
      .zipWithIndex
      .foldLeftL(0L)({
        case (totalYears, (person, index)) => {
          // Need to print something out as we stream or travis will thing the build is stalled and kill it with the following message:
          // "No output has been received in the last 10m0s..."
          if (index % 10000 == 0) println(s"Streaming Test Row: ${index}")
          totalYears + person.age
        }
      })
      .runSyncUnsafe(Duration.Inf)(scheduler, CanBlock.permit)
    result should be > numRows
  }
}
