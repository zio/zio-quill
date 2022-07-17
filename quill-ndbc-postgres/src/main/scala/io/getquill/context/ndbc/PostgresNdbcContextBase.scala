package io.getquill.context.ndbc

import java.time.ZoneOffset

import io.getquill.context.sql.encoding.ArrayEncoding
import io.getquill.{ NamingStrategy, PostgresDialect }
import io.trane.ndbc.{ PostgresPreparedStatement, PostgresRow }

trait PostgresNdbcContextBase[+N <: NamingStrategy] extends NdbcContextBase[PostgresDialect, N, PostgresPreparedStatement, PostgresRow]
  with ArrayEncoding
  with PostgresEncoders
  with PostgresDecoders {

  override type NullChecker = PostgresNdbcNullChecker
  class PostgresNdbcNullChecker extends BaseNullChecker {
    override def apply(index: Index, row: ResultRow): Boolean =
      row.column(index).isNull
  }
  implicit val nullChecker: NullChecker = new PostgresNdbcNullChecker()

  override val idiom = PostgresDialect

  override protected def createPreparedStatement(sql: String) = PostgresPreparedStatement.create(sql)

  override protected val zoneOffset: ZoneOffset = ZoneOffset.UTC
}
