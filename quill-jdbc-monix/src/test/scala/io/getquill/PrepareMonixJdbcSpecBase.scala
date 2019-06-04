package io.getquill

import java.sql.{ Connection, PreparedStatement, ResultSet }

import io.getquill.context.jdbc.ResultSetExtractor
import io.getquill.context.sql.ProductSpec
import monix.eval.Task
import org.scalactic.Equality

trait PrepareMonixJdbcSpecBase extends ProductSpec {

  implicit val productEq = new Equality[Product] {
    override def areEqual(a: Product, b: Any): Boolean = b match {
      case Product(_, desc, sku) => desc == a.description && sku == a.sku
      case _                     => false
    }
  }

  def productExtractor: ResultSet => Product

  def withOrderedIds(products: List[Product]) =
    products.zipWithIndex.map { case (product, id) => product.copy(id = id.toLong + 1) }

  def singleInsert(conn: => Connection)(prep: Connection => Task[PreparedStatement]) = {
    Task(conn).bracket { conn =>
      prep(conn).bracket { stmt =>
        Task(stmt.execute())
      }(stmt => Task(stmt.close()))
    }(conn => Task(conn.close()))
  }

  def batchInsert(conn: => Connection)(prep: Connection => Task[List[PreparedStatement]]) = {
    Task(conn).bracket { conn =>
      prep(conn).flatMap(stmts =>
        Task.sequence(
          stmts.map(stmt =>
            Task(stmt).bracket { stmt =>
              Task(stmt.execute())
            }(stmt => Task(stmt.close())))
        ))
    }(conn => Task(conn.close()))
  }

  def extractResults[T](conn: => Connection)(prep: Connection => Task[PreparedStatement])(extractor: ResultSet => T) = {
    Task(conn).bracket { conn =>
      prep(conn).bracket { stmt =>
        Task(stmt.executeQuery()).bracket { rs =>
          Task(ResultSetExtractor(rs, extractor))
        }(rs => Task(rs.close()))
      }(stmt => Task(stmt.close()))
    }(conn => Task(conn.close()))
  }

  def extractProducts(conn: => Connection)(prep: Connection => Task[PreparedStatement]) =
    extractResults(conn)(prep)(productExtractor)
}
