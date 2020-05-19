package io.getquill.mock

import io.getquill.ZioTestUtil._

import java.io.Closeable
import java.sql._
import javax.sql.DataSource
import io.getquill.{ Literal, PostgresZioJdbcContext }
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must.Matchers._
import io.getquill.context.ZioJdbc._
import org.scalatest.freespec.AnyFreeSpec

import scala.reflect.ClassTag

class ZioMockSpec extends AnyFreeSpec with MockitoSugar { //with AsyncMockitoSugar
  import scala.reflect.runtime.{ universe => ru }

  object MockResultSet {
    def apply[T: ClassTag: ru.TypeTag](data: Seq[T]) = {
      val rs = mock[ResultSet]
      var rowIndex = -1

      def introspection = new Introspection(data(rowIndex))
      def getIndex(i: Int): Any = introspection.getIndex(i)
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

    val ds = mock[MyDataSource]
    val conn = mock[Connection]
    val stmt = mock[PreparedStatement]
    val rs = MockResultSet(people)

    when(ds.getConnection) thenReturn conn
    when(conn.prepareStatement(any[String], any[Int], any[Int])) thenReturn stmt
    when(stmt.executeQuery()) thenReturn rs
    when(conn.getAutoCommit) thenReturn true

    val ctx = new PostgresZioJdbcContext(Literal)
    import ctx._

    val results =
      stream(query[Person])
        .fold(Seq[Person]())({ case (l, p) => p +: l })
        .map(_.reverse)
        .provideConnectionFrom(ds).defaultRun

    results must equal(people)

    // In test suite verifications come after
    val order = inOrder(Seq[AnyRef](conn, stmt, rs): _*)
    // opening autocommit bracket
    order.verify(conn).getAutoCommit
    order.verify(conn).setAutoCommit(false)
    // resultset close bracket
    order.verify(rs).close()
    // close prepared statement
    order.verify(stmt).close()
    // closing autocommit bracket
    order.verify(conn).setAutoCommit(true)
    // connection close bracket
    order.verify(conn).close()
    order.verifyNoMoreInteractions()
  }

  "stream is correctly closed when ending conn.setAutoCommit returns error but is caught" in {
    val people = List(Person("Joe", 11), Person("Jack", 22))

    val ds = mock[MyDataSource]
    val conn = mock[Connection]
    val stmt = mock[PreparedStatement]

    when(ds.getConnection) thenReturn conn
    when(conn.getAutoCommit) thenThrow (new SQLException(msg))

    val ctx = new PostgresZioJdbcContext(Literal)
    import ctx._

    val resultMsg =
      stream(query[Person])
        .fold(Seq[Person]())({ case (l, p) => p +: l })
        .map(_.reverse)
        .provideConnectionFrom(ds).foldCause(cause => cause.prettyPrint, _ => "").defaultRun

    resultMsg.contains("Fiber failed.") mustBe true
    resultMsg.contains(msg) mustBe true

    // In test suite verifications come after
    val order = inOrder(conn)
    order.verify(conn).getAutoCommit
    order.verify(conn).close()
    order.verifyNoMoreInteractions()
  }

  "stream is correctly closed after usage and conn.setAutoCommit afterward fails" in {
    val people = List(Person("Joe", 11), Person("Jack", 22))

    val ds = mock[MyDataSource]
    val conn = mock[Connection]
    val stmt = mock[PreparedStatement]
    val rs = MockResultSet(people)

    when(ds.getConnection) thenReturn conn
    when(conn.prepareStatement(any[String], any[Int], any[Int])) thenReturn stmt
    when(stmt.executeQuery()) thenReturn rs
    when(conn.getAutoCommit) thenReturn true
    when(conn.setAutoCommit(any[Boolean])) thenAnswer ((f: Boolean) => ()) andThenThrow (new SQLException(msg))

    val ctx = new PostgresZioJdbcContext(Literal)
    import ctx._

    // In this case, instead of catching the error inside the observable, let it propogate to the top
    // and make sure that the connection is closed anyhow
    val resultMsg =
      stream(query[Person])
        .fold(Seq[Person]())({ case (l, p) => p +: l })
        .map(_.reverse)
        .provideConnectionFrom(ds).foldCause(cause => cause.prettyPrint, _ => "").defaultRun

    resultMsg.contains("Fiber failed.") mustBe true
    resultMsg.contains(msg) mustBe true

    // In test suite verifications come after
    val order = inOrder(Seq[AnyRef](conn, stmt, rs): _*)
    // opening autocommit bracket
    order.verify(conn).getAutoCommit
    order.verify(conn).setAutoCommit(false)
    // resultset close bracket
    order.verify(rs).close()
    // close prepared statement
    order.verify(stmt).close()
    // closing autocommit bracket
    order.verify(conn).setAutoCommit(true)
    // connection close bracket
    order.verify(conn).close()
    order.verifyNoMoreInteractions()
  }
}
