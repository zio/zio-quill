package io.getquill
import java.io.Closeable
import java.sql.Connection
import scala.util.Try
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.streaming.StreamingContext
import javax.sql.DataSource
import monix.eval.Task
import monix.execution.misc.Local
import monix.execution.Scheduler

abstract class TaskStreamingContext[Dialect <: SqlIdiom, Naming <: NamingStrategy](
  dataSource: DataSource with Closeable,
  val idiom:  Dialect,
  val naming: Naming
)(implicit transactionScheduler: Scheduler)
  extends StreamingContext[Task, Dialect, Naming] {

  private val currentConnection: Local[Option[Connection]] = Local(None)

  override def close(): Unit = dataSource.close()

  override protected def withConnection[T](f: Connection => Task[T]): Task[T] =
    currentConnection.get
      .map(connection => f(connection))
      .getOrElse {
        Task(dataSource.getConnection).bracket(f)(conn => Task(conn.close())).executeOn(transactionScheduler)
      }

  override def transaction[A](f: Task[A]): Task[A] = {
    val dbEffects = for {
      result <- currentConnection.get match {
        case Some(_) => f // Already in a transaction
        case None =>
          Task {
            val connection = dataSource.getConnection
            val wasAutoCommit = connection.getAutoCommit
            connection.setAutoCommit(false)
            (connection, wasAutoCommit)
          }.bracket {
            case (connection, _) =>
              Task(connection).flatMap { connection =>
                currentConnection.update(Some(connection))
                f.onCancelRaiseError(new IllegalStateException()) // TODO Change this to another error
                  .doOnFinish {
                    case Some(error) =>
                      connection.rollback()
                      Task.raiseError(error)
                    case None =>
                      Task(connection.commit())
                  }
              }
          } {
            case (connection, wasAutoCommit) =>
              Task {
                currentConnection.update(None)
                connection.setAutoCommit(wasAutoCommit)
                connection.close()
              }
          }
      }
    } yield result

    dbEffects
      .executeOn(transactionScheduler)
      .executeWithOptions(_.enableLocalContextPropagation)
      .asyncBoundary
  }

  override def probe(statement: String): Try[_] = Try {
    withConnection(conn => Task(conn.createStatement.execute(statement))).runSyncUnsafe()
  }
}
