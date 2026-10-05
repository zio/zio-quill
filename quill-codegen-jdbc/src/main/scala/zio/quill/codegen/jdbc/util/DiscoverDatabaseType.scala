package zio.quill.codegen.jdbc.util

import zio.quill.codegen.jdbc.DatabaseTypes.DatabaseType
import zio.quill.codegen.jdbc.model.JdbcTypes.JdbcConnectionMaker
import zio.quill.util.Using.Manager

import scala.util.{Failure, Success}

object DiscoverDatabaseType {
  def apply(
    connectionMaker: JdbcConnectionMaker
  ): DatabaseType = {
    val tryProductName = Manager { use =>
      val conn = use(connectionMaker())
      val meta = conn.getMetaData
      meta.getDatabaseProductName
    }

    tryProductName.flatMap { productName =>
      DatabaseType.fromProductName(productName)
    } match {
      case Success(value) => value
      case Failure(e)     => throw new IllegalArgumentException("Could not parse database product name.", e)
    }
  }

}
