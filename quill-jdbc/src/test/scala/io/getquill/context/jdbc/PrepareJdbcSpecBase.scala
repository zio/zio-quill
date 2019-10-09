package io.getquill.context.jdbc
import java.sql.{ Connection, PreparedStatement, ResultSet }

import io.getquill.context.sql.ProductSpec
import io.getquill.util.Using.Manager
import org.scalactic.Equality
import scala.util.{ Success, Failure }

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

  def singleInsert(conn: => Connection)(prep: Connection => PreparedStatement) = {
    val flag = Manager { use =>
      val c = use(conn)
      val s = use(prep(c))
      s.execute()
    }
    flag match {
      case Success(value) => value
      case Failure(e)     => throw e
    }
  }

  def batchInsert(conn: => Connection)(prep: Connection => List[PreparedStatement]) = {
    val r = Manager { use =>
      val c = use(conn)
      val st = prep(c)
      appendExecuteSequence(st)
    }
    r.flatten match {
      case Success(value) => value
      case Failure(e)     => throw e
    }
  }

  def extractResults[T](conn: => Connection)(prep: Connection => PreparedStatement)(extractor: ResultSet => T) = {
    val r = Manager { use =>
      val c = use(conn)
      val st = use(prep(c))
      val rs = st.executeQuery()
      ResultSetExtractor(rs, extractor)
    }
    r match {
      case Success(v) => v
      case Failure(e) => throw e
    }
  }

  def extractProducts(conn: => Connection)(prep: Connection => PreparedStatement): List[Product] =
    extractResults(conn)(prep)(productExtractor)

  def appendExecuteSequence(actions: => List[PreparedStatement]) = {
    Manager { use =>
      actions.map { stmt =>
        val s = use(stmt)
        s.execute()
      }
    }
  }
}
