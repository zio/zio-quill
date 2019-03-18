package io.getquill.codegen.jdbc.util

import com.github.choppythelumberjack.tryclose.JavaImplicits._
import com.github.choppythelumberjack.tryclose.TryClose
import io.getquill.codegen.jdbc.DatabaseTypes.DatabaseType
import io.getquill.codegen.jdbc.model.JdbcTypes.JdbcConnectionMaker

import scala.util.{ Failure, Success }

object DiscoverDatabaseType {
  def apply(
    connectionMaker: JdbcConnectionMaker
  ): DatabaseType =
    {
      val tryProductName =
        for {
          conn <- TryClose(connectionMaker())
          meta <- TryClose.wrap(conn.getMetaData)
          productName <- TryClose.wrap(meta.get.getDatabaseProductName)
        } yield productName

      tryProductName.unwrap.asTry.flatMap { productName =>
        DatabaseType.fromProductName(productName)
      } match {
        case Success(value) => value
        case Failure(e)     => throw new IllegalArgumentException("Could not parse database product name.", e)
      }
    }
}
