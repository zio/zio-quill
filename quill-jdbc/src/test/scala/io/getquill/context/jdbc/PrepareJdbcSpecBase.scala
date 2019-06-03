package io.getquill.context.jdbc
import java.sql.{ Connection, PreparedStatement, ResultSet }

import com.github.choppythelumberjack.tryclose._
import com.github.choppythelumberjack.tryclose.JavaImplicits._
import io.getquill.context.sql.ProductSpec
import org.scalactic.Equality

trait PrepareJdbcSpecBase extends ProductSpec {

  implicit val productEq = new Equality[Product] {
    override def areEqual(a: Product, b: Any): Boolean = b match {
      case Product(_, desc, sku) => desc == a.description && sku == a.sku
      case _                     => false
    }
  }

  def productExtractor: ResultSet => Product

  def withOrderedIds(products: List[Product]) =
    products.zipWithIndex.map { case (product, id) => product.copy(id = id.toLong + 1) }

  def singleInsert(conn: => Connection)(prep: Connection => PreparedStatement) =
    (for {
      conn <- TryClose(conn)
      stmt <- TryClose(prep(conn))
      flag <- TryClose.wrap(stmt.execute())
    } yield flag).unwrap match {
      case Success(value) => value
      case Failure(e)     => throw e
    }

  def batchInsert(conn: => Connection)(prep: Connection => List[PreparedStatement]) =
    (for {
      conn <- TryClose(conn)
      list <- appendExecuteSequence(prep(conn))
    } yield list).unwrap match {
      case Success(value) => value
      case Failure(e)     => throw e
    }

  def extractResults[T](conn: => Connection)(prep: Connection => PreparedStatement)(extractor: ResultSet => T) =
    (for {
      conn <- TryClose(conn)
      stmt <- TryClose(prep(conn))
      rs <- TryClose(stmt.executeQuery())
    } yield {
      wrap(ResultSetExtractor(rs, extractor))
    }).unwrap match {
      case Success(value) =>
        value
      case Failure(e) => throw e
    }

  def extractProducts(conn: => Connection)(prep: Connection => PreparedStatement): List[Product] =
    extractResults(conn)(prep)(productExtractor)

  def appendExecuteSequence(actions: => List[PreparedStatement]) = {
    actions.foldLeft(TryClose.wrap(List[Boolean]())) {
      case (currTry, stmt) => currTry.flatMap { wrap =>
        val list = wrap.get
        TryClose(stmt).flatMap { stmtInner =>
          TryClose.wrap(stmtInner.execute() +: list)
        }
      }
    }
  }
}
