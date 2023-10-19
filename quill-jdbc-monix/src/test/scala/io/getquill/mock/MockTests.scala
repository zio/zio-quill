package io.getquill.mock

import io.getquill.base.Spec
import java.io.Closeable
import java.sql._

import io.getquill.context.monix.MonixJdbcContext.EffectWrapper
import javax.sql.DataSource
import io.getquill.{Literal, PostgresMonixJdbcContext}
import monix.eval.Task
import monix.execution.Scheduler
import org.mockito.scalatest.AsyncMockitoSugar
import org.scalatest.matchers.should.Matchers._

import scala.reflect.ClassTag
import scala.util.Try

class MockTests extends Spec with AsyncMockitoSugar {
  import scala.reflect.runtime.{universe => ru}
  implicit val scheduler = Scheduler.io()

  object MockResultSet {
    def apply[T: ClassTag: ru.TypeTag](data: Seq[T]) = {
      val rs       = mock[ResultSet]
      var rowIndex = -1

      def introspection                = new Introspection(data(rowIndex))
      def getIndex(i: Int): Any        = introspection.getIndex(i)
      def getColumn(name: String): Any = introspection.getField(name)

      when(rs.next()) thenAnswer {
        rowIndex += 1
        rowIndex < data.length
      }

      when(rs.getString(any[Int])) thenAnswer ((i: Int) => {
        getIndex(i).asInstanceOf[String]
      })

      when(rs.getInt(any[Int])) thenAnswer ((i: Int) => { getIndex(i).asInstanceOf[Int] })

      rs
    }

  }

  case class Person(name: String, age: Int)

  trait MyDataSource extends DataSource with Closeable

  val msg = "Database blew up for some reason"

  "stream is correctly closed after usage" in {
    val people = List(Person("Joe", 11), Person("Jack", 22))

    val ds   = mock[MyDataSource]
    val conn = mock[Connection]
    val stmt = mock[PreparedStatement]
    val rs   = MockResultSet(people)

    when(ds.getConnection) thenReturn conn
    when(conn.prepareStatement(any[String], any[Int], any[Int])) thenReturn stmt
    when(stmt.executeQuery()) thenReturn rs
    when(conn.getAutoCommit) thenReturn true

    val ctx = new PostgresMonixJdbcContext(Literal, ds, EffectWrapper.using(scheduler))
    import ctx._

    val results =
      stream(query[Person])
        .foldLeftL(Seq[Person]()) { case (l, p) => p +: l }
        .map(_.reverse)
        .runSyncUnsafe()

    results must equal(people)

    // In test suite verifications come after
    val order = inOrder(Seq[AnyRef](conn, rs): _*)
    // opening autocommit bracket
    order.verify(conn).getAutoCommit
    order.verify(conn).setAutoCommit(false)
    // resultset close bracket
    order.verify(rs).close()
    // closing autocommit bracket
    order.verify(conn).setAutoCommit(true)
    // connection close bracket
    order.verify(conn).close()
    order.verifyNoMoreInteractions()
  }

  "stream is correctly closed when ending conn.setAutoCommit returns error but is caught" in {
    val people = List(Person("Joe", 11), Person("Jack", 22))

    val ds   = mock[MyDataSource]
    val conn = mock[Connection]
    val stmt = mock[PreparedStatement]

    when(ds.getConnection) thenReturn conn
    when(conn.getAutoCommit) thenThrow (new SQLException(msg))

    val ctx = new PostgresMonixJdbcContext(Literal, ds, EffectWrapper.using(scheduler))
    import ctx._

    val results =
      stream(query[Person])
        .foldLeftL(Seq[Person]()) { case (l, p) => p +: l }
        .map(_.reverse)
        .materialize
        .runSyncUnsafe()

    results should matchPattern {
      case scala.util.Failure(e) if (e.getMessage == msg) =>
    }

    // In test suite verifications come after
    val order = inOrder(conn)
    order.verify(conn).getAutoCommit
    order.verify(conn).close()
    order.verifyNoMoreInteractions()
  }

  "stream is correctly closed after usage and conn.setAutoCommit afterward fails" in {
    val people = List(Person("Joe", 11), Person("Jack", 22))

    val ds   = mock[MyDataSource]
    val conn = mock[Connection]
    val stmt = mock[PreparedStatement]
    val rs   = MockResultSet(people)

    when(ds.getConnection) thenReturn conn
    when(conn.prepareStatement(any[String], any[Int], any[Int])) thenReturn stmt
    when(stmt.executeQuery()) thenReturn rs
    when(conn.getAutoCommit) thenReturn true
    when(conn.setAutoCommit(any[Boolean])) thenAnswer ((f: Boolean) => ()) andThenThrow (new SQLException(msg))

    val ctx = new PostgresMonixJdbcContext(Literal, ds, EffectWrapper.using(scheduler))
    import ctx._

    // In this case, instead of catching the error inside the observable, let it propagate to the top
    // and make sure that the connection is closed anyhow
    val results =
      Try {
        stream(query[Person])
          .foldLeftL(Seq[Person]()) { case (l, p) => p +: l }
          .map(_.reverse)
          .runSyncUnsafe()
      }

    results should matchPattern {
      case scala.util.Failure(e) if (e.getMessage == msg) =>
    }

    // In test suite verifications come after
    val order = inOrder(Seq[AnyRef](conn, rs): _*)
    // opening autocommit bracket
    order.verify(conn).getAutoCommit
    order.verify(conn).setAutoCommit(false)
    // resultset close bracket
    order.verify(rs).close()
    // closing autocommit bracket
    order.verify(conn).setAutoCommit(true)
    // connection close bracket
    order.verify(conn).close()
    order.verifyNoMoreInteractions()
  }

  "stream is correctly closed after usage and conn.setAutoCommit after results fails - with custom wrapClose" in {
    val people = List(Person("Joe", 11), Person("Jack", 22))

    val ds   = mock[MyDataSource]
    val conn = mock[Connection]
    val stmt = mock[PreparedStatement]
    val rs   = MockResultSet(people)

    when(ds.getConnection) thenReturn conn
    when(conn.prepareStatement(any[String], any[Int], any[Int])) thenReturn stmt
    when(stmt.executeQuery()) thenReturn rs
    when(conn.getAutoCommit) thenReturn true
    when(conn.setAutoCommit(any[Boolean])) thenAnswer ((f: Boolean) => ()) andThenThrow (new SQLException(msg))

    val runner = new EffectWrapper {
      override def schedule[T](t: Task[T]): Task[T] = t.executeOn(scheduler, true)
      override def boundary[T](t: Task[T]): Task[T] = t.executeOn(scheduler, true)
      override def wrapClose(t: => Unit): Task[Unit] =
        Task(t).onErrorHandle[Unit](e => ())
    }

    val ctx = new PostgresMonixJdbcContext(Literal, ds, runner)
    import ctx._

    val results =
      stream(query[Person])
        .foldLeftL(Seq[Person]()) { case (l, p) => p +: l }
        .map(_.reverse)
        .runSyncUnsafe()

    results must equal(people)

    // In test suite verifications come after
    val order = inOrder(Seq[AnyRef](conn, rs): _*)
    // opening autocommit bracket
    order.verify(conn).getAutoCommit
    order.verify(conn).setAutoCommit(false)
    // resultset close bracket
    order.verify(rs).close()
    // closing autocommit bracket
    order.verify(conn).setAutoCommit(true)
    // connection close bracket
    order.verify(conn).close()
    order.verifyNoMoreInteractions()
  }
}
