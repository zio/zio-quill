package io.getquill

import io.getquill.context.ZioJdbc._
import io.getquill.context.jdbc.ResultSetExtractor
import io.getquill.context.qzio.ImplicitSyntax.Implicit
import io.getquill.context.sql.ProductSpec
import org.scalactic.Equality
import zio.{ Runtime, Task, ZEnvironment, ZIO }

import java.sql.{ Connection, PreparedStatement, ResultSet }
import javax.sql.DataSource

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

  def singleInsert(prep: QCIO[PreparedStatement])(implicit runtime: Implicit[Runtime.Managed[DataSource]]) = {
    prep.flatMap(stmt =>
      Task(stmt).acquireReleaseWithAuto { stmt => Task(stmt.execute()) }).onDataSource.runSyncUnsafe()
  }

  def batchInsert(prep: QCIO[List[PreparedStatement]])(implicit runtime: Implicit[Runtime.Managed[DataSource]]) =
    prep.flatMap(stmts =>
      ZIO.collectAll(
        stmts.map(stmt =>
          Task(stmt).acquireReleaseWithAuto { stmt => Task(stmt.execute()) })
      )).onDataSource.runSyncUnsafe()

  def extractResults[T](prepareStatement: QCIO[PreparedStatement])(extractor: (ResultSet, Connection) => T)(implicit runtime: Implicit[Runtime.Managed[DataSource]]) =
    (for {
      conn <- ZIO.service[Connection]
      result <- prepareStatement.provideEnvironment(ZEnvironment(conn)).acquireReleaseWithAuto { stmt =>
        Task(stmt.executeQuery()).acquireReleaseWithAuto { rs =>
          Task(ResultSetExtractor(rs, stmt.getConnection, extractor))
        }
      }
    } yield result).onDataSource.runSyncUnsafe()

  def extractProducts(prep: QCIO[PreparedStatement])(implicit runtime: Implicit[Runtime.Managed[DataSource]]) =
    extractResults(prep)(productExtractor)
}
