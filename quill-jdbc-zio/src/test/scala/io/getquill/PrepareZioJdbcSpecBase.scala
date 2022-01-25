package io.getquill

import io.getquill.context.ZioJdbc._
import io.getquill.context.jdbc.ResultSetExtractor
import io.getquill.context.sql.ProductSpec
import org.scalactic.Equality
import zio.{ Has, Task, ZIO }

import java.sql.{ Connection, PreparedStatement, ResultSet }

trait PrepareZioJdbcSpecBase extends ProductSpec with ZioSpec {

  implicit val productEq = new Equality[Product] {
    override def areEqual(a: Product, b: Any): Boolean = b match {
      case Product(_, desc, sku) => desc == a.description && sku == a.sku
      case _                     => false
    }
  }

  def productExtractor: (ResultSet, Connection) => Product

  def withOrderedIds(products: List[Product]) =
    products.zipWithIndex.map { case (product, id) => product.copy(id = id.toLong + 1) }

  def singleInsert(prep: QCIO[PreparedStatement]) = {
    prep.flatMap(stmt =>
      Task(stmt).bracketAuto { stmt => Task(stmt.execute()) }).onDataSource.provide(Has(pool)).runSyncUnsafe()
  }

  def batchInsert(prep: QCIO[List[PreparedStatement]]) = {
    prep.flatMap(stmts =>
      ZIO.collectAll(
        stmts.map(stmt =>
          Task(stmt).bracketAuto { stmt => Task(stmt.execute()) })
      )).onDataSource.provide(Has(pool)).runSyncUnsafe()
  }

  def extractResults[T](prepareStatement: QCIO[PreparedStatement])(extractor: (ResultSet, Connection) => T) = {
    (for {
      conn <- ZIO.service[Connection]
      result <- prepareStatement.provide(Has(conn)).bracketAuto { stmt =>
        Task(stmt.executeQuery()).bracketAuto { rs =>
          Task(ResultSetExtractor(rs, stmt.getConnection, extractor))
        }
      }
    } yield result).onDataSource.provide(Has(pool)).runSyncUnsafe()
  }

  def extractProducts(prep: QCIO[PreparedStatement]) =
    extractResults(prep)(productExtractor)
}
